package io.zbus.mq.model.disk;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.mq.Protocol;
import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.disk.support.DiskMessage;
import io.zbus.mq.model.disk.support.Index;
import io.zbus.mq.model.disk.support.QueueWriter;

public class DiskQueue implements MessageQueue {
	private static final Logger logger = LoggerFactory.getLogger(DiskQueue.class); 
	final Index index;     
	private final QueueWriter writer; 
	private String name;
	
	public DiskQueue(String mqName, File baseDir) throws IOException { 
		this.name = mqName;
		File mqDir = new File(baseDir, mqName);
		index = new Index(mqDir);
		writer = new QueueWriter(index);
		
		loadChannels();
	} 
	
	private void loadChannels() {
		File[] channelFiles = index.getReaderDir().listFiles( pathname-> {
			return Index.isReaderFile(pathname); 
		});
        if (channelFiles != null && channelFiles.length> 0) {
            for (File channelFile : channelFiles) {  
            	String channelName = channelFile.getName();
            	channelName = channelName.substring(0, channelName.lastIndexOf('.'));  
            }
        } 
	}
	
	@Override
	public String name() { 
		return name;
	}

	@Override
	public void write(Map<String, Object> message) { 
		try { 
			DiskMessage diskMsg = new DiskMessage();
			diskMsg.id = (String)message.get(Protocol.ID);
			diskMsg.tag = (String)message.get(Protocol.TOPIC);
			diskMsg.body = JsonKit.toJSONBytes(message, "UTF8");
			writer.write(diskMsg); 
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		
	}

	@Override
	public List<Map<String, Object>> read(String channelId, int count) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel channel(String channelId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addChannel(Channel channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeChannel(String channelId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Channel> channels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getMask() {
		return index.getMask(); 
	}
	
	@Override
	public void setMask(Integer mask) {
		index.setMask(mask);
	}

	@Override
	public void flush() { 
		
	}
	
	@Override
	public void destroy() { 
		try {
			writer.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		try {
			index.delete();
		} catch (IOException e) { 
			logger.error(e.getMessage(), e);
		} 
	} 
}
