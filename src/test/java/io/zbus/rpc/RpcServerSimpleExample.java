package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;

public class RpcServerSimpleExample {   
	
	Template template;
	@Route(exclude=true)
	public void setTemplate(Template template) {
		this.template = template;
	}
	
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
	public Message home(Message req) {
		System.out.println(req);
		
		Map<String, Object> data = new HashMap<String, Object>();
        data.put("user", "Big Joe");   
        Map<String, Object> product = new HashMap<>();
        product.put("url", "/my");
        product.put("name", "Google");
        data.put("latestProduct", product); 
        
		return template.render("home.html", data); 
	}   
	 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		Template template = new Template();
		template.setTemplateDir("static");
		template.setUrlPrefix("/test");
		
		RpcServerSimpleExample example = new RpcServerSimpleExample();
		example.setTemplate(template);
		
		RpcProcessor p = new RpcProcessor();    
		p.mount("/", example);      
		 
		
		RpcServer rpcServer = new RpcServer(); 
		rpcServer.setRpcProcessor(p); 
		
		rpcServer.setMqServerAddress("localhost:15555");
		rpcServer.setMq("/");  
		rpcServer.start();  
	}  
}
