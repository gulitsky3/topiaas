package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.transport.Message;

public class RpcServerSimpleExample {   
	//default: /plus/{a}/{b}
	public int plus(int a, int b) {
		return a+b;
	} 
	
	@RequestMapping(path="/abc") //default path could be changed
	public Object json() {
		Map<String, Object> value = new HashMap<>();
		value.put("key", System.currentTimeMillis());
		return value;
	}
	 
	/**
	 * Example of returning HTTP message type
	 * @return
	 */
	public Message home() { 
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>java home page</h1>");
		
		return res;
	}  
	 
	public static void main(String[] args) throws Exception {   
		RpcProcessor p = new RpcProcessor();    
		p.mount("/", RpcServerSimpleExample.class);     
		
		RpcServer rpcServer = RpcServerBuilder.embedded();
		rpcServer.setRpcProcessor(p);
		rpcServer.start();  
	} 
	 
}
