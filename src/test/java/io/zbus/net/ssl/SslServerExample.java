package io.zbus.net.ssl;

import io.netty.handler.ssl.SslContext;
import io.zbus.net.EventLoop;
import io.zbus.net.Ssl;
import io.zbus.net.http.HttpMsgAdaptor;
import io.zbus.net.http.HttpServer; 

public class SslServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		SslContext context = Ssl.buildServerSsl("ssl/zbus.crt", "ssl/zbus.key");
		
		EventLoop loop = new EventLoop(); 
		loop.setSslContext(context);
		
		HttpServer server = new HttpServer(loop);  
		server.start(15555, new HttpMsgAdaptor());  
	} 
}
