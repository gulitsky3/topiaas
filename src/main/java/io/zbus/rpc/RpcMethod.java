package io.zbus.rpc;

import java.util.ArrayList;
import java.util.List;

import io.zbus.kit.HttpKit;
import io.zbus.rpc.annotation.Route;

public class RpcMethod {
	public String urlPath;  // java method's url path 
	public String method;   // java method 
	public List<MethodParam> params = new ArrayList<>(); //param list of (type,name)
	public String returnType; 
	public boolean authRequired;
	public boolean docEnabled = true;
	public Route urlAnnotation;
	
	public static class MethodParam {
		public String type;
		public String name; 
	} 
	
	public void addParam(String type, String name) {
		MethodParam p = new MethodParam();
		p.name = name;
		p.type = type;
		params.add(p);
	}
	
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	} 
	
	public void setReturnType(Class<?> returnType) {
		this.returnType = returnType.getName();
	} 
	
	public void addParam(Class<?> type, String name) {
		addParam(type.getName(), name);
	}
	
	public void addParam(String type) {
		addParam(type, null);
	}
	
	public void addParam(Class<?> type) {
		addParam(type, null);
	}
	
	public RpcMethod() {
		
	}
	
	public RpcMethod(RpcMethod m) { 
		this.method = m.method;
		this.params = new ArrayList<>(m.params);  
		this.returnType = m.returnType;
		this.authRequired = m.authRequired;
	} 
	
	public String getUrlPath() {
		if(urlPath == null) return HttpKit.joinPath(method);
		return urlPath;
	} 
	
	public void setUrlPath(String module, String method) {
		this.urlPath = HttpKit.joinPath(module, method);
	}  
}