package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.mq.MessageDispatcher;
import io.zbus.mq.MessageQueueManager;
import io.zbus.mq.Protocol;
import io.zbus.mq.model.MessageQueue;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class PubHandler implements CommandHandler { 
	private final MessageDispatcher messageDispatcher;
	private final MessageQueueManager mqManager; 
	
	public PubHandler(MessageDispatcher messageDispatcher, MessageQueueManager mqManager) {
		this.messageDispatcher = messageDispatcher;
		this.mqManager = mqManager;
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		String mqName = (String)req.getHeader(Protocol.MQ);  
		if(mqName == null) {
			Reply.send(req, 400, "pub command, missing mq field", sess);
			return;
		}
		
		MessageQueue mq = mqManager.get(mqName);
		if(mq == null) { 
			if(mqName.equals("/")) {
				Reply.send(req, 200, "<h1>Welcome to zbus</h1>", sess);
				return;
			}
			Reply.send(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return; 
		} 
		
		mq.write(req); 
		Boolean ack = req.getHeaderBool(Protocol.ACK); 
		if (ack == null || ack == true) {
			String msg = String.format("OK, PUB (mq=%s)", mqName);
			Reply.send(req, 200, msg, sess);
		}
		
		messageDispatcher.dispatch(mq); 
	} 
}
