package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExampleImpl;

public class MqRpcServiceExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		  
		RpcProcessor p = new RpcProcessor();
		p.addModule("example", InterfaceExampleImpl.class);  
		
		
		RpcServer server = new RpcServer();
		server.setProcessor(p);
		//connect to zbus
		server.setAddress("localhost:15555");
		server.setMq("MyRpc");
		server.start();
	} 
}
