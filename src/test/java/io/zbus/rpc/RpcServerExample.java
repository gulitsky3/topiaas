package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.transport.Message;

public class RpcServerExample {   
	
	@RequestMapping("/")
	public Message home() {
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>home page</h1>");
		
		return res;
	}
	
	@RequestMapping(path="/abc", method="POST" )
	public Object json() {
		Map<String, Object> value = new HashMap<>();
		value.put("key", System.currentTimeMillis());
		return value;
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		
		RpcProcessor p = new RpcProcessor();
		p.setDocModule("m");
		p.addModule(RpcServerExample.class); 
		
		RpcServer server = new RpcServer();      
		server.setProcessor(p); 
		server.setPort(8080);
		server.start();
	}
}
