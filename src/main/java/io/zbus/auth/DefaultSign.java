package io.zbus.auth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import io.zbus.kit.CryptoKit;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.transport.Message;

public class DefaultSign implements RequestSign {   
	
	public static JSONObject jsonToSign(Message request) {
		JSONObject json = new JSONObject();
    	JSONObject headers = new JSONObject(); 
		json.put("headers", headers);   
		
		String fields = request.getHeader(SIGN_FIELDS);
    	if(fields == null) fields = DEFAULT_SIGN_FIELDS;
    	else headers.put(SIGN_FIELDS, fields);
    	String[] fieldList = StrKit.split(fields, ","); 
    	
    	for(String f : fieldList) {
    		if(f.equalsIgnoreCase("url")) {
    			String url = request.getUrl();
    			if(url != null) {
    				json.put("url", url);
    			}
    		} else if(f.equalsIgnoreCase("method")) {
    			String method = request.getMethod();
    			if(method != null) {
    				json.put("method", method);
    			}
    		} else if(f.equalsIgnoreCase("body")) {
    			Object body = request.getBody();
    			if(body != null) {
    				if(body instanceof String) { //json body 
			    		try {
			    			body = JsonKit.parse((String)body);
			    		} catch (Exception e) {
							//just ignore
			    			e.printStackTrace();
						} 
    				}
    				json.put("body", body);
    			} 
    		} else if(f.startsWith("h.")) {
    			String key = f.substring(2); 
    			if(key.equals("*")) {
    				headers.putAll(request.getHeaders());
    			} else {
	    			String val = request.getHeader(key);
	    			if(val != null) {
	    				headers.put(key, val);
	    			} 
    			}
    		}
    	}
    	return json;
	}
	
	public static JSONObject jsonToVerifySign(Message request) {  
    	JSONObject json = jsonToSign(request);
    	JSONObject headers = json.getJSONObject("headers");
    	String apiKey = request.getHeader(APIKEY);
    	String signature = request.getHeader(SIGNATURE);
    	
    	if(apiKey != null) {
    		headers.put(APIKEY, apiKey);
    	}
    	
    	if(signature != null) {
    		headers.put(SIGNATURE, signature);
    	} 
    	return json;
	}
	
	public static String calcSignature(JSONObject json, String apiKey, String secret) {  
		JSONObject headers = json.getJSONObject("headers"); 
		headers.put(APIKEY, apiKey);
		headers.remove(SIGNATURE);
		
    	String message = JSON.toJSONString(json, SerializerFeature.MapSortField); //Sort map by key 
		String sign = CryptoKit.hmacSign(message, secret); 
		return sign;
    }
	
	public String calcSignature(Message request, String apiKey, String secret) {   
    	JSONObject json = jsonToSign(request); 
    	return calcSignature(json, apiKey, secret); 
    } 
	
	public void sign(Message request, String apiKey, String secret) { 
		String sign = calcSignature(request, apiKey, secret);
		request.setHeader(APIKEY, apiKey); 
		request.setHeader(SIGNATURE, sign);
    }   
}
