package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.kit.FileKit;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.transport.Message;

public class RpcServerMQ {   
	private FileKit fileKit = new FileKit(false); 
	
	@RequestMapping("/")
	public Message home() { 
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>home page</h1>");
		
		return res;
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
	 
	@RequestMapping(path="/abc")
	public Object json() {
		Map<String, Object> value = new HashMap<>();
		value.put("key", System.currentTimeMillis());
		return value;
	}
	
	@RequestMapping(path="/favicon.ico", docEnabled=false)
	public Message favicon() { 
		return fileKit.loadResource("static/favicon.ico"); 
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		//static resource serving
		StaticResource resource = new StaticResource();
		resource.setBasePath("\\tmp"); 
		
		RpcProcessor p = new RpcProcessor();
		p.mount("/", RpcServerMQ.class); 
		p.mount("/static", resource);
		
		RpcServer server = new RpcServer(p);       
		server.setMqServerAddress("localhost:15555"); 
		server.setMq("/abc/def");
		
		//server.setAuthEnabled(true);
		//server.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		//server.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd"); 
		
		server.start();
	}
}
