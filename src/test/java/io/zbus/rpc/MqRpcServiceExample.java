package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExampleImpl;
import io.zbus.rpc.mq.MqRpcService;

public class MqRpcServiceExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		RpcProcessor processor = new RpcProcessor();
		processor.addModule("example", new InterfaceExampleImpl());
		MqRpcService service = new MqRpcService(processor);
		service.setMq("MyRpc");
		service.setAddress("localhost:15555");
		
		service.start();
	} 
}
