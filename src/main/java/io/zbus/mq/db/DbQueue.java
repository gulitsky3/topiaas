package io.zbus.mq.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.MqInfo;
import io.zbus.mq.model.ChannelReader;
import io.zbus.mq.model.MessageQueue.AbstractMessageQueue;
import io.zbus.transport.Message;

public class DbQueue extends AbstractMessageQueue{
	
	public DbQueue(String mqName, String creator) { 
		super(mqName);
	}
	
	@Override
	public MqInfo info() {
		MqInfo info = new MqInfo();
		info.name = name();
		info.type = type();
		info.mask = getMask();
		info.messageDepth = size(); 
		info.channelList = new ArrayList<>(channels().values()); 
		
		return info;
	}
	
	@Override
	public String type() { 
		return Protocol.DB;
	}
	
	@Override
	public long size() { 
		return 0;
	}

	@Override
	public void write(Message message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(List<Message> messages) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Integer getMask() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMask(Integer mask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ChannelReader buildChannelReader(String channelId) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	 
}