package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.rpc.biz.InterfaceExampleImpl;

 
public class RpcServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		RpcServer b = new RpcServer();    
		
		InterfaceExample example = new InterfaceExampleImpl();
		
		b.setStackTraceEnabled(false); 
		b.addModule("/", example);  
		
		b.setPort(8080);
		b.start();
	}
}
