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
	//2 addressing paths: 1) REST, from URL path  2) RPC, {module: xx,  method: xx }
	protected Map<String, MethodInstance> urlPath2MethodTable = new HashMap<>();             //path => MethodInstance 
	protected Map<String, Map<String, MethodInstance>> module2MethodTable = new HashMap<>(); //module =>{method => MethodInstance}
	
	protected String docUrlRoot = "/";
	protected boolean stackTraceEnabled = true;
	protected boolean methodPageEnabled = true; 
	protected boolean methodPageAuthEnabled = false;
	protected boolean overrideMethod = true;
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
				String urlPath = path(module, methodName);
				
				Path p = m.getAnnotation(Path.class);
				if (p != null) {
					if (p.exclude()) continue; 
					urlPath = p.value(); 
				} 
				
				Auth auth = m.getAnnotation(Auth.class);
				boolean authRequired = classAuthEnabled;
				if(auth != null) {
					authRequired = !auth.exclude();
				}

				m.setAccessible(true);
				
				MethodInstance mi = new MethodInstance(module, m, service); 
				mi.info.authRequired = authRequired;
				mi.info.docEnabled = enableDoc;
				
				List<String> paramTypes = new ArrayList<String>();
				for (Class<?> t : m.getParameterTypes()) {
					paramTypes.add(t.getCanonicalName());
				}
				mi.info.paramTypes = paramTypes;
				
				Annotation[][] paramAnnos = m.getParameterAnnotations(); 
				int size = paramTypes.size(); 
				for(int i=0; i<size; i++) {
					Annotation[] annos = paramAnnos[i];
					for(Annotation annotation : annos) {
						if(Param.class.isAssignableFrom(annotation.getClass())) {
							Param param = (Param)annotation;  
							mi.info.paramNames.add(param.value()); 
							break;
						}
					} 
				}  
				
				//register in tables
				
				boolean exists = this.urlPath2MethodTable.containsKey(urlPath);
				if (exists) {
					if(overrideMethod) {
						log.warn(urlPath + " overridden"); 
						this.urlPath2MethodTable.put(urlPath, mi); 
					} else {
						log.warn(urlPath + " exists, new ignored"); 
					}
				} else {
					this.urlPath2MethodTable.put(urlPath, mi); 
				}
				
				
				Map<String, MethodInstance> methodTable = this.module2MethodTable.get(module);
				if(methodTable == null) {
					methodTable = new HashMap<>();
					this.module2MethodTable.put(module, methodTable);
				}
				
				exists = methodTable.containsKey(methodName);
				if(exists) { 
					if(overrideMethod) {
						log.warn(String.format("module=%s, method=%s overridden", module, methodName));
						this.urlPath2MethodTable.put(urlPath, mi); 
					} else { 
						log.warn(String.format("module=%s, method=%s exists, new ignored", module, methodName));
					} 
				} else {
					methodTable.put(methodName, mi); 
				}
				
			}
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}
	} 
	
	public void addMethod(RpcMethod spec, MethodInvoker service) {
		MethodInstance mi = new MethodInstance(spec.module, spec.method, service);
		mi.info.paramNames = spec.paramNames;
		
		String path = path(spec.module, spec.method);
		this.urlPath2MethodTable.put(path, mi); 
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
				this.removeMethod(module, m.getName());
			}
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}
	}  
	
	public void removeMethod(String path) { 
		this.urlPath2MethodTable.remove(path);  
	} 
	
	public void removeMethod(String module, String method) { 
		String path = path(module, method);
		this.urlPath2MethodTable.remove(path);  
		
		Map<String, MethodInstance> table = this.module2MethodTable.get(module);
		if(table == null) {
			return;
		}
		table.remove(method);
		if(table.isEmpty()) {
			this.module2MethodTable.remove(module);
		}
	} 
	
	private MethodInstance matchMethodByUrl(String url) {  
		int length = 0;
		MethodInstance mi = null;
		for(Entry<String, MethodInstance> e : urlPath2MethodTable.entrySet()) {
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
	
	private MethodInstance matchMethod(String module, String method) {  
		Map<String, MethodInstance> table = this.module2MethodTable.get(module);
		if(table == null) {
			return null;
		}
		return table.get(method);
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
	 
	private void reply(Message response, int status, String message) {
		response.setStatus(status);
		response.addHeader(Http.CONTENT_TYPE, "text/plain; charset=utf8");
		response.setBody(message);
	} 
	
	@SuppressWarnings("unchecked")
	private void invoke(Message req, Message response) throws IllegalAccessException, IllegalArgumentException {   
		try {    
			String url = req.getUrl();  
			MethodInstance target = null;
			Object[] params = null;
			
			if(url == null) { //No URL 
				Object body = req.getBody();
				if(body == null || !(body instanceof Map)) {
					reply(response, 400, "Message body should be RPC request map"); 
					return;
				}
				Map<String, Object> reqBody = (Map<String, Object>)body;
				String module = (String)reqBody.get(Protocol.MODULE);
				String method = (String)reqBody.get(Protocol.METHOD);
				
				params = JsonKit.getArray(reqBody, Protocol.PARAMS);
				if(params == null) {
					params = new Object[0];
				}
				
				target = matchMethod(module, method);
			} else {
				target = matchMethodByUrl(url);  
			}
			
			if(target == null) {
				reply(response, 404, String.format("%s Not Found", url)); 
				return;
			}
			
			if(url != null) {
				Object body = req.getBody(); //assumed to be params 
				if(body != null && body instanceof Object[]) {
					params = (Object[])body;
				}  
				
				if(params == null) {
					String subUrl = url.substring(target.modulePath().length());
					UrlInfo info = HttpKit.parseUrl(subUrl);
					List<Object> paramList = new ArrayList<>(info.path); 
					if(!info.params.isEmpty()) {
						paramList.add(info.params);
					}
					params = paramList.toArray();
				}
			} 
			//////////////////////////////location of method completed/////////////////////////////////
			
			
			if(authFilter != null && target.info.authRequired) { 
				boolean next = authFilter.doFilter(req, response);
				if(!next) return;
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
							if(target.info.paramNames == null) break;
							if(i<target.info.paramNames.size()) {
								mapParams.put(target.info.paramNames.get(i), params[i]);
							}
						}
					}
				}
				data = target.target.invoke(target.info.method, mapParams);
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
		public RpcMethod info = new RpcMethod();  
		//Mode1 reflection method of class
		public Method reflectedMethod;
		public Object instance;    
		
		//Mode2 proxy to target
		public MethodInvoker target;     
		
		public String modulePath() {
			if(info.module == null) return "/";
			if(!info.module.startsWith("/")) {
				return "/"+info.module;
			}
			return info.module;
		} 
		
		public MethodInstance(String module, Method reflectedMethod, Object instance) {
			this.reflectedMethod = reflectedMethod;
			this.instance = instance; 
			this.info.module = module;
			this.info.method = reflectedMethod.getName();
		}
		
		public MethodInstance(String module, String methodName, MethodInvoker target) {  
			this.info.module = module;
			this.info.method = methodName;
			this.target = target;
		}
		
		public MethodInstance(RpcMethod info, MethodInvoker target) {
			if(info.method == null) {
				throw new IllegalArgumentException("method required");
			}
			this.info = info;
			this.target = target;
		}
		
		public MethodInstance(RpcMethod info, Method reflectedMethod, Object instance) {
			this.reflectedMethod = reflectedMethod;
			this.instance = instance; 
			this.info = info; 
			if(info.method == null) {
				this.info.method = reflectedMethod.getName(); 
			}
		}
		
	}
}
