package io.zbus.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.auth.AuthResult;
import io.zbus.auth.RequestAuth;
import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.StrKit;
import io.zbus.mq.Protocol.ChannelInfo;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
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
	private Map<String, CommandHandler> commandTable; 
	
	public MqServerAdaptor(MqServerConfig config) {
		subscriptionManager = new SubscriptionManager();  
		mqManager = new MessageQueueManager();
		
		messageDispatcher = new MessageDispatcher(subscriptionManager, sessionTable); 
		mqManager.mqDir = config.mqDiskDir; 
		
		mqManager.loadQueueTable();
		
		commandTable = new HashMap<>();
		commandTable.put(Protocol.PUB, pubHandler);
		commandTable.put(Protocol.SUB, subHandler);
		commandTable.put(Protocol.TAKE, takeHandler);
		commandTable.put(Protocol.ROUTE, routeHandler);
		commandTable.put(Protocol.CREATE, createHandler); 
		commandTable.put(Protocol.REMOVE, removeHandler); 
		commandTable.put(Protocol.QUERY, queryHandler); 
		commandTable.put(Protocol.PING, pingHandler); 
	} 
	
	public MqServerAdaptor duplicate() {
		MqServerAdaptor copy = new MqServerAdaptor(sessionTable);
		copy.subscriptionManager = subscriptionManager;
		copy.messageDispatcher = messageDispatcher;
		copy.mqManager = mqManager;
		copy.requestAuth = requestAuth;
		copy.commandTable = commandTable;
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
			reply(req, 400, "json format required", sess); 
			return;
		} 
		//check integrity 
		if(requestAuth != null) {
			AuthResult authResult = requestAuth.auth(req);
			if(!authResult.success) {
				reply(req, 403, authResult.message, sess); 
				return; 
			}
		}
		
		attachInfo(req, sess);
		
		handleUrlControl(req);
		
		String cmd = (String)req.removeHeader(Protocol.CMD); 
		
		if (cmd == null) {
			reply(req, 400, "cmd key required", sess); 
			return;
		} 
		cmd = cmd.toLowerCase();  
		
		CommandHandler handler = commandTable.get(cmd);
		if(handler == null) {
			reply(req, 404, "Command(" + cmd + ") Not Found", sess); 
			return; 
		}
		try {
			handler.handle(req, sess);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			reply(req, 500, e.getMessage(), sess); 
			return; 
		}
	}   
	

	private void handleUrlControl(Message msg) { 
		String url = msg.getUrl();
		if(url == null) return; 
		if(msg.getBody() != null) return;
		//CMD and MQ populated in header, use header control, no need parse URL
		if(msg.getHeader(Protocol.CMD) != null && msg.getHeader(Protocol.MQ) != null) {
			return;
		}
		 
		UrlInfo info = HttpKit.parseUrl(url);
		if(info.pathList.size()==0) { 
			for(Entry<String, String> e : info.queryParamMap.entrySet()) {
				String key = e.getKey();
				Object value = e.getValue();
				if(key.equals("body")) {
					msg.setBody(value);
					continue;
				}
				msg.setHeader(key.toLowerCase(), value);
			}
			return;
		}
		
		//Assumed to be RPC
		if(msg.getHeader(Protocol.CMD) == null) { // RPC assumed
			msg.setHeader(Protocol.CMD, Protocol.PUB);
			msg.setHeader(Protocol.ACK, false); //ACK should be disabled
		}   
		String mq = msg.getHeader(Protocol.MQ);
		if(mq == null) {
			if(info.pathList.size() > 0) {
				msg.setHeader(Protocol.MQ, info.pathList.get(0));
			}
		} 
	} 
	
	
	private CommandHandler createHandler = (req, sess) -> { 
		String mqName = (String)req.getHeader(Protocol.MQ);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return;
		}
		String mqType = (String)req.getHeader(Protocol.MQ_TYPE);
		Integer mqMask = req.getHeaderInt(Protocol.MQ_MASK); 
		String channel = (String)req.getHeader(Protocol.CHANNEL); 
		Integer channelMask = req.getHeaderInt(Protocol.CHANNEL_MASK);
		Long offset = req.getHeaderLong(Protocol.OFFSET);
		
		try {
			mqManager.saveQueue(mqName, mqType, mqMask, channel, offset, channelMask);
		} catch (IOException e) { 
			logger.error(e.getMessage(), e);
			
			reply(req, 500, e.getMessage(), sess);
			return;
		} 
		String msg = String.format("OK, CREATE (mq=%s,channel=%s)", mqName, channel); 
		if(channel == null) {
			msg = String.format("OK, CREATE (mq=%s)", mqName); 
		}
		reply(req, 200, msg, sess);
	};
	
	
	private CommandHandler removeHandler = (req, sess) -> { 
		String mqName = (String)req.getHeader(Protocol.MQ);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return;
		}
		String channel = (String)req.getHeader(Protocol.CHANNEL);
		try {
			mqManager.removeQueue(mqName, channel);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			reply(req, 500, e.getMessage(), sess);
			return;
		}
		String msg = String.format("OK, REMOVE (mq=%s,channel=%s)", mqName, channel); 
		if(channel == null) {
			msg = String.format("OK, REMOVE (mq=%s)", mqName); 
		}
		reply(req, 200, msg, sess);
	}; 
	
	private CommandHandler pingHandler = (req, sess) -> { 
		//ignore
	};  
	
	private CommandHandler pubHandler = (req, sess) -> {
		String mqName = (String)req.getHeader(Protocol.MQ);  
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return;
		}
		
		MessageQueue mq = mqManager.get(mqName);
		if(mq == null) { 
			reply(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return; 
		} 
		
		mq.write(req); 
		Boolean ack = req.getHeaderBool(Protocol.ACK); 
		if (ack == null || ack == true) {
			String msg = String.format("OK, PUB (mq=%s)", mqName);
			reply(req, 200, msg, sess);
		}
		
		messageDispatcher.dispatch(mq); 
	}; 
	
	private boolean validateRequest(Message req, Session sess) {
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return false;
		}
		if(channelName == null) {
			reply(req, 400, "Missing channel field", sess);
			return false;
		} 
		
		MessageQueue mq = mqManager.get(mqName); 
		if(mq == null) {
			reply(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return false;
		} 
		if(mq.channel(channelName) == null) { 
			reply(req, 404, "Channel(" + channelName + ") Not Found", sess);
			return false;
		} 
		
		return true;
	}
	
	private CommandHandler subHandler = (req, sess) -> { 
		if(!validateRequest(req, sess)) return;
		
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL); 
		Boolean ack = req.getHeaderBool(Protocol.ACK); 
		if (ack == null || ack == true) {
			String msg = String.format("OK, SUB (mq=%s,channel=%s)", mqName, channelName); 
			reply(req, 200, msg, sess);
		}
		
		Integer window = req.getHeaderInt(Protocol.WINDOW);
		Subscription sub = subscriptionManager.get(sess.id());
		if(sub == null) {
			sub = new Subscription();
			sub.clientId = sess.id(); 
			sub.mq = mqName;
			sub.channel = channelName; 
			sub.window = window;
			subscriptionManager.add(sub);
		} else {
			sub.window = window;
		}  
		
		String topic = (String)req.getHeader(Protocol.TOPIC);
		sub.topics.clear();
		if(topic != null) {
			sub.topics.add(topic); 
		}    
		MessageQueue mq = mqManager.get(mqName);
		messageDispatcher.dispatch(mq, channelName); 
	};
	
	private CommandHandler takeHandler = (req, sess) -> { 
		if(!validateRequest(req, sess)) return;
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL); 
		Integer window = req.getHeaderInt(Protocol.WINDOW); 
		String msgId = (String)req.getHeader(Protocol.ID);
		MessageQueue mq = mqManager.get(mqName); 
		if(window == null) window = 1; 
		
	    messageDispatcher.take(mq, channelName, window, msgId, sess); 
	};
	
	private CommandHandler routeHandler = (req, sess) -> {  
		String recver = (String)req.removeHeader(Protocol.TARGET);
		req.removeHeader(Protocol.SOURCE); 
		
		Session target = sessionTable.get(recver); 
		if(target != null) {
			target.write(req); 
		} else {
			logger.warn("Target=" + recver + " Not Found");
		}
		
		Boolean ack = req.getHeaderBool(Protocol.ACK);  
		if(ack != null && ack == true) {
			if(target == null) {
				reply(req, 404,  "Target=" + recver + " Not Found", sess);
			} else {
				reply(req, 200,  "OK", sess);
			}
			return;
		}  
	};
	
	private CommandHandler queryHandler = (req, sess) -> { 
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return;
		} 
		MessageQueue mq = mqManager.get(mqName); 
		if(mq == null) {
			reply(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return;
		} 
		if(channelName == null) { 
			Message res = new Message();
			res.setStatus(200);
			res.setBody(mq.info()); 
			reply(req, res, sess);
			return;
		} 
		
		ChannelInfo channel = mq.channel(channelName);
		if(channel == null) { 
			reply(req, 404, "Channel(" + channelName + ") Not Found", sess);
			return;
		}  
		
		Message res = new Message();
		res.setStatus(200);
		res.setBody(channel); 
		reply(req, res, sess);
		return;
	};
	
	private void reply(Message req, int status, String message, Session sess) {
		Message res = new Message();
		res.setStatus(status);
		res.setBody(message);  
		res.setHeader(Http.CONTENT_TYPE, "text/plain; charset=utf8");
		reply(req, res, sess);
	}
	
	private void reply(Message req, Message res, Session sess) {
		if(req != null) {
			res.setHeader(Protocol.ID, (String)req.getHeader(Protocol.ID)); 
		}
		sess.write(res); 
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
}

interface CommandHandler{
	void handle(Message msg, Session sess) throws IOException;
}
