package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExampleImpl;

public class RpcServerMQ {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		  
		RpcProcessor p = new RpcProcessor();
		p.setUrlPrefix("/MyRpc");
		p.addModule("example", InterfaceExampleImpl.class);  
		
		
		RpcServer server = new RpcServer(p); 
		//connect to zbus
		server.setAddress("localhost:15555");
		server.setMq("MyRpc"); 
		//MQ authentication, no need to configure if use HTTP direct RPC
		//server.setAuthEnabled(true);
		//server.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		//server.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd"); 
		
		server.start();
	} 
}
