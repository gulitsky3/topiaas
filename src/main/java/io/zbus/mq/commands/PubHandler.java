package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.mq.MessageDispatcher;
import io.zbus.mq.MqManager;
import io.zbus.mq.Protocol;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.plugin.MqMessageInterceptor;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class PubHandler implements CommandHandler { 
	private final MessageDispatcher messageDispatcher;
	private final MqManager mqManager; 
	
	private MqMessageInterceptor beforePub;
	
	public PubHandler(MessageDispatcher messageDispatcher, MqManager mqManager) {
		this.messageDispatcher = messageDispatcher;
		this.mqManager = mqManager;
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		String mqName = (String)req.getHeader(Protocol.MQ);  
		if(mqName == null) {
			MsgKit.reply(req, 400, "pub command, missing mq field", sess);
			return;
		}
		
		MessageQueue mq = mqManager.get(mqName);
		if(mq == null) {  
			MsgKit.reply(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return; 
		} 
		if(beforePub != null) {
			if(!beforePub.intercept(req)) return;
		}
		
		mq.write(req); 
		Boolean ack = req.getHeaderBool(Protocol.ACK); 
		if (ack == null || ack == true) {
			String msg = String.format("OK, PUB (mq=%s)", mqName);
			MsgKit.reply(req, 200, msg, sess);
		}
		
		messageDispatcher.dispatch(mq); 
	}

	public MqMessageInterceptor getBeforePub() {
		return beforePub;
	}

	public void setBeforePub(MqMessageInterceptor beforePub) {
		this.beforePub = beforePub;
	}  
}
