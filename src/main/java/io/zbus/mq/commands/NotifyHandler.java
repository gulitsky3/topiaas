package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.kit.JsonKit;
import io.zbus.mq.NotifyManager;
import io.zbus.mq.NotifyManager.NotifyTarget;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class NotifyHandler implements CommandHandler {  
	private final NotifyManager notifyManager; 
	
	public NotifyHandler(NotifyManager notifyManager) { 
		this.notifyManager = notifyManager;
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		 NotifyTarget target = JsonKit.convert(req.getBody(), NotifyTarget.class); 
		 notifyManager.addNotifyTarget(target.port, target.urlPrefix, sess);
	} 
}
