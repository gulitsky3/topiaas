package io.zbus.rpc;

import java.util.HashMap;

public class Request extends HashMap<String, Object> {   
	private static final long serialVersionUID = -2112706466764692497L;   
	
	public String getMethod() {
		return get("method");
	} 
	public void setMethod(String value) {
		put("method", value);
	} 
	 
	public String getId() { 
		return get("id");
	}
	public void setId(String value) {
		put("id", value);
	} 
	
	public String getModule() {
		return get("module");
	} 
	public void setModule(String value) {
		put("module", value);
	} 
	
	public Object[] getParams() {
		return get("params");
	} 
	public void setParams(Object[] value) {
		put("params", value);
	} 
	
	public String[] getParamTypes() {
		return get("paramTypes");
	} 
	public void setParamTypes(String[] value) {
		put("paramTypes", value);
	}  
	
	public String getApiKey() { 
		return get("apiKey");
	}
	public void setApiKey(String value) {
		put("apiKey", value);
	} 
	
	public String getSignature() { 
		return get("signature");
	}
	public void setSignature(String value) {
		put("signature", value);
	} 
	
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T)super.get(key);
	}
}