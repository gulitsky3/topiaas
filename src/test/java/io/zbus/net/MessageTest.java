package io.zbus.net;

import io.zbus.kit.JsonKit;
import io.zbus.transport.Message;

public class MessageTest { 
	
	public static void main(String[] args) {
		Message message = new Message();
		message.setUrl("/a/b/");
		 
		
		message.setParam("k", new String[] {"ab", "cd"});
		message.setParam("13423242");
		message.setCookie("key", "value");
		message.setCookie("key2", "value2");  
		
		
		String json = JsonKit.toJSONString(message,true);
		System.out.println(json); 
		
		Message m2 = JsonKit.parseObject(json, Message.class);
		
		System.out.println(JsonKit.toJSONString(m2, true));
	}
}
