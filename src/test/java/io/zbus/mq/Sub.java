package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
			Map<String, Object> req = new HashMap<>();
			req.put("cmd", "create"); //create MQ/Channel
			req.put("mq", mq); 
			req.put("mqType", mqType); 
			req.put("channel", channel);  
			Map<String, Object> res = client.invoke(req);
			System.out.println(res);
			
			Map<String, Object> sub = new HashMap<>();
			sub.put("cmd", "sub"); //Subscribe on MQ/Channel
			sub.put("mq", mq); 
			sub.put("channel", channel);
			client.invoke(sub, data->{
				System.out.println(data);
			});
		});
		
		client.connect();  
	} 
}
