package io.zbus.rpc;

import io.zbus.mq.MqServer;
import io.zbus.rpc.biz.InterfaceExampleImpl;

public class RpcServerMQInproc {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		  
		RpcProcessor p = new RpcProcessor(); 
		p.mount("/", InterfaceExampleImpl.class);  
		
		
		//Serve RPC via MQ Server InProc 
		MqServer mqServer = new MqServer(15555);   
		
		RpcServer rpc = new RpcServer(p);   
		rpc.setMqServer(mqServer); //InProc MqServer
		rpc.setMq("/");        //Choose MQ to group Service physically
		 
		//server.setAuthEnabled(true);
		//server.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		//server.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd"); 
		
		rpc.start();
	} 
}
