package io.zbus.mq.commands;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.mq.MessageQueueManager;
import io.zbus.mq.Protocol;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class CreateHandler implements CommandHandler { 
	private static final Logger logger = LoggerFactory.getLogger(CreateHandler.class);  
	private final MessageQueueManager mqManager; 
	
	public CreateHandler(MessageQueueManager mqManager) { 
		this.mqManager = mqManager;
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		String mqName = (String)req.getHeader(Protocol.MQ);
		if(mqName == null) {
			Reply.send(req, 400, "create command, missing mq field", sess);
			return;
		}
		String mqType = (String)req.getHeader(Protocol.MQ_TYPE);
		Integer mqMask = req.getHeaderInt(Protocol.MQ_MASK); 
		String channel = (String)req.getHeader(Protocol.CHANNEL); 
		Integer channelMask = req.getHeaderInt(Protocol.CHANNEL_MASK);
		Long offset = req.getHeaderLong(Protocol.OFFSET);
		String creator = sess.remoteAddress();
		try {
			mqManager.saveQueue(mqName, mqType, mqMask, channel, offset, channelMask, creator);
		} catch (IOException e) { 
			logger.error(e.getMessage(), e);
			
			Reply.send(req, 500, e.getMessage(), sess);
			return;
		} 
		String msg = String.format("OK, CREATE (mq=%s,channel=%s)", mqName, channel); 
		if(channel == null) {
			msg = String.format("OK, CREATE (mq=%s)", mqName); 
		}
		Reply.send(req, 200, msg, sess);
	} 
}
