package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.rpc.biz.InterfaceExampleImpl;

public class MqRpcServiceExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		RpcServer b = new RpcServer(); 
		
		FileService resource = new FileService();
		//resource.setBasePath("");
		//resource.setCacheEnabled(false); 
		
		InterfaceExample example = new InterfaceExampleImpl();
		
		b.setStackTraceEnabled(false);
		//b.setAutoLoadService(true);
		//b.setMethodPageModule("m");
		b.addModule("example", example); 
		b.addModule("static", resource);
		
		//connect to zbus
		b.setAddress("localhost:15555");
		b.setMq("MyRpc");
		b.start();
	} 
}
