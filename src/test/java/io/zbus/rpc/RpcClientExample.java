package io.zbus.rpc;

import io.zbus.transport.Message;

public class RpcClientExample { 
	
	public static void main(String[] args) throws Exception {  
		for(int i=0;i<100000;i++) {
			RpcClient rpc = new RpcClient("localhost:15555");   
			
			Message req = new Message();
			req.setUrl("/plus");
			req.setBody(new Object[] {1,2}); //body as parameter array
			
			Message res = rpc.invoke(req); //同步调用
			System.out.println(res);
			
			rpc.close();
			System.out.println(">>>>>" + i);
		}
	}
}
