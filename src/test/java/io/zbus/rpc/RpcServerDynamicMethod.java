package io.zbus.rpc;

import java.util.Arrays;
import java.util.Map;

import io.zbus.mq.MqServer;


public class RpcServerDynamicMethod {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		GenericService service = new GenericService();
		
		//抽象的服务调用，增加一个具体的方法
		RpcMethod spec = new RpcMethod(); 
		spec.method = "func1";
		spec.paramTypes = Arrays.asList(String.class.getName(), Integer.class.getName());
		spec.paramNames = Arrays.asList("name", "age");
		spec.returnType = Map.class.getName();
		
		RpcProcessor p = new RpcProcessor();
		p.mount(spec, service);  
	
		MqServer server = new MqServer(15555);
		server.setRpcProcessor(p);
		server.start();  
	}
}
