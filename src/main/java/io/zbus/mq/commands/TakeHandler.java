package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.mq.MessageDispatcher;
import io.zbus.mq.MessageQueueManager;
import io.zbus.mq.Protocol;
import io.zbus.mq.model.MessageQueue;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class TakeHandler implements CommandHandler { 
	private final MessageDispatcher messageDispatcher;
	private final MessageQueueManager mqManager;  
	
	public TakeHandler(MessageDispatcher messageDispatcher, MessageQueueManager mqManager) {
		this.messageDispatcher = messageDispatcher;
		this.mqManager = mqManager; 
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		if(!validateRequest(req, sess)) return;
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL); 
		Integer window = req.getHeaderInt(Protocol.WINDOW); 
		String msgId = (String)req.getHeader(Protocol.ID);
		MessageQueue mq = mqManager.get(mqName); 
		if(window == null) window = 1; 
		
	    messageDispatcher.take(mq, channelName, window, msgId, sess); 
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
