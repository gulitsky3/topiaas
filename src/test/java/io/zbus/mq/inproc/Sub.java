package io.zbus.mq.inproc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.mq.MqClient;
import io.zbus.mq.MqServer;
import io.zbus.mq.MqServerConfig;
import io.zbus.transport.Message;

public class Sub { 
	
	@SuppressWarnings({ "resource" })
	public static void main(String[] args) throws Exception { 
		MqServer server = new MqServer(new MqServerConfig());
		MqClient client = new MqClient(server);    
		client.heartbeat(30, TimeUnit.SECONDS);
		
		final String mq = "DiskQ", channel = "MyChannel";
		AtomicInteger count = new AtomicInteger(0);  
		client.addMqHandler(mq, channel, data->{
			if(count.getAndIncrement() % 10000 == 0) {
				System.out.println(data); 
			} 
		});  
		
		client.onOpen(()->{
			Message req = new Message();
			req.addHeader("cmd", "create"); //create MQ/Channel
			req.addHeader("mq", mq); 
			req.addHeader("mqType", "disk"); //Set as Disk type
			req.addHeader("channel", channel);  
			client.invoke(req, res->{
				System.out.println(res);
			});  
			
			Message sub = new Message();
			sub.addHeader("cmd", "sub"); //Subscribe on MQ/Channel
			sub.addHeader("mq", mq); 
			sub.addHeader("channel", channel);
			client.invoke(sub, res->{
				System.out.println(res);
			});
		});
		
		client.connect();  
	} 
}
