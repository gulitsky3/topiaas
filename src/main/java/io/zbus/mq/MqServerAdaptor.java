package io.zbus.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.auth.AuthResult;
import io.zbus.auth.RequestAuth;
import io.zbus.kit.FileKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.commands.CommandHandler;
import io.zbus.mq.commands.CreateHandler;
import io.zbus.mq.commands.MsgKit;
import io.zbus.mq.commands.PubHandler;
import io.zbus.mq.commands.QueryHandler;
import io.zbus.mq.commands.RemoveHandler;
import io.zbus.mq.commands.RouteHandler;
import io.zbus.mq.commands.SubHandler;
import io.zbus.mq.commands.TakeHandler;
import io.zbus.mq.plugin.DefaultUrlRouter;
import io.zbus.mq.plugin.IpFilter;
import io.zbus.mq.plugin.UrlRouter;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.Http;

/**
 * 
 * Message control based on HTTP headers extension
 * 
 * @author leiming.hong Jul 9, 2018
 *
 */
public class MqServerAdaptor extends ServerAdaptor implements Cloneable { 
	private static final Logger logger = LoggerFactory.getLogger(MqServerAdaptor.class); 
	private SubscriptionManager subscriptionManager;
	private MessageDispatcher messageDispatcher;
	private MessageQueueManager mqManager; 
	private RequestAuth requestAuth; 
	private Map<String, CommandHandler> commandTable = new HashMap<>(); 
	
	private RpcProcessor rpcProcessor;
	private MqServerConfig config;
	
	private UrlRouter urlRouter;
	private IpFilter sessionFilter;
	
	private FileKit fileKit;
	
	public MqServerAdaptor(MqServerConfig config) { 
		this.config = config;
		mqManager = new MessageQueueManager();
		subscriptionManager = new SubscriptionManager(mqManager);  
		
		messageDispatcher = new MessageDispatcher(subscriptionManager, sessionTable); 
		mqManager.mqDir = config.mqDiskDir;  
		
		fileKit = new FileKit(config.fileCacheEnabled);
		mqManager.loadQueueTable();    
		
		urlRouter = config.getUrlMqRouter();
		
		if(urlRouter == null) {
			urlRouter = new DefaultUrlRouter();
		} 
		
		commandTable.put(Protocol.PUB, new PubHandler(messageDispatcher, mqManager));
		commandTable.put(Protocol.SUB, new SubHandler(messageDispatcher, mqManager, subscriptionManager));
		commandTable.put(Protocol.TAKE, new TakeHandler(messageDispatcher, mqManager));
		commandTable.put(Protocol.ROUTE, new RouteHandler(sessionTable));
		commandTable.put(Protocol.CREATE, new CreateHandler(mqManager)); 
		commandTable.put(Protocol.REMOVE, new RemoveHandler(mqManager)); 
		commandTable.put(Protocol.QUERY, new QueryHandler(mqManager));  
		commandTable.put(Protocol.PING, (req, sess)->{}); 
	} 
	
	@Override
	protected MqServerAdaptor clone() { 
		try {
			MqServerAdaptor clone = (MqServerAdaptor) super.clone();
			clone.requestAuth = null;
			return clone;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}  
	
	private void attachInfo(Message request, Session sess) {
		request.setHeader(Protocol.SOURCE, sess.id());
		request.setHeader(Protocol.REMOTE_ADDR, sess.remoteAddress());
		if(request.getHeader(Protocol.ID) == null) {
			request.setHeader(Protocol.ID, StrKit.uuid());
		}
	}
	
	@Override
	public void sessionCreated(Session sess) throws IOException { 
		if(sessionFilter != null) {
			if(!sessionFilter.doFilter(sess)) {
				sess.close();
				return;
			}
		}
		super.sessionCreated(sess);
	}
	 
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		Message req = (Message)msg;    
		if (req == null) {
			MsgKit.reply(req, 400, "json format required", sess); 
			return;
		}   
		String cmd = req.getHeader(Protocol.CMD); 
		
		if(Protocol.PING.equals(cmd)) {
			return;
		}
		
		if(config.verbose) { 
			logger.info(sess.remoteAddress() + ":" + req); 
		}
		
		if(cmd == null) { //Special case for favicon
			if(req.getBody() == null && "/favicon.ico".equals(req.getUrl())) {
				Message res = FileKit.INSTANCE.loadResource("static/favicon.ico");
				sess.write(res);
				return;
			}
		}
		
		//check integrity 
		if(requestAuth != null) {
			AuthResult authResult = requestAuth.auth(req);
			if(!authResult.success) {
				MsgKit.reply(req, 403, authResult.message, sess); 
				return; 
			}
		}   
		
		if(cmd == null) {
			//Filter on URL of request
			boolean handled = routeUrl(req, sess);
			if(handled) return;
		} 
		
		attachInfo(req, sess);  
		
		cmd = req.removeHeader(Protocol.CMD); 
		if (cmd == null) {
			MsgKit.reply(req, 400, "cmd key required", sess); 
			return;
		} 
		cmd = cmd.toLowerCase();  
		
		CommandHandler handler = commandTable.get(cmd);
		if(handler == null) {
			MsgKit.reply(req, 404, "Command(" + cmd + ") Not Found", sess); 
			return; 
		}
		try {
			handler.handle(req, sess);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			MsgKit.reply(req, 500, e.getMessage(), sess);  
		}
	}    
	
	
	public boolean routeUrl(Message req, Session sess) { 
		String url = req.getUrl();
		if(url == null) return false;   
		
		if(config.urlMatchLocalFirst) {
			if(rpcProcessor != null) {
				if(rpcProcessor.matchUrl(url)) {
					Message res = new Message();
					rpcProcessor.process(req, res);
					sess.write(res); 
					return true;
				} 
			} 
		}
		
		String mq = urlRouter.match(mqManager, url); 
		if(mq != null) {
			req.setHeader(Protocol.MQ, mq);
			//Assumed to be RPC
			if(req.getHeader(Protocol.CMD) == null) { // RPC assumed
				req.setHeader(Protocol.CMD, Protocol.PUB);
				req.setHeader(Protocol.ACK, false); //ACK should be disabled
			}  
			
			//TODO check if consumer exists, reply 502, no service available 
			return false;
		} 
		
		if(!config.urlMatchLocalFirst) {
			if(rpcProcessor != null) {
				if(rpcProcessor.matchUrl(url)) {
					Message res = new Message();
					rpcProcessor.process(req, res);
					sess.write(res); 
					return true;
				} 
			} 
		} 
		
		Message res = null;
		if("/".equals(url)) { 
			res = fileKit.loadResource("static/index.html"); 
			if(res.getStatus() != 200) {
				res = new Message();
				res.setStatus(200);
				res.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
				res.setBody("<h1> Welcome to zbus</h1>"); 
			} 
			 
		} else {
			res = new Message();
			res.setStatus(404);
			res.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
			res.setBody(String.format("URL=%s Not Found", url));
		}
		sess.write(res);
		return true; 
	}
	
	public void setRpcProcessor(RpcProcessor rpcProcessor) {
		this.rpcProcessor = rpcProcessor;
	}  
	
	@Override
	protected void cleanSession(Session sess) throws IOException { 
		String sessId = sess.id();
		super.cleanSession(sess); 
		
		subscriptionManager.removeByClientId(sessId);
	}

	public void setRequestAuth(RequestAuth requestAuth) {
		this.requestAuth = requestAuth;
	}

	public SubscriptionManager getSubscriptionManager() {
		return subscriptionManager;
	}

	public MessageQueueManager getMqManager() {
		return mqManager;
	}  
	
}
