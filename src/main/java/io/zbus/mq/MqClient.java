package io.zbus.mq;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.transport.Client;
import io.zbus.transport.DataHandler;
import io.zbus.transport.Message; 

public class MqClient extends Client{ 
	private static final Logger logger = LoggerFactory.getLogger(MqClient.class); 
	private Map<String, Map<String,DataHandler<Message>>> handlerTable = new ConcurrentHashMap<>(); //mq=>{channel=>handler}
	
	public MqClient(String address) {  
		super(address);
		onMessage(msg->{
			handleMessage(msg);
		});
	}  
	
	public MqClient(MqServer server) {
		super(server.getServerAdaptor()); 
		onMessage(msg->{
			handleMessage(msg);
		});
	}
	
	private void handleMessage(Message response) throws Exception {
		boolean handled = handleInvokeResponse(response);
		if(handled) return;
		
		
		//Subscribed message pushing 
		String mq = (String)response.getHeader(Protocol.MQ);
		String channel = (String)response.getHeader(Protocol.CHANNEL);
		if(mq == null || channel == null) {
			logger.warn("MQ/Channel both required in reponse: " + JsonKit.toJSONString(response));
			return;
		} 
		Map<String,DataHandler<Message>> mqHandlers = handlerTable.get(mq);
		if(mqHandlers == null) return;
		DataHandler<Message> handler = mqHandlers.get(channel);
		if(handler == null) return;
		try {
			handler.handle(response); 
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	 
	public synchronized void heartbeat(long interval, TimeUnit timeUnit) {
		heartbeat(interval, timeUnit, ()->{
			Message msg = new Message();
			msg.addHeader(Protocol.CMD, Protocol.PING);
			return msg;
		});
	}  
	
	public void addMqHandler(String mq, String channel, DataHandler<Message> dataHandler) {
		Map<String,DataHandler<Message>> mqHandlers = handlerTable.get(mq);
		if(mqHandlers == null) {
			mqHandlers = new ConcurrentHashMap<>();
			handlerTable.put(mq, mqHandlers);
		}
		mqHandlers.put(channel, dataHandler);
	}  
}
