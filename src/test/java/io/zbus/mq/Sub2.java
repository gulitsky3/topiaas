package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;

public class Sub2 {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqClient ws = new MqClient("localhost:15555");

		ws.onText = msg -> {
			System.out.println(msg); 
		};

		ws.onOpen = () -> { 
			Map<String, Object> req = new HashMap<>();
			req.put("cmd", "sub"); 
			req.put("topic", "/abcedf"); 
			req.put("channel", "share2");
			 
			ws.sendMessage(req);
		};

		ws.connect(); 
	} 
}
