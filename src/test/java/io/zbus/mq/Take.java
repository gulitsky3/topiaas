package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;

public class Take {  
	
	public static MqClient buildInproClient() {
		MqServer server = new MqServer(new MqServerConfig());
		return new MqClient(server);
	}
	
	public static void main(String[] args) throws Exception { 
		MqClient client = new MqClient("localhost:15555");
		//MqClient client = buildInproClient();
		
		final String mq = "MyMQ", channel = "MyChannel";
		
		Map<String, Object> req = new HashMap<>();
		req.put("cmd", "take");  
		req.put("mq", mq); 
		req.put("channel", channel); 
		
		client.invoke(req, res->{
			System.out.println(res);
			
			client.close();
		}, e->{
			e.printStackTrace();
			try { client.close(); } catch (Exception ex) { }
		});  
	} 
}
