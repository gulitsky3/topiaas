package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.transport.Message;

public class RpcClientSimpleExample { 
	
	public static void main(String[] args) throws Exception {  
		RpcClient rpc = new RpcClient("localhost:15555");   

		Message req = new Message();
		req.setUrl("/map");
		Map<String, Object> map = new HashMap<>();
		map.put("key", "value");
		map.put("nullkey", null);
		req.setBody(new Object[] {map}); //body as parameter array
		
		Message res = rpc.invoke(req); //同步调用
		System.out.println(res);
		
		rpc.close();
	}
}
