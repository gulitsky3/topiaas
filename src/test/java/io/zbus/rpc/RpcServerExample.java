package io.zbus.rpc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.zbus.kit.FileKit;
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
	
	@RequestMapping("/favicon.ico")
	public Message favicon() { 
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("content-type", "image/x-icon"); 
		byte[] data = new byte[0];
		try {
			data = FileKit.INSTANCE.loadFileBytes("favicon.ico");
		} catch (IOException e) {  
			res.setStatus(500);
		}
		res.setBody(data); 
		return res;
	}
	 
	@RequestMapping(path="/abc")
	public Object json() {
		Map<String, Object> value = new HashMap<>();
		value.put("key", System.currentTimeMillis());
		return value;
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		
		RpcProcessor p = new RpcProcessor();
		StaticResource resource = new StaticResource();
		resource.setBasePath("/tmp");
		p.setDocModule("m");
		p.addModule("", RpcServerExample.class); 
		p.addModule("static", resource);
		
		RpcServer server = new RpcServer();      
		server.setProcessor(p); 
		server.setPort(8080);
		server.start();
	}
}
