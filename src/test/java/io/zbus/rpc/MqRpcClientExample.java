package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

public class MqRpcClientExample {

	public static void main(String[] args) throws Exception {  
		RpcClient rpc = new RpcClient("localhost:15555");  
		rpc.setMq("MyRpc"); //MQ required
		
		rpc.setAuthEnabled(true);
		rpc.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		rpc.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd");
		

		Map<String, Object> req = new HashMap<>();
		req.put("module", "example");
		req.put("method", "getOrder"); 
		
		Map<String, Object> res = rpc.invoke(req); //同步调用
		System.out.println(res);
		
		rpc.invoke(req, resp -> { //异步调用
			System.out.println(resp); 
		}); 
		 
		 
		rpc.close(); 
	}
}
