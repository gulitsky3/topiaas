package io.zbus.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import io.zbus.kit.JsonKit;
import io.zbus.transport.Message;

public class SignAuthExample {
	public static void main(String[] args) {
		ApiKeyProvider apiKeyProvider = new XmlApiKeyProvider("rpc/auth.xml");
		RequestAuth auth = new DefaultAuth(apiKeyProvider);
		
		RequestSign sign = new DefaultSign();
		
		Message req = new Message();
		for(int i=0;i<10;i++) {
			req.setHeader("key"+i, new Random().nextInt());
		}
		
		Map<String, Object> f = new HashMap<>();
		for(int i=0;i<10;i++) {
			f.put("key"+i, new Random().nextInt());
		}
		Object[] params = new Object[] {f};
		req.setBody(params);
		
		String apiKey = "2ba912a8-4a8d-49d2-1a22-198fd285cb06";
		String secretKey = "461277322-943d-4b2f-b9b6-3f860d746ffd";
		
		sign.sign(req, apiKey, secretKey);
		
		String wired = JsonKit.toJSONString(req);
		System.out.println(wired);
		Message req2 = JsonKit.parseObject(wired, Message.class);
		AuthResult res = auth.auth(req2);
		
		System.out.println(res.success); 
	}
}
