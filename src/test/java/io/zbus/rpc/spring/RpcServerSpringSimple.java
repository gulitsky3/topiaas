package io.zbus.rpc.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcServerSpringSimple {
 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		new ClassPathXmlApplicationContext("rpc/spring-server-simple.xml");      
	}

}
