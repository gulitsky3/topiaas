package org.zbus;

import org.remoting.Message;
import org.remoting.RemotingClient;
import org.zbus.client.Consumer;
import org.zbus.common.MessageMode;

public class SubWithClient {

	public static void main(String[] args) throws Exception {  
		
		final RemotingClient client = new RemotingClient("127.0.0.1:15555");
		
		final Consumer consumer = new Consumer(client, "MySub", MessageMode.PubSub);   
		consumer.setTopic("qhee,xmee");
		
		int i=1;
		while(true){
			Message msg = consumer.recv(10000); 
			if(msg == null) continue;
			System.out.format("=====================%d====================\n", i++);
			System.out.println(msg);
		}
		
	}

}
