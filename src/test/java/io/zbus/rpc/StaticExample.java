package io.zbus.rpc;

public class StaticExample {    
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		RpcProcessor p = new RpcProcessor();      
		
		StaticResource resource = new StaticResource(); 
		resource.setBasePath("static");   
		resource.setUrlPrefix("/static");
		
		p.mount("/static", resource);
		
		RpcServer rpcServer = new RpcServer(); 
		rpcServer.setRpcProcessor(p); 
		
		rpcServer.setMqServerAddress("localhost:15555");
		rpcServer.setMq("/");   
		rpcServer.start();  
	}  
}
