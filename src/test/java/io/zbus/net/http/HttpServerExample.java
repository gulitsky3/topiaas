package io.zbus.net.http;

import io.zbus.kit.JsonKit;
import io.zbus.transport.Message;
import io.zbus.transport.Server;
import io.zbus.transport.http.Http;
import io.zbus.transport.http.HttpWsServer;
import io.zbus.transport.http.HttpWsServerAdaptor;

public class HttpServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		HttpWsServerAdaptor adaptor = new HttpWsServerAdaptor();
		
		adaptor.url("/", (msg, sess) -> {   
			System.out.println(JsonKit.toJSONString(msg));
			Message res = new Message();
			res.setStatus(200);
			
			res.addHeader(Message.ID, res.getHeader(Message.ID)); 
			res.addHeader(Http.CONTENT_TYPE, "text/plain; charset=utf8");
			res.setBody("中文"+System.currentTimeMillis());
			sess.write(res); 
		});   
		 
		Server server = new HttpWsServer();   
		server.start(80, adaptor);  
	} 
}
