package io.zbus.kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StrKit {
	
	public static String uuid(){
		return UUID.randomUUID().toString();
	}
	
	public static boolean isEmpty(String str) {
		return str == null || str.trim().equals(""); 
	}
	
	public static Map<String, String> kvp(String value){
		return StrKit.kvp(value, "&");
	} 
	
	public static String[] split(String value, String delim) {
		if(value == null) return new String[0];
		String[] bb = value.split(delim);
		List<String> strs = new ArrayList<>();
		for(String b : bb) {
			if(b.isEmpty()) continue;
			strs.add(b);
		}
		return strs.toArray(new String[0]);
	}
	
	public static String[] split(String value) {
		return split(value, "[ ,;]");
	}

	public static Map<String, String> kvp(String value, String delim){
		if(isEmpty(delim)) {
			delim = "[ ;]";
		} 
		
		Map<String, String> res = new HashMap<String, String>();
		if(isEmpty(value)) return res;
		
		
		String[] kvs = value.split(delim);
		for(String kv : kvs){
			kv = kv.trim();
			if(kv.equals("")) continue;
			String[] bb = kv.split("=");
			String k="",v="";
			if(bb.length > 0){
				k = bb[0].trim();
			}
			if(bb.length > 1){
				v = bb[1].trim();
			}
			res.put(k, v);
		}
		return res;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> keyValueWithArray(String value, String delim){
		if(isEmpty(delim)) {
			delim = "[&]";
		} 
		
		Map<String, Object> res = new HashMap<String, Object>();
		if(isEmpty(value)) return res;
		
		
		String[] kvs = value.split(delim);
		for(String kv : kvs){
			kv = kv.trim();
			if(kv.equals("")) continue;
			String[] bb = kv.split("=");
			String k = "";
			Object v = null;
			if(bb.length > 0){
				k = bb[0].trim();
			}
			if(bb.length > 1){
				v = bb[1].trim();
			}
			Object exitsValue = res.get(k);
			if(exitsValue != null) {
				if(exitsValue instanceof List) {
					List<Object> list = (List<Object>)exitsValue;
					list.add(v);
					v = list;
				} else {
					List<Object> list = new ArrayList<>();
					list.add(exitsValue);
					list.add(v);
					v = list;
				}
			}
			res.put(k, v);
		}
		return res;
	}
	
	
	public static List<String> getArrayValue(String value, String key){
		String delim = "[&]";  
		List<String> valueArray = new ArrayList<>(); 
		if(value == null) return valueArray;
		
		String[] kvs = value.split(delim);
		for(String kv : kvs){
			kv = kv.trim();
			if(kv.equals("")) continue;
			String[] bb = kv.split("=");
			String k="",v="";
			if(bb.length > 0){
				k = bb[0].trim();
			}
			if(bb.length > 1){
				v = bb[1].trim();
			}
			if(!k.equals(key)) continue;
			valueArray.add(v);
		}
		return valueArray;
	}
	
	public static Integer getInt(Map<String, String> map, String key) {
		String value = map.get(key);
		if(value == null) return null;
		return Integer.valueOf(value);
	}
	
	public static Long getLong(Map<String, String> map, String key) {
		String value = map.get(key);
		if(value == null) return null;
		return Long.valueOf(value);
	}
	
	public static Double getDouble(Map<String, String> map, String key) {
		String value = map.get(key);
		if(value == null) return null;
		return Double.valueOf(value);
	}

}
