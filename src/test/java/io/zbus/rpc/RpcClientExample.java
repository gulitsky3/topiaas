package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.transport.Message;

public class RpcClientExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		RpcClient rpc = new RpcClient("localhost:8080"); 
		rpc.setAuthEnabled(true);
		rpc.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		rpc.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd");

		
		Map<String, Object> data = new HashMap<>();
		//data.put("module", "/");
		data.put("method", "plus");
		data.put("params", new Object[] {1,2});
		
		Message req = new Message();
		req.setBody(data);
		
		Message res = rpc.invoke(req); //同步调用
		System.out.println(res); 
		
		rpc.invoke(req, resp -> { //异步调用
			System.out.println(resp); 
		}); 
		
		InterfaceExample example = rpc.createProxy("/", InterfaceExample.class);
		int c = example.plus(1, 2);
		System.out.println(c);
		
		//rpc.close(); 
	}
}
