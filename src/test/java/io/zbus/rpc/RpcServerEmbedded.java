package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.mq.MqServer;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.transport.Message;

public class RpcServerEmbedded {  
	  
	public Message home() { 
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>java home page</h1>");
		
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
		p.setDocUrl("/");
		p.mount("/example", RpcServerEmbedded.class);     
		
		StaticResource resource = new StaticResource(); 
		resource.setBasePath("\\tmp"); 
		p.mount("/static", resource);
		
		//Serve RPC embedded in MqServer 
		MqServer mqServer = new MqServer(15555);    
		RpcServer rpcServer = new RpcServer(p);
		rpcServer.setMqServer(mqServer); 
		rpcServer.start();
	} 
}
