package io.zbus.examples.rpc.http;

import io.zbus.rpc.bootstrap.HttpServiceBootstrap;
 
public class RpcService {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		HttpServiceBootstrap b = new HttpServiceBootstrap();  
		
		b.port(15555)          // start server inside 
		 .autoDiscover(true)   // disable if add modules manually 
		 //.ssl("ssl/zbus.crt", "ssl/zbus.key") //Enable SSL
		 //.serviceToken("myrpc_service") //Enable Token authentication
		 .start();
	}
}