package io.zbus.mq.commands;

import io.zbus.mq.MqManager;
import io.zbus.mq.Protocol;
import io.zbus.mq.model.MessageQueue;
import io.zbus.transport.Message;
import io.zbus.transport.Session;
import io.zbus.transport.http.Http;

public class MsgKit {
	public static void reply(Message req, int status, String message, Session sess) {
		Message res = new Message();
		res.setStatus(status);
		res.setBody(message);  
		res.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
		reply(req, res, sess);
	}
	
	public static void reply(Message req, Message res, Session sess) {
		if(req != null) {
			res.setHeader(Protocol.ID, (String)req.getHeader(Protocol.ID)); 
		}
		sess.write(res); 
	}
	
	public static boolean validateRequest(MqManager mqManager, Message req, Session sess) {
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
}
