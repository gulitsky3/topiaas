package io.zbus.mq.plugin;

import io.zbus.kit.FileKit;
import io.zbus.mq.MqManager;
import io.zbus.mq.MqServerAdaptor;
import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.Message;
import io.zbus.transport.Session;
import io.zbus.transport.http.Http;

public class PublicUrlRouter implements UrlRouter {
	private boolean urlMatchLocalFirst = false; 
	private MqManager mqManager;
	private FileKit fileKit; 
	private MqServerAdaptor mqServerAdaptor; 
	
	@Override
	public void init(MqServerAdaptor mqServerAdaptor) {
		this.mqServerAdaptor = mqServerAdaptor;
		this.mqManager = mqServerAdaptor.getMqManager(); 
		this.fileKit = new FileKit(mqServerAdaptor.getConfig().fileCacheEnabled);
		this.urlMatchLocalFirst = mqServerAdaptor.getConfig().urlMatchLocalFirst; 
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
	public boolean route(Message req, Session sess) { 
		String url = req.getUrl();
		if(url == null) return false;   
		RpcProcessor rpcProcessor = mqServerAdaptor.getRpcProcessor();
		
		if(urlMatchLocalFirst) {
			if(rpcProcessor != null) {
				if(rpcProcessor.matchUrl(url)) {
					Message res = new Message();
					res.setHeader("Access-Control-Allow-Origin", "*");
					res.setHeader("Access-Control-Allow-Headers", "X-Requested-With,content-type");
					res.setHeader("Access-Control-Allow-Methods", "PUT,POST,GET,DELETE,OPTIONS"); 
					rpcProcessor.process(req, res);
					sess.write(res); 
					return true;
				} 
			} 
		}
		
		String mq = match(url); 
		if(mq != null) {
			req.setHeader(Protocol.MQ, mq);
			//Assumed to be RPC
			if(req.getHeader(Protocol.CMD) == null) { // RPC assumed
				req.setHeader(Protocol.CMD, Protocol.PUB);
				req.setHeader(Protocol.ACK, false); //ACK should be disabled
			}  
			
			//TODO check if consumer exists, reply 502, no service available 
			return false;
		} 
		
		if(!urlMatchLocalFirst) {
			if(rpcProcessor != null) {
				if(rpcProcessor.matchUrl(url)) {
					Message res = new Message();
					//TODO
					res.setHeader("Access-Control-Allow-Origin", "*");
					res.setHeader("Access-Control-Allow-Headers", "X-Requested-With,content-type");
					res.setHeader("Access-Control-Allow-Methods", "PUT,POST,GET,DELETE,OPTIONS"); 
					rpcProcessor.process(req, res);
					sess.write(res); 
					return true;
				} 
			} 
		} 
		
		Message res = null;
		if("/".equals(url)) { 
			res = fileKit.loadResource("static/index.html"); 
			res.setHeader("Access-Control-Allow-Origin", "*");
			res.setHeader("Access-Control-Allow-Headers", "X-Requested-With,content-type");
			res.setHeader("Access-Control-Allow-Methods", "PUT,POST,GET,DELETE,OPTIONS"); 
			
			if(res.getStatus() != 200) {
				res = new Message();
				res.setHeader("Access-Control-Allow-Origin", "*");
				res.setHeader("Access-Control-Allow-Headers", "X-Requested-With,content-type");
				res.setHeader("Access-Control-Allow-Methods", "PUT,POST,GET,DELETE,OPTIONS"); 
				res.setStatus(200);
				res.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
				res.setBody("<h1> Welcome to zbus</h1>"); 
			} 
			 
		} else {
			res = new Message();
			res.setHeader("Access-Control-Allow-Origin", "*");
			res.setHeader("Access-Control-Allow-Headers", "X-Requested-With,content-type");
			res.setHeader("Access-Control-Allow-Methods", "PUT,POST,GET,DELETE,OPTIONS"); 
			res.setStatus(404);
			res.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
			res.setBody(String.format("URL=%s Not Found", url));
		}
		sess.write(res);
		return true; 
	} 
}
