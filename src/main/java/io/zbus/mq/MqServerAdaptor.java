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
import io.zbus.mq.commands.PubHandler;
import io.zbus.mq.commands.QueryHandler;
import io.zbus.mq.commands.RemoveHandler;
import io.zbus.mq.commands.Reply;
import io.zbus.mq.commands.RouteHandler;
import io.zbus.mq.commands.SubHandler;
import io.zbus.mq.commands.TakeHandler;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.Http;

public class MqServerAdaptor extends ServerAdaptor { 
	private static final Logger logger = LoggerFactory.getLogger(MqServerAdaptor.class); 
	private SubscriptionManager subscriptionManager;
	private MessageDispatcher messageDispatcher;
	private MessageQueueManager mqManager; 
	private RequestAuth requestAuth; 
	private Map<String, CommandHandler> commandTable = new HashMap<>();
	private boolean verbose = true;  
	
	private RpcProcessor rpcProcessor;
	
	public MqServerAdaptor(MqServerConfig config) { 
		mqManager = new MessageQueueManager();
		subscriptionManager = new SubscriptionManager(mqManager);  
		
		messageDispatcher = new MessageDispatcher(subscriptionManager, sessionTable); 
		mqManager.mqDir = config.mqDiskDir; 
		verbose = config.verbose;
		
		mqManager.loadQueueTable();    
		
		commandTable.put(Protocol.PUB, new PubHandler(messageDispatcher, mqManager));
		commandTable.put(Protocol.SUB, new SubHandler(messageDispatcher, mqManager, subscriptionManager));
		commandTable.put(Protocol.TAKE, new TakeHandler(messageDispatcher, mqManager));
		commandTable.put(Protocol.ROUTE, new RouteHandler(sessionTable));
		commandTable.put(Protocol.CREATE, new CreateHandler(mqManager)); 
		commandTable.put(Protocol.REMOVE, new RemoveHandler(mqManager)); 
		commandTable.put(Protocol.QUERY, new QueryHandler(mqManager));  
		commandTable.put(Protocol.PING, (req, sess)->{}); 
	} 
	
	public MqServerAdaptor duplicate() {
		MqServerAdaptor copy = new MqServerAdaptor(sessionTable);
		copy.subscriptionManager = subscriptionManager;
		copy.messageDispatcher = messageDispatcher;
		copy.mqManager = mqManager;
		copy.requestAuth = requestAuth;
		copy.commandTable = commandTable;
		copy.verbose = verbose;  
		copy.rpcProcessor = rpcProcessor;
		return copy;
	}
	
	private MqServerAdaptor(Map<String, Session> sessionTable) {
		super(sessionTable);
	}
	
	protected void attachInfo(Message request, Session sess) {
		request.setHeader(Protocol.SOURCE, sess.id());
		if(request.getHeader(Protocol.ID) == null) {
			request.setHeader(Protocol.ID, StrKit.uuid());
		}
	}
	 
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		Message req = (Message)msg;    
		if (req == null) {
			Reply.send(req, 400, "json format required", sess); 
			return;
		}   
		String cmd = req.getHeader(Protocol.CMD); 
		
		if(Protocol.PING.equals(cmd)) {
			return;
		}
		
		if(verbose) { 
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
				Reply.send(req, 403, authResult.message, sess); 
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
			Reply.send(req, 400, "cmd key required", sess); 
			return;
		} 
		cmd = cmd.toLowerCase();  
		
		CommandHandler handler = commandTable.get(cmd);
		if(handler == null) {
			Reply.send(req, 404, "Command(" + cmd + ") Not Found", sess); 
			return; 
		}
		try {
			handler.handle(req, sess);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			Reply.send(req, 500, e.getMessage(), sess);  
		}
	}   
	
	/**
	 * Find longest URL matched
	 * @param url
	 * @return
	 */
	private String matchMqPath(String url) {
		int length = 0; 
		String matched = null;
		for(String mq : mqManager.mqNames()) { 
			if(url.startsWith(mq)) {
				if(mq.length() > length) {
					length = mq.length();
					matched = mq; 
				}
			}
		}  
		return matched;
	}
	
	private boolean routeUrl(Message req, Session sess) { 
		String url = req.getUrl();
		if(url == null) return false;   
		
		String mq = matchMqPath(url); //
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
		
		if(rpcProcessor != null) {
			if(rpcProcessor.matchUrl(url)) {
				Message res = new Message();
				rpcProcessor.process(req, res);
				sess.write(res); 
				return true;
			} 
		} 
		  
		Message res = new Message(); 
		res.setStatus(200);
		res.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
		res.setBody("<h1> Welcome to zbus</h1>");
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
