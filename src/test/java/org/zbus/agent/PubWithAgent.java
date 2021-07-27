package org.zbus.agent;

import org.remoting.Message;
import org.remoting.ticket.ResultCallback;
import org.zbus.client.Producer;
import org.zbus.client.ha.AgentConfig;
import org.zbus.client.ha.ClientAgent;
import org.zbus.common.MessageMode;

public class PubWithAgent {

	public static void main(String[] args) throws Exception {  
		AgentConfig config = new AgentConfig();
		config.setSeedBroker("127.0.0.1:15555");
		config.setTrackServerList("127.0.0.1:16666;127.0.0.1:16667");
		
		ClientAgent agent = new ClientAgent(config, 3000);
		
		Producer pub = new Producer(agent, "MySub", MessageMode.PubSub); 
		
		final int count = 100;
		for(int i=0;i<count;i++){   
			Message msg = new Message(); 
			msg.setTopic("qhee");
			msg.setBody("hello "+i); 
			
			pub.send(msg, new ResultCallback() { 
				@Override
				public void onCompleted(Message msg) { 
					System.out.println(msg); 
				}
			}); 
		} 
		
	}

}
