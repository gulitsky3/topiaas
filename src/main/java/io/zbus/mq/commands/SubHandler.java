package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.mq.MessageDispatcher;
import io.zbus.mq.MqManager;
import io.zbus.mq.Protocol;
import io.zbus.mq.SubscriptionManager;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class SubHandler implements CommandHandler { 
	private final MessageDispatcher messageDispatcher;
	private final MqManager mqManager; 
	private final SubscriptionManager subscriptionManager;
	
	public SubHandler(MessageDispatcher messageDispatcher, MqManager mqManager, SubscriptionManager subscriptionManager) {
		this.messageDispatcher = messageDispatcher;
		this.mqManager = mqManager;
		this.subscriptionManager = subscriptionManager;
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		if(!MsgKit.validateRequest(mqManager, req, sess)) return;
		
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL); 
		Boolean ack = req.getHeaderBool(Protocol.ACK); 
		if (ack == null || ack == true) {
			String msg = String.format("OK, SUB (mq=%s,channel=%s)", mqName, channelName); 
			MsgKit.reply(req, 200, msg, sess);
		}
		
		Integer window = req.getHeaderInt(Protocol.WINDOW);
		Subscription sub = subscriptionManager.get(sess.id());
		if(sub == null) {
			sub = new Subscription();
			sub.clientId = sess.id(); 
			sub.clientAddress = sess.remoteAddress();
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
}
