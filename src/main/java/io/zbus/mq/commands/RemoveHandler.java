package io.zbus.mq.commands;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.mq.MessageQueueManager;
import io.zbus.mq.Protocol;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class RemoveHandler implements CommandHandler { 
	private static final Logger logger = LoggerFactory.getLogger(RemoveHandler.class);  
	private final MessageQueueManager mqManager; 
	
	public RemoveHandler(MessageQueueManager mqManager) { 
		this.mqManager = mqManager;
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		String mqName = (String)req.getHeader(Protocol.MQ);
		if(mqName == null) {
			Reply.send(req, 400, "remove command, missing mq field", sess);
			return;
		}
		String channel = (String)req.getHeader(Protocol.CHANNEL);
		try {
			mqManager.removeQueue(mqName, channel);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			Reply.send(req, 500, e.getMessage(), sess);
			return;
		}
		String msg = String.format("OK, REMOVE (mq=%s,channel=%s)", mqName, channel); 
		if(channel == null) {
			msg = String.format("OK, REMOVE (mq=%s)", mqName); 
		}
		Reply.send(req, 200, msg, sess);
	} 
}
