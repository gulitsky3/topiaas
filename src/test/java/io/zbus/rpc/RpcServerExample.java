package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.rpc.biz.InterfaceExampleImpl;

 
public class RpcServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		RpcServer b = new RpcServer(); 
		
		FileService resource = new FileService();
		//re"module=example, method=getUsers2 Not Found"source.setBasePath("");
		//resource.setCacheEnabled(false);
		
		
		InterfaceExample example = new InterfaceExampleImpl();
		
		b.setStackTraceEnabled(false);
		//b.setAutoLoadService(true);
		//b.setMethodPageModule("m");
		b.addModule("example", example); 
		b.addModule("static", resource);
		
		b.setPort(8080);
		b.start();
	}
}
