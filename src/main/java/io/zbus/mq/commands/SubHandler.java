package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.mq.MessageDispatcher;
import io.zbus.mq.MessageQueueManager;
import io.zbus.mq.Protocol;
import io.zbus.mq.SubscriptionManager;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class SubHandler implements CommandHandler { 
	private final MessageDispatcher messageDispatcher;
	private final MessageQueueManager mqManager; 
	private final SubscriptionManager subscriptionManager;
	
	public SubHandler(MessageDispatcher messageDispatcher, MessageQueueManager mqManager, SubscriptionManager subscriptionManager) {
		this.messageDispatcher = messageDispatcher;
		this.mqManager = mqManager;
		this.subscriptionManager = subscriptionManager;
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		if(!validateRequest(req, sess)) return;
		
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL); 
		Boolean ack = req.getHeaderBool(Protocol.ACK); 
		if (ack == null || ack == true) {
			String msg = String.format("OK, SUB (mq=%s,channel=%s)", mqName, channelName); 
			Reply.send(req, 200, msg, sess);
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
		
		String filter = (String)req.getHeader(Protocol.FILTER); 
		if(filter != null) {
			sub.setFilter(filter); //Parse topic
		}    
		MessageQueue mq = mqManager.get(mqName);
		messageDispatcher.dispatch(mq, channelName); 
	} 
	
	private boolean validateRequest(Message req, Session sess) {
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL);
		if(mqName == null) {
			Reply.send(req, 400, "Missing mq field", sess);
			return false;
		}
		if(channelName == null) {
			Reply.send(req, 400, "Missing channel field", sess);
			return false;
		} 
		
		MessageQueue mq = mqManager.get(mqName); 
		if(mq == null) {
			Reply.send(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return false;
		}  
		if(mq.channel(channelName) == null) { 
			Reply.send(req, 404, "Channel(" + channelName + ") Not Found", sess);
			return false;
		}  
		return true;
	}
}
