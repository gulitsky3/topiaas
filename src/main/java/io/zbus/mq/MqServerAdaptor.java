package io.zbus.mq;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.auth.AuthResult;
import io.zbus.auth.RequestAuth;
import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.commands.CommandHandler;
import io.zbus.mq.commands.CreateHandler;
import io.zbus.mq.commands.MsgKit;
import io.zbus.mq.commands.NotifyHandler;
import io.zbus.mq.commands.PubHandler;
import io.zbus.mq.commands.QueryHandler;
import io.zbus.mq.commands.RemoveHandler;
import io.zbus.mq.commands.RouteHandler;
import io.zbus.mq.commands.SubHandler;
import io.zbus.mq.commands.TakeHandler;
import io.zbus.mq.plugin.Filter;
import io.zbus.mq.plugin.IpFilter;
import io.zbus.mq.plugin.UrlRouteFilter;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;

/**
 * 
 * Message control based on HTTP headers extension
 * 
 * @author leiming.hong Jul 9, 2018
 *
 */
public class MqServerAdaptor extends ServerAdaptor implements Cloneable { 
	private static final Logger logger = LoggerFactory.getLogger(MqServerAdaptor.class); 
	protected SubscriptionManager subscriptionManager;
	protected MessageDispatcher messageDispatcher;
	protected MqManager mqManager; 
	protected NotifyManager notifyManager = new NotifyManager();
	protected RequestAuth requestAuth; 
	protected Map<String, CommandHandler> commandTable = new HashMap<>(); 
	
	protected RpcProcessor rpcProcessor; 
	protected MqServerConfig config;
	 
	protected IpFilter sessionFilter; 
	
	protected List<Filter> filterList = new ArrayList<>();
	public MqServerAdaptor(MqServerConfig config, Map<String, Object> registeredMethodTable) { 
		this.config = config;
		mqManager = new MqManager();
		subscriptionManager = new SubscriptionManager(mqManager);  
		
		messageDispatcher = new MessageDispatcher(subscriptionManager, sessionTable); 
		mqManager.mqDir = config.getMqDiskDir();   
		 
		mqManager.loadQueueTable();     
		 
		filterList.add(new UrlRouteFilter());
		
		commandTable.put(Protocol.PUB, new PubHandler(messageDispatcher, mqManager));
		commandTable.put(Protocol.SUB, new SubHandler(messageDispatcher, mqManager, subscriptionManager));
		commandTable.put(Protocol.TAKE, new TakeHandler(messageDispatcher, mqManager));
		commandTable.put(Protocol.ROUTE, new RouteHandler(sessionTable));
		commandTable.put(Protocol.CREATE, new CreateHandler(mqManager, registeredMethodTable)); 
		commandTable.put(Protocol.REMOVE, new RemoveHandler(mqManager)); 
		commandTable.put(Protocol.QUERY, new QueryHandler(mqManager));  
		commandTable.put(Protocol.ON_NOTIFY, new NotifyHandler(notifyManager));  
		commandTable.put(Protocol.BIND, new NotifyHandler(notifyManager)); 
		
		commandTable.put(Protocol.PING, (req, sess)->{});  
	} 
	
	protected MqServerAdaptor(MqServerAdaptor that) {
		this.config = that.config;
		this.mqManager = that.mqManager;
		this.subscriptionManager = that.subscriptionManager;
		this.messageDispatcher = that.messageDispatcher;
		this.commandTable = that.commandTable; 
	}
	
	public void onInit() {
		for(Filter filter : filterList) {
			filter.init(this);
		}
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
		String addr = this.config.getPublicServer().getAddress();
		int port = Integer.parseInt(addr.split(":")[1]);
		request.setServerPort(port);
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
	
	protected void process(Message req, Message res, Session sess) {
		for(Filter filter : this.filterList) {
			boolean next = filter.doFilter(req, res);
			if(!next) return; 
		}
		
	}
	
	protected boolean doAuth(Message req, Session sess) {
		//Nothing should change on message before check integrity!!
		if(requestAuth == null) return true;
		if(requestAuth != null) {
			String url = req.getUrl();
			List<String> excludedList = config.publicServer.authExcludedList;
			boolean excluded = false;
			if(url != null) {
				for(String pattern : excludedList) {   
					excluded = HttpKit.urlMatched(url, pattern);
					if(excluded) break; 
				}
			}
			if(!excluded) {
				AuthResult authResult = requestAuth.auth(req);
				if(!authResult.success) {
					MsgKit.reply(req, 403, authResult.message, sess); 
					return false; 
				}
			}
		}  
		return true;
	}
	
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {

		Message req = (Message)msg;    
		Message res = new Message(); 
		if (req == null) {
			MsgKit.reply(req, 400, "json format required", sess); 
			return;
		}    
		
		String cmd = req.getHeader(Protocol.CMD); 
		
		if(Protocol.PING.equals(cmd)) {
			return;
		} 
		
		if(cmd == null) { //Special case for favicon
			if(req.getBody() == null && "/favicon.ico".equals(req.getUrl())) {
				FileKit.INSTANCE.render(res, "static/favicon.ico");
				sess.write(res);
				return;
			} 
		}  
		
		if(config.verbose) { 
			String type = "<Request>";
			if(Protocol.ROUTE.equals(cmd)) type = "<Response>";
			logger.info(type + " " + sess.remoteAddress() + ": " + req); 
		}  
		
		//Nothing should change on message before check integrity!!
		if(!doAuth(req, sess)) return; 
		
		attachInfo(req, sess);  
		
		for(Filter filter : this.filterList) {
			boolean next = filter.doFilter(req, res);
			if(!next) {
				res.setHeader(Message.ID, req.getHeader(Message.ID));
				if(config.verbose) { 
					logger.info("<Response> " + sess.remoteAddress() + ": " + res); 
				}  
				sess.write(res);
				return;
			}
		}  
		
		cmd = req.removeHeader(Protocol.CMD); 
		if (cmd == null) {
			MsgKit.reply(req, 400, "cmd key required", sess); 
			return;
		}  
		
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

	public MqManager getMqManager() {
		return mqManager;
	}  
	public MqServerConfig getConfig() {
		return config;
	}
	public RpcProcessor getRpcProcessor() {
		return rpcProcessor;
	}
}
