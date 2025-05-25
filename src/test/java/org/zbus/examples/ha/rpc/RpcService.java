package org.zbus.examples.ha.rpc;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.examples.rpc.appdomain.InterfaceExampleImpl;
import org.zbus.mq.ConsumerService;
import org.zbus.mq.ConsumerServiceConfig;
import org.zbus.rpc.RpcProcessor;

public class RpcService {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException{     
		RpcProcessor processor = new RpcProcessor();  
		processor.addModule(new InterfaceExampleImpl());  
		
		Broker broker = new ZbusBroker("127.0.0.1:16666;127.0.0.1:16667");
		//Broker broker = new ZbusBroker("127.0.0.1:15555");

		ConsumerServiceConfig config = new ConsumerServiceConfig();
		config.setConsumerCount(2); 
		config.setMq("MyRpc"); 
		config.setBroker(broker);    
		config.setMessageProcessor(processor); 
		config.setVerbose(true);
		
		ConsumerService svc = new ConsumerService(config);
		svc.start();  
	}
}
