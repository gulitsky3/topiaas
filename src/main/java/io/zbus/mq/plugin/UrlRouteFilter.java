package io.zbus.mq.plugin;

import io.zbus.kit.FileKit;
import io.zbus.mq.MqManager;
import io.zbus.mq.MqServerAdaptor;
import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;

public class UrlRouteFilter implements Filter { 
	private MqManager mqManager;
	private FileKit fileKit; 
	private MqServerAdaptor mqServerAdaptor; 
	
	@Override
	public void init(MqServerAdaptor mqServerAdaptor) {
		this.mqServerAdaptor = mqServerAdaptor;
		this.mqManager = mqServerAdaptor.getMqManager(); 
		this.fileKit = new FileKit(mqServerAdaptor.getConfig().isStaticFileCacheEnabled()); 
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
		 
		if("/".equals(url)) {  
			fileKit.render(res, "static/index.html");   
		} else { 
			res.setStatus(404);
			res.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
			res.setBody(String.format("URL=%s Not Found", url)); 
		} 
		return false; 
	} 
}
