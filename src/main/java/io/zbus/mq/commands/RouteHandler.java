package io.zbus.mq.commands;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.mq.Protocol;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class RouteHandler implements CommandHandler { 
	private static final Logger logger = LoggerFactory.getLogger(RouteHandler.class); 
	private final Map<String, Session> sessionTable; 
	
	public RouteHandler(Map<String, Session> sessionTable) { 
		this.sessionTable = sessionTable; 
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		String recver = (String)req.removeHeader(Protocol.TARGET);
		req.removeHeader(Protocol.SOURCE); 
		
		Session target = sessionTable.get(recver); 
		if(target != null) {
			target.write(req); 
		} else {
			logger.warn("Target=" + recver + " Not Found");
		}
		
		Boolean ack = req.getHeaderBool(Protocol.ACK);    
		if(ack != null && ack == true) {
			if(target == null) {
				MsgKit.reply(req, 404,  "Target=" + recver + " Not Found", sess);
			} else {
				MsgKit.reply(req, 200,  "OK", sess);
			}
			return;
		}  
	}  
}
