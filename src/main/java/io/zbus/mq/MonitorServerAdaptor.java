package io.zbus.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.auth.AuthResult;
import io.zbus.auth.RequestAuth;
import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.mq.Protocol.MqInfo;
import io.zbus.mq.commands.CommandHandler;
import io.zbus.mq.commands.CreateHandler;
import io.zbus.mq.commands.QueryHandler;
import io.zbus.mq.commands.RemoveHandler;
import io.zbus.mq.commands.Reply;
import io.zbus.mq.model.MessageQueue;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.StaticResource;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;

public class MonitorServerAdaptor extends ServerAdaptor {
	private static final Logger logger = LoggerFactory.getLogger(MonitorServerAdaptor.class); 
	private RpcProcessor rpcProcessor = new RpcProcessor();
	private MessageQueueManager mqManager;
	protected SubscriptionManager subManager;
	
	private RequestAuth requestAuth; 
	
	private Map<String, CommandHandler> commandTable = new HashMap<>();
	
	public MonitorServerAdaptor(MqServerConfig config, MessageQueueManager mqManager, SubscriptionManager subManager) {
		if (config.monitorServer != null && config.monitorServer.auth != null) {
			requestAuth = config.monitorServer.auth; 
		}
		
		this.mqManager = mqManager;
		this.subManager = subManager;
		
		commandTable.put(Protocol.CREATE, new CreateHandler(mqManager)); 
		commandTable.put(Protocol.REMOVE, new RemoveHandler(mqManager)); 
		commandTable.put(Protocol.QUERY, new QueryHandler(mqManager));  
		commandTable.put(Protocol.PING, (req, sess)->{}); 
 
		
		rpcProcessor.mount("/", new MonitorService());
		rpcProcessor.mountDoc();
		StaticResource staticResource = new StaticResource();
		staticResource.setCacheEnabled(false); // TODO turn if off in production
		rpcProcessor.mount("/static", staticResource, false);
	}

	public void onMessage(Object msg, Session sess) throws IOException {
		Message req = (Message) msg;   
		//check integrity 
		if(requestAuth != null) {
			AuthResult authResult = requestAuth.auth(req);
			if(!authResult.success) {
				Reply.send(req, 403, authResult.message, sess); 
				return; 
			}
		}   
		
		String cmd = req.getHeader(Protocol.CMD); 
		if(cmd == null && req.getUrl() != null) { 
			String url = req.getUrl(); 
			UrlInfo info = HttpKit.parseUrl(url); 
			if(info.pathList.size()==0 && !info.queryParamMap.isEmpty()) { //special for command control in headers
				for(Entry<String, String> e : info.queryParamMap.entrySet()) {
					String key = e.getKey(); 
					req.setHeader(key.toLowerCase(), e.getValue());
				}    
			} else { 
				if(rpcProcessor != null) {
					if(rpcProcessor.matchUrl(url)) {
						Message res = new Message();
						rpcProcessor.process(req, res);
						sess.write(res); 
						return;
					} 
				} 
			}
		} 
		
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
	
	class MonitorService {
		private FileKit fileKit = new FileKit();

		@RequestMapping("/")
		public Message home() {
			return fileKit.loadResource("static/home.htm");
		}

		@RequestMapping(path = "/favicon.ico", docEnabled = false)
		public Message favicon() {
			return fileKit.loadResource("static/favicon.ico");
		}
		 
		public Object query(String mq, String channel) { 
			MessageQueue q = mqManager.get(mq);
			if(channel == null) { 
				if(q == null) {
					return null;
				}
				return q.info();
			}
			return q.channel(channel);
		}

		public List<MqInfo> mqInfoList() {
			return mqManager.mqInfoList();
		} 
	} 
}

