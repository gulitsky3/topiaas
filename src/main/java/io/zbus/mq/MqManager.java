package io.zbus.mq;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.mq.Protocol.ChannelInfo;
import io.zbus.mq.Protocol.MqInfo;
import io.zbus.mq.disk.DiskQueue;
import io.zbus.mq.memory.MemoryQueue;
import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;

public class MqManager {  
	private static final Logger logger = LoggerFactory.getLogger(MqManager.class);
	public String mqDir = "/tmp/zbus";
	public String dbConnectionString;
	
	private Map<String, MessageQueue> mqTable = new ConcurrentHashMap<>(); //lower cased
	
	public void loadQueueTable() {
		logger.info("Loading MQ from disk ..."); 
		File[] mqDirs = new File(this.mqDir).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		
		if(mqDirs != null && mqDirs.length > 0) {
			for (File dir : mqDirs) { 
				try {
					MessageQueue mq = new DiskQueue(dir.getName(), new File(this.mqDir)); 
					mqTable.put(mq.name(), mq);
					logger.info("MQ({}) loaded", mq.name()); 
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					continue;
				} 
			}
		}   
	}
	
	public Set<String> mqNames(){ 
		return mqTable.keySet(); //TODO make it threadsafe
	}
	
	
	public MessageQueue get(String mqName) {
		if(mqName == null) mqName = "";
		return mqTable.get(mqName);
	} 
	
	public MessageQueue saveQueue(String mqName, String channel, String creator) throws IOException { 
		return saveQueue(mqName, Protocol.MEMORY, null, channel, null, null, creator);
	}
	
	/**
	 * 
	 * Create MQ or Channel of MQ
	 * 
	 * @param mqName name of message queue
	 * @param mqType type of mq
	 * @param channel channel name of mq
	 * @return created/updated mq
	 * @throws IOException 
	 */
	public synchronized MessageQueue saveQueue(
			String mqName, String mqType, Integer mqMask, 
			String channel, Long channelOffset, Integer channelMask,
			String creator
			) throws IOException { 
		
		if(mqName == null) {
			throw new IllegalArgumentException("Missing mqName");
		} 
		if(mqType == null) {
			mqType = Protocol.MEMORY;
		}
		
		MessageQueue mq = mqTable.get(mqName); 
		if(mq == null) {
			if(Protocol.MEMORY.equals(mqType)) {
				mq = new MemoryQueue(mqName, creator);
			} else if (Protocol.DISK.equals(mqType)) {
				mq = new DiskQueue(mqName, new File(mqDir), creator);
			} else {
				throw new IllegalArgumentException("mqType(" + mqType + ") Not Support");
			}  
			mqTable.put(mqName, mq);
		}
		
		mq.setMask(mqMask); 
		
		if(channel != null) {
			Channel ch = new Channel(channel, channelOffset);  
			ch.mask = channelMask;
			mq.saveChannel(ch);
		}
		
		return mq;
	} 
	
	/**
	 * Remove MQ or Channel of MQ
	 * 
	 * @param mq name of mq
	 * @param channel channel of mq
	 */ 
	public void removeQueue(String mq, String channel) throws IOException { 
		if(channel == null) {
			MessageQueue q = mqTable.remove(mq);
			if(q != null) {
				q.destroy();
			}
			return;
		}
		
		MessageQueue q = mqTable.get(mq);
		if(q != null) {
			q.removeChannel(channel);
		}
	} 
	
	public List<MqInfo> mqInfoList() {
		List<MqInfo> res = new ArrayList<>();
		for(Entry<String, MessageQueue> e : mqTable.entrySet()) {
			MessageQueue mq = e.getValue();
			res.add(mq.info());
		}
		return res;
	}
	
	public ChannelInfo channelInfo(String mq, String channel) {
		MessageQueue q = mqTable.get(mq);
		if(q == null) return null;
		return q.channel(channel);
	}
}
