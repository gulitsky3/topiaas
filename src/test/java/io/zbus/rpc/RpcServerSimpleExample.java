package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.annotation.Filter;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;
 
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
	
	public Map<String, Object> map(Map<String, Object> table) {
		System.out.println(table);
		return table;
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
		
		p.setBeforeFilter(new RpcFilter() { 
			@Override
			public boolean doFilter(Message request, Message response, Throwable exception) {
				Map<String, Object> ctx = new HashMap<>();
				ctx.put("key", "set in before filter");
				request.setContext(ctx );
				return true;
			}
		});
		
		p.setAfterFilter(new RpcFilter() { 
			@Override
			public boolean doFilter(Message request, Message response, Throwable exception) {
				Object ctx = request.getContext();
				System.out.println("In After Filter>>>>>" + ctx);
				return true;
			}
		});
		
		RpcServer rpcServer = new RpcServer(); 
		rpcServer.setRpcProcessor(p); 
		//rpcServer.setChannel("temp");
		//rpcServer.setRouteDisabled(true);
		
		rpcServer.setMqServerAddress("localhost:15555");
		rpcServer.setMq("/"); 
		//rpcServer.setMqServer(new MqServer(15555));
		rpcServer.start();  
	}  
}
