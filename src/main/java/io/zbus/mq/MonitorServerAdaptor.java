package io.zbus.mq;

import java.util.List;

import io.zbus.kit.FileKit;
import io.zbus.mq.Protocol.MqInfo;
import io.zbus.mq.plugin.MonitorUrlRouter;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.StaticResource;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;

public class MonitorServerAdaptor extends MqServerAdaptor {  
	
	public MonitorServerAdaptor(MqServerAdaptor mqServerAdaptor) {
		super(mqServerAdaptor);
		if (config.monitorServer != null && config.monitorServer.auth != null) {
			requestAuth = config.monitorServer.auth; 
		}  
		
		this.rpcProcessor = new RpcProcessor();
		StaticResource staticResource = new StaticResource();
		staticResource.setCacheEnabled(false); // TODO turn if off in production
		
		rpcProcessor.mount("/", new MonitorService()); 
		rpcProcessor.mount("/static", staticResource, false);
		rpcProcessor.mountDoc(); 
		
		this.urlRouter = new MonitorUrlRouter(rpcProcessor); 
		this.urlRouter.init(this);
	}
 
	
	class MonitorService {
		private FileKit fileKit = new FileKit();  
		
		@Route(path = "/favicon.ico", docEnabled = false)
		public Message favicon() {
			return fileKit.loadResource("static/favicon.ico");
		}
		
		@Route("/")
		public List<MqInfo> home() {  
			return mqManager.mqInfoList();
		}  
	} 
}

