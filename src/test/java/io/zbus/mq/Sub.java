package io.zbus.mq;

import java.util.concurrent.TimeUnit;

import io.zbus.transport.Message;

public class Sub {  
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqClient client = new MqClient("wss://zbus.io");    
		
		client.heartbeat(30, TimeUnit.SECONDS);
		
		final String mq = "MyMQ", channel = "MyChannel", mqType = Protocol.MEMORY;
		
		client.addMqHandler(mq, channel, data->{  
			System.out.println(data);  
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
			sub.setHeader("window", 1);
			//sub.setHeader("filter", "abc");
			
			client.invoke(sub, data->{
				System.out.println(data);
			});
		});
		
		client.connect();  
	} 
}
