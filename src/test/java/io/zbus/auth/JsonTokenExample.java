package io.zbus.auth;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;

public class JsonTokenExample {
	public static void main(String[] args) throws Exception {
		JSONObject payload = new JSONObject();
		payload.put("sub", "1234567890");
		payload.put("name", "hong");  
		
		String token = JsonToken.builder()
			.setPayload(payload)
			.setIssuedAt(System.currentTimeMillis())
			.setExpiration(System.currentTimeMillis()+10000)
			.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd")
			.build();
		
		System.out.println(token);
		
		Map<String, Object> claims = JsonToken.parser()
			.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd")
			.parse(token);
		System.out.println(claims);
	} 
}
