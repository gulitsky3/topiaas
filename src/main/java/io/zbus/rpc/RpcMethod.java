package io.zbus.rpc;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

import io.zbus.kit.HttpKit;
import io.zbus.rpc.annotation.Route;

public class RpcMethod {
	public String urlPath;  // java method's url path 
	public String method;   // java method 
	public List<MethodParam> params = new ArrayList<>(); //param list of (type,name)
	public String returnType;
	public String genericReturnType;
	@JSONField(serialize = false)
	public boolean docEnabled = true;
	@JSONField(serialize = false)
	public boolean enabled = true;
	@JSONField(serialize = false)
	public boolean ignoreResult = false;
	@JSONField(serialize = false)
	public Route urlAnnotation;
	@JSONField(serialize = false)
	public List<RpcFilter> filters = new ArrayList<>();
	
	public static class MethodParam {
		public Class<?> type;
		public String typeName;
		public String genericTypeName;
		public String name;  
		public boolean fromContext;
	} 
	
	public void addParam(Class<?> type, String name) {
		addParam(type, name, type.getName(), type.getName());
	}

	public void addParam(Class<?> typeClass, String name, String typeName, String genericTypeName) {
		MethodParam p = new MethodParam();
		p.name = name;
		p.type = typeClass;
		p.typeName = typeName;
		p.genericTypeName = genericTypeName;
		params.add(p);
	}
	
	public void addParam(Class<?> type) {
		addParam(type, null);
	}
	
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	} 
	
	public void setReturnType(Class<?> returnType) {
		this.returnType = returnType.getName();
	} 

	public void setGenericReturnType(String genericReturnType) {
		this.genericReturnType = genericReturnType;
	}
	public void setGenericReturnType(Type genericReturnType) {
		this.setGenericReturnType(genericReturnType.getTypeName());
	}

	public RpcMethod() {
		
	}
	
	public RpcMethod(RpcMethod m) { 
		this.method = m.method;
		this.params = new ArrayList<>(m.params);  
		this.returnType = m.returnType; 
	} 
	
	public String getUrlPath() {
		if(urlPath == null) return HttpKit.joinPath(method);
		return urlPath;
	} 
	
	public void setUrlPath(String module, String method) {
		this.urlPath = HttpKit.joinPath(module, method);
	}  
	
}