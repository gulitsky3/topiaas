package io.zbus.kit;

import java.util.HashMap;
import java.util.Map;



public class JsonKitExample { 
	public static class User{  
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
		User user = new User();
		user.setName("hong");
		
		@SuppressWarnings("unchecked")
		Map<String, Object> map = JsonKit.convert(user, Map.class); 
		System.out.println(map); 
		
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
