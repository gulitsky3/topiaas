package org.zbus.rpc.simple;

import org.zbus.client.rpc.Rpc;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;

public class KcxpRpc {

	public static void main(String[] args) throws Exception {  
		final RemotingClient client = new RemotingClient("127.0.0.1", 15555);
		
		Rpc rpc = new Rpc(client, "KCXP");  
		
		for(int i=0;i<10;i++){
			Message req = new Message(); 
			req.setBody("hello from client "+i); 
			
			Message reply = rpc.invokeSync(req, 10000); 
			System.out.println(reply);
		}
		
		System.out.println("--done--");
	}
}