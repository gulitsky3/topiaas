package io.zbus.mq;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.transport.Message;

public class Sub { 
	public static MqClient buildInproClient() {
		MqServer server = new MqServer(new MqServerConfig());
		return new MqClient(server);
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqClient client = new MqClient("localhost:15555");   
		//MqClient client = buildInproClient();
		
		client.heartbeat(30, TimeUnit.SECONDS);
		
		final String mq = "MyMQ", channel = "MyChannel", mqType = Protocol.MEMORY;
		
		AtomicInteger count = new AtomicInteger(0);  
		client.addMqHandler(mq, channel, data->{ 
			if(count.getAndIncrement() % 10000 == 0) {
				System.out.println(data); 
			} 
		});  
		
		client.onOpen(()->{
			Message req = new Message();
			req.setHeader("cmd", "create"); //create MQ/Channel
			req.setHeader("mq", mq); 
			req.setHeader("mqType", mqType); 
			req.setHeader("channel", channel);  
			Message res = client.invoke(req);
			System.out.println(res);
			
			Message sub = new Message();
			sub.setHeader("cmd", "sub"); //Subscribe on MQ/Channel
			sub.setHeader("mq", mq); 
			sub.setHeader("channel", channel);
			client.invoke(sub, data->{
				System.out.println(data);
			});
		});
		
		client.connect();  
	} 
}
