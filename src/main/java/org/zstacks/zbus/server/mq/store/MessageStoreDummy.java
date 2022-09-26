package org.zstacks.zbus.server.mq.store;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.zstacks.zbus.server.mq.MessageQueue;
import org.zstacks.znet.Message;
import org.zstacks.znet.log.Logger;
 
public class MessageStoreDummy implements MessageStore {
	private static final Logger log = Logger.getLogger(MessageStoreDummy.class);
	
	public void saveMessage(Message message) { 
		if(log.isDebugEnabled()){
			log.debug("Dummy save: "+ message);
		}
	}

	public void removeMessage(Message message) {  
		if(log.isDebugEnabled()){
			log.debug("Dummy remove: "+ message);
		}
	}
	
	public void onMessageQueueCreated(MessageQueue mq) { 
		
	}
	
	public void onMessageQueueRemoved(MessageQueue mq) { 
		
	}
	
	public ConcurrentMap<String, MessageQueue> loadMqTable() { 
		return new ConcurrentHashMap<String, MessageQueue>();
	}
	
	public void start() { 
	}
	
	public void shutdown() { 
		
	}
}
