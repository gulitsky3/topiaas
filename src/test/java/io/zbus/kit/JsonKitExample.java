package io.zbus.kit;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

public class JsonKitExample { 
	
	public static void main(String[] args) {  
		Map<String, Object> data = new HashMap<>();
		data.put("a", null);
		data.put("c", "ddd");
		data.put("b", "balue");
		
		String json = JsonKit.toJSONString(data); 
		System.out.println(json); 
		System.out.println(data);
		
		JSONObject jdata = JsonKit.parseObject(json, JSONObject.class);
		System.out.println(jdata.keySet());
	}
	
	public static void fixJson() {
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
