package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.transport.Message;

public class RpcClientTest { 
	
	public static void doTest(RpcClient rpc) throws Exception {    
		//1) 原始的方法调用中的数据格式: {module: xx, method: xx, params: []}
		Map<String, Object> data = new HashMap<>();
		data.put("module", "/"); //可选
		data.put("method", "plus");
		data.put("params", new Object[] {1,2}); //可选，如果无参
		
		Message req = new Message();
		req.setBody(data); 
		Message res = rpc.invoke(req); //同步调用
		System.out.println(res);
		
		//2)纯异步API
		rpc.invoke(req, resp -> { //异步调用
			System.out.println(resp); 
		}); 
		
		//3) 动态代理
		InterfaceExample example = rpc.createProxy("/", InterfaceExample.class);
		int c = example.plus(1, 2);
		System.out.println(c);
		
		
		//4) 基于URL的调用格式 
		req = new Message();
		req.setUrl("/plus/1/2");
		res = rpc.invoke(req); 
		System.out.println("urlbased: " + res);
	}
}
