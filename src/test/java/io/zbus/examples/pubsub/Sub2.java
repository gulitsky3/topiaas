package io.zbus.examples.pubsub;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Consumer;
import io.zbus.mq.Consumer.ConsumerHandler;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.net.http.Message;

public class Sub2 {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		Consumer c = new Consumer(broker, "MyMQ");  
		
		//control more details
		ConsumeGroup group = new ConsumeGroup("Group2"); 
		c.setConsumeGroup(group);  
		c.declareMQ();
		
		c.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException {   
				System.out.println(msg); 
			}
		});    
	} 
}
