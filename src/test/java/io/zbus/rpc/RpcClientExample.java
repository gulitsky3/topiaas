package io.zbus.rpc;

import io.zbus.transport.Message;

public class RpcClientExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		RpcClient rpc = new RpcClient("localhost:8080"); 
		rpc.setAuthEnabled(true);
		rpc.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		rpc.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd");

		Message req = new Message();
		req.setUrl("/test");  
		
		for(int i=0;i<10;i++) {
			Message res = rpc.invoke(req); //同步调用
			System.out.println(res);
		}
		
		rpc.invoke(req, resp -> { //异步调用
			System.out.println(resp); 
		}); 
		

		 
		//rpc.close(); 
	}
}
