package io.zbus.proxy.dmz;
 
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;

import io.zbus.mq.Protocol;
import io.zbus.transport.Message;
import io.zbus.transport.http.WebsocketClient;

public class DmzProxy {

	public static void main(String[] args) {
		WebsocketClient notifyClient = new WebsocketClient("ws://localhost:15555");
		
		notifyClient.onText = (msg)->{
			System.out.println(msg);
		};
		
		Message msg = new Message();
		msg.setHeader(Protocol.CMD, Protocol.ON_NOTIFY);
		
		JSONObject data = new JSONObject();
		data.put("port", 15555); 
		data.put("time", System.currentTimeMillis());
		msg.setBody(data);
		
		notifyClient.sendMessage(msg);
		
		notifyClient.connect(); 
		notifyClient.heartbeat(10, TimeUnit.SECONDS, ()->{
			Message hbt = new Message();
			hbt.setHeader(Protocol.CMD, Protocol.PING);
			return hbt;
		}); 
	}
}
