package io.zbus.net.ws;

import java.util.HashMap;
import java.util.Map;

import io.zbus.transport.Message;
import io.zbus.transport.http.WebsocketClient;

public class WebSocketExample {
 
	public static void main(String[] args) throws Exception {  
		
		WebsocketClient ws = new WebsocketClient("localhost:15555"); 
		ws.onText = data -> {
			 System.out.println(data);
			 ws.close();
		};   
		
		ws.onOpen(()->{
			Map<String, Object> command = new HashMap<>();
			command.put("module", "example");
			command.put("method", "echo");
			command.put("params", new Object[] {"hong"});
			
			Message message = new Message();
			message.setBody(command);
			
			//for MQ
			message.addHeader("cmd", "pub");
			message.addHeader("mq", "MyRpc");
			message.addHeader("ack", false); 
			
			ws.sendMessage(message);
			
		});
		
		ws.connect();  
	}
}
