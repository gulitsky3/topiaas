package io.zbus.rpc;

public class MqRpcClientExample { 
	
	public static void main(String[] args) throws Exception {  
		RpcClient rpc = new RpcClient("localhost", "MyRpc");   
		
		rpc.setAuthEnabled(true);
		rpc.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		rpc.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd");
		

		RpcClientTest.doTest(rpc);
		
		rpc.close();
	}
}
