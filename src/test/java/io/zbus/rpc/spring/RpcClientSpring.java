package io.zbus.rpc.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import io.zbus.rpc.biz.InterfaceExample;

public class RpcClientSpring { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		
		ClassPathXmlApplicationContext ioc = new ClassPathXmlApplicationContext("rpc/spring-client.xml");      
		InterfaceExample example = ioc.getBean(InterfaceExample.class);
		int c = example.plus(1, 2);
		System.out.println(c);
		
	}
}
