package io.zbus.rpc;

public class RpcClientExample { 
	
	public static void main(String[] args) throws Exception {  
		RpcClient rpc = new RpcClient("localhost:15555");   
		
		
		
		rpc.close();
	}
}
