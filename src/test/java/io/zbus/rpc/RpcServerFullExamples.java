package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import io.zbus.kit.FileKit;
import io.zbus.mq.MqServer;
import io.zbus.mq.MqServerConfig;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Route;
import io.zbus.rpc.biz.InterfaceExampleImpl;
import io.zbus.rpc.biz.model.User;
import io.zbus.transport.Message;

public class RpcServerFullExamples {  
	private FileKit fileKit = new FileKit(false);  
	
	
	//default: /plus/{a}/{b}
	public int plus(int a, int b) {
		return a+b;
	}  
	
	@Route("/abc") //default path could be changed
	public Object json() {
		Map<String, Object> value = new HashMap<>();
		value.put("key", System.currentTimeMillis());
		return value;
	}
	
	
	public Object p(User p) {
		return p;
	}
	 
	/**
	 * Example of returning HTTP message type
	 * @return
	 */
	@Route("/home") //Test: change to /  
	public Message home() { 
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>java home page</h1>");
		
		return res;
	}  
	
	
	public Message test2() { 
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("Content-Type", "application/json; charset=utf-8"); 
		res.setBody("test");
		
		return res;
	}  
	
	public Map<String, Object> p(@Param("name") String name, @Param("age")int age) {
		Map<String, Object> value = new HashMap<>();
		value.put("key1", name);
		value.put("key2", age);
		return value;
	}

	public JSONObject retJson() {
		JSONObject j = new JSONObject();
		j.put("code", "0");
		j.put("body", "the body xx");
		return j;
	}

	public Map<String, Object> nullValue() {
		Map<String, Object> value = new HashMap<>();
		value.put("key1", null);
		value.put("key2", "hi");
		return value;
	} 
	 
	@Route("/showUpload")
	public Message showUpload() { 
		return fileKit.render("page/upload.html"); 
	}
	
	@Route("/upload")
	public Message doUpload(Message req) {  
		FileKit.saveUploadedFile(req, "/tmp/upload");
		Message res = new Message();
		
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>Uploaded Success</h1>");
		
		return res;
	}  
	
	@Route(path="/favicon.ico", docEnabled=false)
	public Message favicon() { 
		return fileKit.render("static/favicon.ico"); 
	}
	 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		RpcProcessor p = new RpcProcessor();   
		//1) mount two java class to different root URLs
		p.mount("/", RpcServerFullExamples.class);  
		p.mount("/example", InterfaceExampleImpl.class);
		
		//2) Serve static files
		StaticResource resource = new StaticResource(); 
		resource.setBasePath("\\tmp");   
		p.mount("/static", resource);
		
		
		//3) Dynamically insert a method
		RpcMethod spec = new RpcMethod(); 
		spec.urlPath = "/dynamic/func1";
		spec.method = "func1";
		spec.addParam(String.class, "name");
		spec.addParam(Integer.class, "age"); 
		
		spec.returnType = Map.class.getName(); 
		p.mount(spec, new GenericService());    
		
		p.setExceptionFilter(new RpcFilter() { 
			@Override
			public boolean doFilter(Message request, Message response, Throwable exception) { 
				response.setStatus(500);
				response.setBody(exception.getMessage()); 
				return false;
			}
		});
		
		MqServerConfig config = new MqServerConfig("0.0.0.0", 15555);
		config.setVerbose(false);
		
		RpcServer rpcServer = new RpcServer(); 
		rpcServer.setRpcProcessor(p); 
		p.setDocFile("rpc.html");
		
		//rpcServer.setMqServerAddress("localhost:15555");
		rpcServer.setMq("/");  
		rpcServer.setMqServer(new MqServer(config));
		rpcServer.start();  
	}  
}
