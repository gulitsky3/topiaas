package io.zbus.rpc;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsonKit;
import io.zbus.rpc.annotation.Auth;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Path;
import io.zbus.rpc.doc.DocRender;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;

public class RpcProcessor {
	private static final Logger log = LoggerFactory.getLogger(RpcProcessor.class);  
	protected Map<String, MethodInstance> methodTable = new HashMap<>();      //path => MethodInstance 
	
	protected String docUrlRoot = "/";
	protected boolean stackTraceEnabled = true;
	protected boolean methodPageEnabled = true; 
	protected boolean methodPageAuthEnabled = false;
	protected String methodPageModule = "index";
	
	protected RpcFilter beforeFilter;
	protected RpcFilter afterFilter;
	protected RpcFilter authFilter; 
	
	public void enableMethodPageModule() { 
		DocRender render = new DocRender(this, docUrlRoot);
		addModule(methodPageModule, render, false, methodPageAuthEnabled);
	}   
	 
	public void addModule(String module, Object service) {
		addModule(module, service, true);
	}
	
	public void addModule(String module, Object service, boolean enableDoc) {
		addModule(module, service, enableDoc, true);
	} 
	
	public void addModule(String module, Object service, boolean defaultAuth, boolean enableDoc) { 
		try {
			Method[] methods = service.getClass().getMethods();
			boolean classAuthEnabled = defaultAuth;
			Auth classAuth = service.getClass().getAnnotation(Auth.class);
			if(classAuth != null) {
				classAuthEnabled = !classAuth.exclude();
			}
			
			for (Method m : methods) {
				if (m.getDeclaringClass() == Object.class) continue; 
				
				String methodName =  m.getName();
				String path = path(module, methodName);
				
				Path p = m.getAnnotation(Path.class);
				if (p != null) {
					if (p.exclude()) continue; 
					path = p.value(); 
				} 
				
				Auth auth = m.getAnnotation(Auth.class);
				boolean authRequired = classAuthEnabled;
				if(auth != null) {
					authRequired = !auth.exclude();
				}

				m.setAccessible(true);
				MethodInstance mi = new MethodInstance(m, service);
				mi.path = path;
				mi.authRequired = authRequired;
				mi.enableDoc = enableDoc;
				
				List<String> paramTypes = new ArrayList<String>();
				for (Class<?> t : m.getParameterTypes()) {
					paramTypes.add(t.getCanonicalName());
				}
				
				Annotation[][] paramAnnos = m.getParameterAnnotations(); 
				int size = paramTypes.size(); 
				for(int i=0; i<size; i++) {
					Annotation[] annos = paramAnnos[i];
					for(Annotation annotation : annos) {
						if(Param.class.isAssignableFrom(annotation.getClass())) {
							Param param = (Param)annotation;  
							mi.paramNames.add(param.value()); 
							break;
						}
					} 
				}  
				
				if (this.methodTable.containsKey(path)) {
					log.warn(path + " overrided");
				}
				this.methodTable.put(path, mi); 
			}
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}
	} 
	
	public void addMethod(RpcMethod spec, MethodInvoker service) {
		MethodInstance mi = new MethodInstance(spec.method, service);
		mi.paramNames = spec.paramNames;
		
		String path = path(spec.module, spec.method);
		this.methodTable.put(path, mi); 
	}
	
	public void removeModule(String module, Object service) {
		try {
			Method[] methods = service.getClass().getMethods();
			for (Method m : methods) {
				String path = path(module, m.getName());
				Path cmd = m.getAnnotation(Path.class);
				if (cmd != null) {
					path = cmd.value();
					if (cmd.exclude()) continue; 
				} 
				this.removeMethod(path);
			}
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}
	}  
	
	public void removeMethod(String path) { 
		this.methodTable.remove(path);  
	} 
	
	private MethodInstance matchMethod(String url) {  
		int length = 0;
		MethodInstance mi = null;
		for(Entry<String, MethodInstance> e : methodTable.entrySet()) {
			String key = e.getKey();
			if(url.startsWith(key)) {
				if(key.length() > length) {
					length = key.length();
					mi = e.getValue();
				}
			}
		}
		return mi;
	} 
	
	private String path(String module, String method) {
		if(!module.startsWith("/")) module = "/"+module;
		if(!module.endsWith("/")) module += "/";
		return module + method;
	}
	
	public Message process(Message req) {  
		Message response = new Message();   
		try {  
			if (req == null) {
				req = new Message();  
			}   
			
			if(beforeFilter != null) {
				boolean next = beforeFilter.doFilter(req, response);
				if(!next) return response;
			} 
			
			invoke(req, response);
			
			if(afterFilter != null) {
				afterFilter.doFilter(req, response);
			}
			 
		} catch (Throwable e) {
			response.setBody(new RpcException(e.getMessage(), e.getCause(), false, stackTraceEnabled)); 
			response.setStatus(500);
		} finally {
			bindRequestResponse(req, response); 
			if(response.getStatus() == null) {
				response.setStatus(200);
			}
		} 
		return response;
	}
	
