package io.zbus.proxy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.HttpRequest;
import io.zbus.proxy.http.HttpDecodeFilter;
import io.zbus.proxy.http.HttpProxyHandler;
import io.zbus.proxy.http.ProxyTarget;
import io.zbus.proxy.http.ProxyUrlMatcher;
import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.HttpWsServer; 

public class HttpProxyExample{   
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {   
		Map<String, ProxyTarget> targetTable = new ConcurrentHashMap<>(); 
		ProxyTarget target = new ProxyTarget();
		target.urlPrefix = "/f5query";
		target.remoteHost = "mw.cmft.com";
		target.remotePort = 8080;
		targetTable.put("/f5query", target); 
		
		ProxyUrlMatcher urlMatcher = new ProxyUrlMatcher(targetTable); 
		HttpDecodeFilter filter = new HttpDecodeFilter(urlMatcher);
		
		HttpWsServer server = new HttpWsServer(); 
		server.setDecodeFilter(filter);
		
		HttpProxyHandler proxyHandler = new HttpProxyHandler(urlMatcher);
		
		ServerAdaptor adaptor = new ServerAdaptor() { 
			@Override
			public void onMessage(Object msg, Session sess) throws IOException {   
				if(msg instanceof HttpRequest) {
					proxyHandler.onMessage(msg, sess);
					return;
				}
				
				Message res = new Message();
				res.setStatus(200);
				
				res.setHeader("id", res.getHeader("id")); 
				res.setHeader("content-type", "text/plain; charset=utf8");
				
				res.setBody("中文"+System.currentTimeMillis());
				
				sess.write(res);  
			}  
			
			@Override
			public void sessionToDestroy(Session sess) throws IOException { 
				super.sessionToDestroy(sess);
			}
		};   
		  
		server.start(80, adaptor);   
	}  
}
