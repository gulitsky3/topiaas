package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.mq.NotifyManager;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class BindHandler implements CommandHandler {  
	protected final NotifyManager notifyManager; 
	
	public BindHandler(NotifyManager notifyManager) { 
		this.notifyManager = notifyManager;
	}
	
	@Override
	public void handle(Message req, Session sess) throws IOException { 
		 
	} 
}
