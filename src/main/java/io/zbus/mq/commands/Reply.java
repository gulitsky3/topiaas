package io.zbus.mq.commands;

import io.zbus.mq.Protocol;
import io.zbus.transport.Message;
import io.zbus.transport.Session;
import io.zbus.transport.http.Http;

public class Reply {
	public static void send(Message req, int status, String message, Session sess) {
		Message res = new Message();
		res.setStatus(status);
		res.setBody(message);  
		res.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
		send(req, res, sess);
	}
	
	public static void send(Message req, Message res, Session sess) {
		if(req != null) {
			res.setHeader(Protocol.ID, (String)req.getHeader(Protocol.ID)); 
		}
		sess.write(res); 
	}
}
