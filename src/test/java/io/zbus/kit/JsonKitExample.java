package io.zbus.kit;

import java.util.HashMap;
import java.util.Map;

public class JsonKitExample {
	public static void main(String[] args) {
		String json = "select:[display_name,id],where:{id:1}";
		json = JsonKit.fixJson(json);
		System.out.println(json);
		
		Map<String, Object> data = new HashMap<>();
		data.put("a", null);
		data.put("c", "ddd");
		data.put("b", "balue");
		
		json = JsonKit.toJSONString(data); 
		System.out.println(json);
	}
}
