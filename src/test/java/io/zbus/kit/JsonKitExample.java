package io.zbus.kit;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;



public class JsonKitExample { 
	public static class User extends JSONObject{ 
		private static final long serialVersionUID = -3503854199033444335L;
		private String name;
		private String password;
		public String getName() {
			return name;
		} 
		public void setName(String name) {
			this.name = name;
		}
		public String getPassword() {
			return password;
		} 
		public void setPassword(String password) {
			this.password = password;
		} 
		@Override
		public String toString() {
			return "User [name=" + name + ", password=" + password + "]" ;
		} 
	}
	
	public static void main(String[] args) {    
		JSONObject j = new JSONObject();
		j.put("name", "hong");
		
		User user = JsonKit.convert(j, User.class);
		System.out.println(user); 
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
