package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.annotation.Filter;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;

@Route(value="/")
@Filter("login")
public class RpcServerSimpleExample {    
	
	public int plus(int a, int b) {
		return a+b;
	}  
	
	@Filter(exclude = "login")
	public Map<String, Object> p(@Param("name") String name, @Param("age")int age) {
		Map<String, Object> value = new HashMap<>();
		value.put("name", name);
		value.put("age", age);
		value.put("nullKey", null);
		System.out.println(name);
		return value;
	}
	 
	@Route("/abc") //default path could be changed
	public Object json() {
		Map<String, Object> value = new HashMap<>();
		value.put("key1", System.currentTimeMillis());
		value.put("key2", System.currentTimeMillis());
		return value;
	} 
	
	@Route("/") //default path could be changed
	public Message home(Message req) {
		System.out.println(req);
		Message res = new Message();
		res.setHeader("content-type", "text/html; charset=utf8");
		res.setBody("<h1>test body</h1>");
		return res;
	} 
	
	 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {     
		RpcProcessor p = new RpcProcessor();    
		p.mount("/", RpcServerSimpleExample.class);      
		
		//p.setBeforeFilter(new MyFilter());
		
		RpcServer rpcServer = new RpcServer(); 
		rpcServer.setRpcProcessor(p); 
		
		rpcServer.setMqServerAddress("localhost:15555");
		rpcServer.setMq("/jhb");  
		rpcServer.start();  
	}  
}
