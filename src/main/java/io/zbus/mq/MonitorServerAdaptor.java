package io.zbus.mq;

import java.util.ArrayList;
import java.util.List;

import io.zbus.kit.FileKit;
import io.zbus.mq.Protocol.ChannelInfo;
import io.zbus.mq.Protocol.MqInfo;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.plugin.MonitorUrlFilter;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.StaticResource;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;

public class MonitorServerAdaptor extends MqServerAdaptor {  
	
	private SubscriptionManager subscriptionManager;
	public MonitorServerAdaptor(MqServerAdaptor mqServerAdaptor) {
		super(mqServerAdaptor);
		
		requestAuth = null;
		if (config.monitorServer != null && config.monitorServer.auth != null) {
			requestAuth = config.monitorServer.auth; 
		}  
		
		this.subscriptionManager = mqServerAdaptor.subscriptionManager;
		
		this.rpcProcessor = new RpcProcessor();
		StaticResource staticResource = new StaticResource();
		staticResource.setCacheEnabled(false); // TODO turn if off in production
		
		rpcProcessor.mount("/", new MonitorService()); 
		rpcProcessor.mount("/static", staticResource, false);
		rpcProcessor.mountDoc(); 
		
		filterList.clear();
		filterList.add(new MonitorUrlFilter(rpcProcessor)); 
	}
 
	
	class MonitorService {
		private FileKit fileKit = new FileKit();  
		
		@Route(path = "/favicon.ico", docEnabled = false)
		public Message favicon() {
			return fileKit.render("static/favicon.ico");
		}
		
		@Route("/")
		public List<MqInfo> home() {  
			List<MqInfo> res = mqManager.mqInfoList();
			for(MqInfo mqInfo : res) {
				for(ChannelInfo channelInfo : mqInfo.channelList) {
					channelInfo.subscriptions = subscriptionManager.getSubscriptionList(mqInfo.name, channelInfo.name);
					if(channelInfo.subscriptions == null) {
						channelInfo.subscriptions = new ArrayList<>();
					}
				} 
			}
			
			return res;
		}  
		 
		public MqInfo info(String mq, String channel) {  
			MessageQueue q = mqManager.get(mq);
			if(q == null) {
				return null;
			}
			MqInfo res = q.info();
			for(ChannelInfo channelInfo : res.channelList) {
				channelInfo.subscriptions = subscriptionManager.getSubscriptionList(mq, channel);
				if(channelInfo.subscriptions == null) {
					channelInfo.subscriptions = new ArrayList<>();
				}
			} 
			return res;  
		}  
	} 
}