	private void bindRequestResponse(Message request, Message response) {
		response.addHeader(Protocol.ID, request.getHeader(Protocol.ID)); //Id Match
	} 
	 
	
	@SuppressWarnings("unchecked")
	private void invoke(Message req, Message response) throws IllegalAccessException, IllegalArgumentException {   
		try {    
			String url = req.getUrl(); 
			if(url == null) url = "/"; 
			Object body = req.getBody(); //assumed to be params
			Object[] params = null;
			if(body != null && body instanceof Object[]) {
				params = (Object[])body;
			}
			
			MethodInstance target = matchMethod(url);   
			
			if(target == null) {
				response.setStatus(404);
				response.addHeader(Http.CONTENT_TYPE, "text/plain; charset=utf8");
				response.setBody(String.format("%s Not Found", url));
				return;
			}
			
			if(authFilter != null && target.authRequired) { 
				boolean next = authFilter.doFilter(req, response);
				if(!next) return;
			} 
			
			if(params == null) {
				String url2 = url.substring(target.path.length());
				UrlInfo info = HttpKit.parseUrl(url2);
				List<Object> paramList = new ArrayList<>(info.path); 
				if(!info.params.isEmpty()) {
					paramList.add(info.params);
				}
				params = paramList.toArray();
			}
			
			Object data = null;
			if(target.reflectedMethod != null) {
				Class<?>[] targetParamTypes = target.reflectedMethod.getParameterTypes();
				Object[] invokeParams = new Object[targetParamTypes.length]; 
				for (int i = 0; i < targetParamTypes.length; i++) { 
					Class<?> paramType = targetParamTypes[i];
					if(Message.class.isAssignableFrom(paramType)) {
						invokeParams[i] = req;
						continue;
					}
					if(i>=params.length) {
						invokeParams[i] = null;
					} else {
						invokeParams[i] = JsonKit.convert(params[i], targetParamTypes[i]);  
					}
				}
				data = target.reflectedMethod.invoke(target.instance, invokeParams);
				
			} else if(target.target != null) {
				Map<String, Object> mapParams = new HashMap<>();  
				if(params != null) {
					if(params.length == 1 && params[0] instanceof Map) {
						mapParams = (Map<String, Object>)params[0]; 
					} else {
						for(int i=0;i <params.length; i++) {
							if(target.paramNames == null) break;
							if(i<target.paramNames.size()) {
								mapParams.put(target.paramNames.get(i), params[i]);
							}
						}
					}
				}
				data = target.target.invoke(target.methodName, mapParams);
			}
			if(data instanceof Message) {
				response.replace((Message)data);
			} else {
				response.setStatus(200); 
				response.setBody(data); 
			}
		} catch (InvocationTargetException e) {  
			Throwable t = e.getTargetException();
			if(t != null) {
				if(!stackTraceEnabled) {
					t.setStackTrace(new StackTraceElement[0]);
				}
			}
			response.setBody(t);
			response.setStatus(500); 
		} 
	}

	public void setBeforeFilter(RpcFilter beforeFilter) {
		this.beforeFilter = beforeFilter;
	} 

	public void setAfterFilter(RpcFilter afterFilter) {
		this.afterFilter = afterFilter;
	} 

	public void setAuthFilter(RpcFilter authFilter) {
		this.authFilter = authFilter;
	} 

	public boolean isStackTraceEnabled() {
		return stackTraceEnabled;
	}

	public void setStackTraceEnabled(boolean stackTraceEnabled) {
		this.stackTraceEnabled = stackTraceEnabled;
	}

	public boolean isMethodPageEnabled() {
		return methodPageEnabled;
	}

	public void setMethodPageEnabled(boolean methodPageEnabled) {
		this.methodPageEnabled = methodPageEnabled;
	}
	
	public void setMethodPageAuthEnabled(boolean methodPageAuthEnabled) {
		this.methodPageAuthEnabled = methodPageAuthEnabled;
	}

	public String getMethodPageModule() {
		return methodPageModule;
	}

	public void setMethodPageModule(String methodPageModule) {
		this.methodPageModule = methodPageModule;
	}
	
	public void setDocUrlRoot(String docUrlRoot) {
		this.docUrlRoot = docUrlRoot;
	} 
	
	static class MethodInstance {
		public String path;
		public String methodName; 
		public boolean authRequired = true;
		
		public Method reflectedMethod;
		public Object instance;   
		
		public MethodInvoker target;   
		public List<String> paramNames = new ArrayList<>(); 
		
		
		public boolean enableDoc = true;
		
		public MethodInstance(Method reflectedMethod, Object instance) {
			this.reflectedMethod = reflectedMethod;
			this.instance = instance;
			this.methodName = this.reflectedMethod.getName();
		}
		
		public MethodInstance(String methodName, MethodInvoker target) {  
			this.methodName = methodName;
			this.target = target;
		}
	}
}
