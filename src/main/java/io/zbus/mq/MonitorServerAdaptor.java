package io.zbus.mq;

import java.io.IOException;

import io.zbus.kit.FileKit;
import io.zbus.rpc.RpcAuthFilter;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.StaticResource;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;

public class MonitorServerAdaptor extends ServerAdaptor {  
	private RpcProcessor processor;
	public MonitorServerAdaptor(MqServerConfig config) { 
		 if(config.monitorServer != null && config.monitorServer.auth != null) {
			 RpcAuthFilter authFilter = new RpcAuthFilter(config.monitorServer.auth);
			 processor.setAuthFilter(authFilter);
		 }
		 processor.mount("/", new MonitorService());
		 StaticResource staticResource = new StaticResource();
		 staticResource.setCacheEnabled(false); //TODO turn if off in production
		 processor.mount("/static", staticResource, false);
	} 
	
	public void onMessage(Object msg, Session sess) throws IOException {
		Message request = null;  
		if (!(msg instanceof Message)) { 
			throw new IllegalStateException("Not support message type");
		}
		request = (Message) msg;  
		
		if(Protocol.PING.equals(request.getHeader(Protocol.CMD))) {
			return; //ignore
		}
		Message response = new Message();
		processor.process(request, response);
		if(response.getStatus() == null) {
			response.setStatus(200);
		}
		sess.write(response);
	} 
}
 

class MonitorService {  
	private FileKit fileKit = new FileKit();
	
	@RequestMapping("/")
	public Message home() { 
		return fileKit.loadResource("static/home.htm");
	} 
	
	@RequestMapping(path="/favicon.ico", docEnabled=false)
	public Message favicon() { 
		return fileKit.loadResource("static/favicon.ico"); 
	}
}

