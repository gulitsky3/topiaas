package io.zbus.rpc.spring;

import io.zbus.rpc.Spring;

public class RpcServiceSpring {  
	public static void main(String[] args) throws Exception {  
		Spring.run("rpc/spring-server-local.xml");      
	} 
}
