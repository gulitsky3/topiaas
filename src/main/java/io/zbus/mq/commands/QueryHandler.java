package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.mq.MqManager;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ChannelInfo;
import io.zbus.mq.model.MessageQueue;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class QueryHandler implements CommandHandler {  
	private final MqManager mqManager; 
	
	public QueryHandler(MqManager mqManager) { 
		this.mqManager = mqManager;
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		String mqName = (String)req.getHeader(Protocol.MQ);
		String channelName = (String)req.getHeader(Protocol.CHANNEL);
		if(mqName == null) {
			Message res = new Message();
			res.setStatus(200);
			res.setBody(mqManager.mqInfoList());
			MsgKit.reply(req, res, sess);
			return;
		} 
		
		MessageQueue mq = mqManager.get(mqName); 
		if(mq == null) {
			MsgKit.reply(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return;
		} 
		if(channelName == null) { 
			Message res = new Message();
			res.setStatus(200);
			res.setBody(mq.info()); 
			MsgKit.reply(req, res, sess);
			return;
		} 
		
		ChannelInfo channel = mq.channel(channelName);
		if(channel == null) { 
			MsgKit.reply(req, 404, "Channel(" + channelName + ") Not Found", sess);
			return;
		}  
		
		Message res = new Message();
		res.setStatus(200);
		res.setBody(channel); 
		MsgKit.reply(req, res, sess); 
	} 
}
