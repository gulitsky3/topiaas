package io.zbus.mq.plugin;

import io.zbus.mq.MqManager;
import io.zbus.mq.MqServerAdaptor;
import io.zbus.mq.MqServerConfig;
import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.StaticResource;
import io.zbus.transport.Message;

public class UrlRouteFilter implements Filter { 
	private MqManager mqManager; 
	private MqServerAdaptor mqServerAdaptor; 
	private StaticResource staticResource = new StaticResource(); 
	
	@Override
	public void init(MqServerAdaptor mqServerAdaptor) {
		MqServerConfig config = mqServerAdaptor.getConfig();
		this.mqServerAdaptor = mqServerAdaptor;
		this.mqManager = mqServerAdaptor.getMqManager();  
		
		staticResource.setBasePath(config.getStaticFileDir());
		staticResource.setCacheEnabled(config.isStaticFileCacheEnabled());
	}
	 
	private String match(String url) {
		if(mqManager == null) return null;
		
		int length = 0; 
		String matched = null;
		for(String mq : mqManager.mqNames()) { 
			if(url.startsWith(mq)) {
				if(mq.length() > length) {
					length = mq.length();
					matched = mq; 
				}
			}
		}  
		return matched;
	}
	
	@Override
	public boolean doFilter(Message req, Message res) { 
		String cmd = req.getHeader(Protocol.CMD);
		if(cmd != null) return true; //cmd 
		String url = req.getUrl();
		if(url == null) return true;      
		
		String mq = match(url); 
		if(mq != null) {
			req.setHeader(Protocol.MQ, mq);
			//Assumed to be RPC
			if(req.getHeader(Protocol.CMD) == null) { // RPC assumed
				req.setHeader(Protocol.CMD, Protocol.PUB);
				req.setHeader(Protocol.ACK, false); //ACK should be disabled
			}  
			
			//TODO check if consumer exists, reply 502, no service available 
			return true;
		} 
		
		RpcProcessor rpcProcessor = mqServerAdaptor.getRpcProcessor();  
		if(rpcProcessor != null) {
			rpcProcessor.process(req, res); 
			return false;
		}  
		
		//last resolve to static resource
		Message data = staticResource.file(req);
		res.replace(data); 
		return false; 
	} 
}
