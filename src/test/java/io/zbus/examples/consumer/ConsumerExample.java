package io.zbus.examples.consumer;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.ConsumeHandler;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MqClient;

public class ConsumerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Broker broker = new Broker("conf/broker.xml");   
		
		ConsumerConfig config = new ConsumerConfig(broker);
		config.setTopic("MyTopic");
		config.setConsumeThreadCount(2);      // connection count to each server in broker
		config.setConsumeRunnerPoolSize(64);  // consume handler running thread pool size
		config.setConsumeHandler(new ConsumeHandler() { 
			@Override
			public void handle(Message msg, MqClient client) throws IOException {
				System.out.println(msg);
			}
		});
		
		Consumer consumer = new Consumer(config);
		consumer.start(); 
	}

}
