package io.zbus.rpc;

import java.util.Arrays;
import java.util.Map;


public class RpcServerExample_DynamicMethod {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		RpcServer server = new RpcServer();  
		
		GenericService service = new GenericService();
		
		//抽象的服务调用，增加一个具体的方法
		RpcMethod spec = new RpcMethod();
		spec.module = "/";
		spec.method = "func1";
		spec.paramTypes = Arrays.asList(String.class.getName(), Integer.class.getName());
		spec.paramNames = Arrays.asList("name", "age");
		spec.returnType = Map.class.getName();
		server.addMethod(spec, service); 
			
		server.setPort(80);
		server.start();
	}
}
