package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.rpc.biz.InterfaceExampleImpl;

public class MqRpcServiceExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		RpcServer server = new RpcServer();  
		
		InterfaceExample example = new InterfaceExampleImpl(); 
		server.addModule("/", example);  
		
		//connect to zbus
		server.setAddress("localhost:15555");
		server.setMq("MyRpc");
		server.start();
	} 
}
