package io.zbus.net.ws;

import io.zbus.transport.Message;
import io.zbus.transport.http.WebsocketClient;

public class WebSocketExample {
 
public static void main(String[] args) throws Exception {  
		
		WebsocketClient ws = new WebsocketClient("ws://zbus.io"); 
		ws.onText = data -> {
			 System.out.println(data);
			 ws.close();
		};   
		
		ws.onOpen(()->{ 
			
			Message message = new Message();  
			message.setHeader("cmd", "pub");
			message.setHeader("mq", "MyMQ");  
			message.setBody("test from websocket");
			
			ws.sendMessage(message);
			
		});
		
		ws.connect();  
	}
}
