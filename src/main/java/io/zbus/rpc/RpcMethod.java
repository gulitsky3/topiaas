package io.zbus.rpc;

import java.util.ArrayList;
import java.util.List;

import io.zbus.rpc.annotation.RequestMapping;

public class RpcMethod {
	public String urlPath; //if null, use module/method
	public String module;
	public String method; 
	public List<String> paramTypes = new ArrayList<>();
	public List<String> paramNames = new ArrayList<>();
	public String returnType; 
	public boolean authRequired;
	public boolean docEnabled = true;
	public RequestMapping urlAnnotation;
	
	public RpcMethod() {
		
	}
	
	public RpcMethod(RpcMethod m) {
		this.module = m.module;
		this.method = m.method;
		this.paramTypes = new ArrayList<>(m.paramTypes);
		this.paramNames = new ArrayList<>(m.paramNames);
		this.returnType = m.returnType;
		this.authRequired = m.authRequired;
	}
	
	public String key() {
		return String.format("%s:%s", module, method);
	}
}