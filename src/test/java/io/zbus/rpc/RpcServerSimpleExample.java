package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.mq.MqServer;
import io.zbus.mq.MqServerConfig;
import io.zbus.rpc.annotation.Param;
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
		value.put("key1", System.currentTimeMillis());
		value.put("key2", System.currentTimeMillis());
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
	
	public Map<String, Object> p(@Param("name") String name, @Param("age")int age) {
		Map<String, Object> value = new HashMap<>();
		value.put("key1", name);
		value.put("key2", age);
		return value;
	}
	 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		RpcProcessor p = new RpcProcessor();    
		p.mount("/", RpcServerSimpleExample.class);     
		
		
		//based on MqServer
//		MqServerConfig config = new MqServerConfig();
//		config.setPublicServer("0.0.0.0:15555");
//		config.setMonitorServer("0.0.0.0:25555");
//		config.setVerbose(true); 
//		MqServer mqServer = new MqServer(config); 
//		mqServer.setRpcProcessor(p); 
		
		RpcServer rpcServer = new RpcServer(); 
		rpcServer.setMqServerAddress("localhost:15555");
		rpcServer.setMq("/");
		//rpcServer.setMqServer(mqServer); //embedded in MqServer
		rpcServer.setRpcProcessor(p); 
		rpcServer.start();  
	} 
	 
}
