package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.mq.MessageDispatcher;
import io.zbus.mq.MqManager;
import io.zbus.mq.Protocol;
import io.zbus.mq.model.MessageQueue;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class TakeHandler implements CommandHandler { 
	private final MessageDispatcher messageDispatcher;
	private final MqManager mqManager;  
	
	public TakeHandler(MessageDispatcher messageDispatcher, MqManager mqManager) {
		this.messageDispatcher = messageDispatcher;
		this.mqManager = mqManager; 
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		if(!MsgKit.validateRequest(mqManager, req, sess)) return;
		
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL); 
		Integer window = req.getHeaderInt(Protocol.WINDOW); 
		String msgId = (String)req.getHeader(Protocol.ID);
		MessageQueue mq = mqManager.get(mqName); 
		if(window == null) window = 1; 
		
	    messageDispatcher.take(mq, channelName, window, msgId, sess); 
	} 
}
