package io.zbus.kit;

import java.util.Map;



public class StrKitExample {  
	
	public static void main(String[] args) {    
		String url = "k=v1&k=v2&a=b";
		
		Map<String, Object> res = StrKit.keyValueWithArray(url, null);
		System.out.println(res);
	}
	
	 
}
