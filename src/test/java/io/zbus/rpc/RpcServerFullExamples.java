package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.kit.FileKit;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.rpc.biz.InterfaceExampleImpl;
import io.zbus.transport.Message;

public class RpcServerFullExamples {  
	private FileKit fileKit = new FileKit(false);  
	
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
	@RequestMapping("/home") //Test: change to /  
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
	
	
	@RequestMapping("/showUpload")
	public Message showUpload() { 
		return fileKit.loadResource("page/upload.html"); 
	}
	
	@RequestMapping("/upload")
	public Message doUpload(Message req) {  
		FileKit.saveUploadedFile(req, "/tmp/upload");
		Message res = new Message();
		
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>Uploaded Success</h1>");
		
		return res;
	}  
	
	@RequestMapping(path="/favicon.ico", docEnabled=false)
	public Message favicon() { 
		return fileKit.loadResource("static/favicon.ico"); 
	}
	 
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
		
		//4) Enable doc on methods, optional
		p.setDocUrl("/"); //serve as home page at your service
		p.mountDoc();
		
		//Start RpcServer, capable of different modes: embedded, remote MQ, InProc
		RpcServer rpcServer = RpcServerBuilder.embedded();
		rpcServer.setRpcProcessor(p);
		rpcServer.start();  
	}  
}
