package io.zbus.kit;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON; 
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JsonKitExample {
	public static void main(String[] args) {
		String json = "select:[display_name,id],where:{id:1}";
		json = JsonKit.fixJson(json);
		System.out.println(json);
		
		Map<String, Object> data = new HashMap<>();
		data.put("a", null);
		data.put("b", "balue");
		
		json = JsonKit.toJSONString(data);
		json = JSON.toJSONString(data, SerializerFeature.WriteMapNullValue);
		System.out.println(json);
	}
}
