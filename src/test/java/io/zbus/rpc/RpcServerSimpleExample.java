package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;

public class RpcServerSimpleExample {   
	
	public int plus(int a, int b) {
		return a+b;
	}  
	
	public Map<String, Object> p(@Param("name") String name, @Param("age")int age) {
		Map<String, Object> value = new HashMap<>();
		value.put("key1", name);
		value.put("key2", age);
		value.put("nullKey", null);
		return value;
	}
	
	@Route("/abc") //default path could be changed
	public Object json() {
		Map<String, Object> value = new HashMap<>();
		value.put("key1", System.currentTimeMillis());
		value.put("key2", System.currentTimeMillis());
		return value;
	}
	 
	@Route("/")
	public Message home() { 
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>java home page</h1>");
		
		return res;
	}  
	
	@Route("/log")
	public Message log() { 
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>my log</h1>");
		
		return res;
	}  
	 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		RpcProcessor p = new RpcProcessor();    
		p.mount("/", RpcServerSimpleExample.class);      
		 
		
		RpcServer rpcServer = new RpcServer(); 
		rpcServer.setRpcProcessor(p); 
		
		rpcServer.setMqServerAddress("localhost:15555");
		rpcServer.setMq("/");  
		rpcServer.start();  
	}  
}
