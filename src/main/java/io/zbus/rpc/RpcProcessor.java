package io.zbus.rpc;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsonKit;
import io.zbus.rpc.annotation.Auth;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.rpc.doc.DocRender;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;

public class RpcProcessor {
	private static final Logger logger = LoggerFactory.getLogger(RpcProcessor.class);  
	//2 addressing paths: 1) REST, from URL path  2) RPC, {module: xx,  method: xx }
	protected Map<String, MethodInstance> urlPath2MethodTable = new HashMap<>();             //path => MethodInstance 
	protected Map<String, Map<String, MethodInstance>> module2MethodTable = new HashMap<>(); //module =>{method => MethodInstance}
	
	protected String docUrlPrefix = "/";
	protected boolean docEnabled = true; 
	protected String docModule = "index";
	protected boolean docAuthRequired = false;
	
	protected boolean stackTraceEnabled = true; 
	protected boolean overrideMethod = true;
	
	
	protected RpcFilter beforeFilter;
	protected RpcFilter afterFilter;
	protected RpcFilter authFilter;  
	
	public RpcProcessor addModule(Object service) {
		return addModule("", service);
	} 
	
	public RpcProcessor addModule(String module, Object service) { 
		return addModule(module, service, true);
	}
	
	public RpcProcessor addModule(String module, Object service, boolean enableDoc) {
		return addModule(module, service, enableDoc, true);
	} 
	
	public RpcProcessor addModule(String module, Object service, boolean defaultAuth, boolean enableDoc) {  
		
		if(module.startsWith("/")) {
			module = module.substring(1);
		}
		try {
			if(service instanceof Class<?>) {
				service = ((Class<?>)service).newInstance();
			} 
			
			Method[] methods = service.getClass().getMethods();
			boolean classAuthEnabled = defaultAuth;
			Auth classAuth = service.getClass().getAnnotation(Auth.class);
			if(classAuth != null) {
				classAuthEnabled = !classAuth.exclude();
			}
			
			for (Method m : methods) {
				if (m.getDeclaringClass() == Object.class) continue;  
				
				if(Modifier.isStatic(m.getModifiers())) {
					continue;
				}
				
				RpcMethod info = new RpcMethod();
				String methodName =  m.getName();
				String urlPath = path(module, methodName);
				
				RequestMapping p = m.getAnnotation(RequestMapping.class);
				if (p != null) { 
					if (p.exclude()) continue; 
					
					info.urlAnnotation = p;
					urlPath = annoPath(p);  
					//add module prefix
					if(!urlPath.startsWith("/")) {
						urlPath = "/"+urlPath;
					}
					urlPath = module + urlPath;
					if(!urlPath.startsWith("/")) {
						urlPath = "/"+urlPath;
					}
				} 
				
				Auth auth = m.getAnnotation(Auth.class);
				boolean authRequired = classAuthEnabled;
				if(auth != null) {
					authRequired = !auth.exclude();
				}

				m.setAccessible(true);
				
				
				info.urlPath = urlPath;
				info.module = module;
				info.method = methodName;
				info.authRequired = authRequired;
				info.docEnabled = enableDoc;
				info.returnType = m.getReturnType().getCanonicalName();
				
				
				List<String> paramTypes = new ArrayList<String>();
				for (Class<?> t : m.getParameterTypes()) {
					paramTypes.add(t.getCanonicalName());
				}
				info.paramTypes = paramTypes;  
				Annotation[][] paramAnnos = m.getParameterAnnotations(); 
				int size = paramTypes.size(); 
				for(int i=0; i<size; i++) {
					Annotation[] annos = paramAnnos[i];
					for(Annotation annotation : annos) {
						if(Param.class.isAssignableFrom(annotation.getClass())) {
							Param param = (Param)annotation;  
							info.paramNames.add(param.value()); 
							break;
						}
					} 
				}  
				
				//register in tables
				addMethod(new MethodInstance(info, m, service));  
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	} 
	
	public RpcProcessor addMethod(RpcMethod spec, MethodInvoker service) {
		MethodInstance mi = new MethodInstance(spec, service);  
		return addMethod(mi);
	}
	
	public RpcProcessor addMethod(MethodInstance mi) { 
		RpcMethod spec = mi.info;
		String urlPath = spec.urlPath;
		if(urlPath == null) {
			urlPath = path(spec.module, spec.method);;
		}   
		
		boolean exists = this.urlPath2MethodTable.containsKey(urlPath);
		if (exists) {
			if(overrideMethod) {
				logger.warn(urlPath + " overridden"); 
				this.urlPath2MethodTable.put(urlPath, mi); 
			} else {
				logger.warn(urlPath + " exists, new ignored"); 
			}
		} else {
			this.urlPath2MethodTable.put(urlPath, mi); 
		}
		String module = spec.module;
		if(module == null) module = "/";
		
		Map<String, MethodInstance> methodTable = this.module2MethodTable.get(module);
		if(methodTable == null) {
			methodTable = new HashMap<>();
			this.module2MethodTable.put(module, methodTable);
		}
		
		String methodName = spec.method;
		exists = methodTable.containsKey(methodName);
		if(exists) { 
			if(overrideMethod) {
				logger.warn(String.format("module=%s, method=%s overridden", module, methodName));
				this.urlPath2MethodTable.put(urlPath, mi); 
			} else { 
				logger.warn(String.format("module=%s, method=%s exists, new ignored", module, methodName));
			} 
		} else {
			methodTable.put(methodName, mi); 
		} 
		return this;
	} 
	
	public RpcProcessor removeModule(String module, Object service) {
		try {
			Method[] methods = service.getClass().getMethods();
			for (Method m : methods) {
				String path = path(module, m.getName());
				RequestMapping p = m.getAnnotation(RequestMapping.class);
				if (p != null) {
					if (p.exclude()) continue; 
					path = annoPath(p); 
				} 
				this.removeMethod(path); 
				this.removeMethod(module, m.getName());
			}
		} catch (SecurityException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}  
	
	public RpcProcessor removeMethod(String path) { 
		MethodInstance mi = this.urlPath2MethodTable.get(path);  
		if(mi != null) { 
			removeMethod(mi.info.module, mi.info.method);
		} 
		return this;
	} 
	
	public RpcProcessor removeMethod(String module, String method) {  
		Map<String, MethodInstance> table = this.module2MethodTable.get(module);
		if(table == null) {
			String path = path(module, method);
			this.urlPath2MethodTable.remove(path);  
			return this;
		}
		MethodInstance mi = table.remove(method);
		if(table.isEmpty()) {
			this.module2MethodTable.remove(module);
		}
		if(mi != null && mi.info.urlPath != null) {
			this.urlPath2MethodTable.remove(mi.info.urlPath);  
		}
		return this;
	}  
	
	private String annoPath(RequestMapping p) {
		if(p.path().length() == 0) return p.value();
		return p.path();
	}
	 
	private static String path(String module, String method) {
		if(!module.startsWith("/")) module = "/"+module;
		if(!module.endsWith("/")) module += "/";
		return module + method;
	}
	
	public void process(Message req, Message response) {   
		try {  
			if (req == null) {
				req = new Message();  
			}   
			
			if(beforeFilter != null) {
				boolean next = beforeFilter.doFilter(req, response);
				if(!next) return;
			} 
			
			invoke(req, response);
			
			if(afterFilter != null) {
				afterFilter.doFilter(req, response);
			} 
		} catch (Throwable e) {
			logger.info(e.getMessage(), e);  
			response.setBody(e.getMessage()); 
			response.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
			response.setStatus(500);
		} finally {
			response.setHeader(Protocol.ID, req.getHeader(Protocol.ID)); //Id Match
			if(response.getStatus() == null) {
				response.setStatus(200);
			}
		}  
	} 
	 
	private void reply(Message response, int status, String message) {
		response.setStatus(status);
		response.setHeader(Http.CONTENT_TYPE, "text/plain; charset=utf8");
		response.setBody(message);
	}  
	
	
	private boolean checkParams(Message req, Message res, Method method, Object[] params, Object[] invokeParams) {
		Class<?>[] targetParamTypes = method.getParameterTypes();
		int count = 0;
		for(Class<?> paramType : targetParamTypes) {
			if(Message.class.isAssignableFrom(paramType)) {
				continue;
			}
			count++;
		}
		
		if(count != params.length) { 
			String msg = String.format("Request(Url=%s, Method=%s, Params=%s) Bad Format", req.getUrl(), method.getName(), JsonKit.toJSONString(params));
			reply(res, 400, msg);
			return false;
		}
		
		for (int i = 0; i < targetParamTypes.length; i++) { 
			Class<?> paramType = targetParamTypes[i];
			if(Message.class.isAssignableFrom(paramType)) {
				invokeParams[i] = req;
				continue;
			}
			if(i >= params.length) {
				invokeParams[i] = null;
			} else {
				invokeParams[i] = JsonKit.convert(params[i], targetParamTypes[i]);  
			}
		} 
		return true;
	}
	
	
	private class MethodTarget{
		public MethodInstance methodInstance;
		public Object[] params;
	}
	
	
	@SuppressWarnings("unchecked")
	private MethodTarget findMethodByRpcFormat(Message req, Message response) { 
		Object body = req.getBody();
		if(body == null) {
			reply(response, 400, "Message body missing in RPC request"); 
			return null;
		}
		Map<String, Object> reqBody = null;
		try {
			reqBody = JsonKit.convert(body,Map.class);
		}catch (Exception e) { 
			reply(response, 400, "Message body should be RPC request format"); 
		}
		
		String module = (String)reqBody.get(Protocol.MODULE);  
		String method = (String)reqBody.get(Protocol.METHOD);
		if(module == null) module = "index";
		if(method == null) method = "index";
		
		Object[] params = JsonKit.getArray(reqBody, Protocol.PARAMS);
		if(params == null) {
			params = new Object[0];
		} 
		
		Map<String, MethodInstance> table = this.module2MethodTable.get(module);
		if(table == null) {
			reply(response, 404, String.format("module=%s Not Found", module, method)); 
			return null;
		} 
		MethodInstance mi = table.get(method);
		if(mi == null) {
			reply(response, 404, String.format("module=%s, method=%s Not Found", module, method)); 
			return null;
		}  
		
		MethodTarget target = new MethodTarget();
		target.methodInstance = mi;
		target.params = params;
		return target;
	}
	
	private boolean httpMethodMatached(Message req, RequestMapping anno) { 
		if(anno.method().length == 0) {
			return true;
		}
		String httpMethod = req.getMethod();
		for(String m : anno.method()) {
			if(m.equalsIgnoreCase(httpMethod)) return true;
		}
		return false;
	}
	
	private MethodTarget findMethodByUrl(Message req, Message response) {  
		String url = req.getUrl();  
		int length = 0;
		Entry<String, MethodInstance> matched = null;
		for(Entry<String, MethodInstance> e : urlPath2MethodTable.entrySet()) {
			String key = e.getKey();
			if(url.startsWith(key)) {
				if(key.length() > length) {
					length = key.length();
					matched = e; 
				}
			}
		}  
		if(matched == null) {
			reply(response, 404, String.format("Url=%s Not Found", url)); 
			return null;
		}
		
		String urlPathMatched = matched.getKey();
		
		MethodTarget target = new MethodTarget(); 
		target.methodInstance = matched.getValue();
		Object[] params = null; 
		
		//TODO more support on URL parameters 
		RequestMapping anno = target.methodInstance.info.urlAnnotation;
		if(anno != null) {
			boolean httpMethodMatched = httpMethodMatached(req, anno);
			if(!httpMethodMatched) {
				reply(response, 405, String.format("Method(%s) Not Allowd", req.getMethod())); 
				return null;
			}
		}
		
		Object body = req.getBody(); //assumed to be params 
		if(body != null && body instanceof Object[]) {
			params = (Object[])body;
		}   
		if(params == null) { 
			String subUrl = url.substring(urlPathMatched.length());
			UrlInfo info = HttpKit.parseUrl(subUrl);
			List<Object> paramList = new ArrayList<>(info.path); 
			if(!info.params.isEmpty()) {
				paramList.add(info.params);
			}
			params = paramList.toArray();
		} 
		target.params = params; 
		return target;
	}
	
	@SuppressWarnings("unchecked")
	private void invoke0(Message req, Message response) throws Exception {    
		String url = req.getUrl();  
		MethodTarget target = null; 
		if(url == null) { //match by Message body
			target = findMethodByRpcFormat(req, response); 
		} else { //match by URL
			target = findMethodByUrl(req, response); 
		} 
		if(target == null) return;   
		
		Object[] params = target.params; 
		MethodInstance mi = target.methodInstance;
		
		//Authentication step in if required
		if(authFilter != null && mi.info.authRequired) { 
			boolean next = authFilter.doFilter(req, response);
			if(!next) return;
		}  
		
		Object data = null;
		if(mi.reflectedMethod != null) {
			Class<?>[] targetParamTypes = mi.reflectedMethod.getParameterTypes();
			Object[] invokeParams = new Object[targetParamTypes.length];  
			
			boolean ok = checkParams(req, response, mi.reflectedMethod, params, invokeParams);
			if(!ok) return;

			data = mi.reflectedMethod.invoke(mi.instance, invokeParams);
			
		} else if(mi.target != null) {
			Map<String, Object> mapParams = new HashMap<>();  
			if(params != null) {
				if(params.length == 1 && params[0] instanceof Map) {
					mapParams = (Map<String, Object>)params[0]; 
				} else {
					for(int i=0;i <params.length; i++) {
						if(mi.info.paramNames == null) break;
						if(i<mi.info.paramNames.size()) {
							mapParams.put(mi.info.paramNames.get(i), params[i]);
						}
					}
				}
			}
			data = mi.target.invoke(mi.info.method, mapParams);
		}
		
		if(data instanceof Message) {
			response.replace((Message)data);
		} else {
			response.setStatus(200); 
			response.setBody(data); 
		} 
	}
	 
	private void invoke(Message req, Message response) {   
		try {     
			invoke0(req, response);
		} catch (Throwable e) {  
			logger.error(e.getMessage(), e);
			Throwable t = e;
			if(t instanceof InvocationTargetException) {
				t  = ((InvocationTargetException)e).getTargetException();
				if(t == null) {
					t = e;
				}
			}  
			if(!stackTraceEnabled) {
				t.setStackTrace(new StackTraceElement[0]);
			}
			response.setBody(t.getMessage());
			response.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
			response.setStatus(500); 
		}  
	}
	
	public RpcProcessor enableDoc() { 
		DocRender render = new DocRender(this, docUrlPrefix); 
		if(!this.urlPath2MethodTable.containsKey(path(docModule, ""))) {
			addModule(docModule, render, false, false);
		} 
		if(!this.urlPath2MethodTable.containsKey("/")) {
			addModule("", render, false, false);
		} 
		return this;
	}   

	public RpcProcessor setBeforeFilter(RpcFilter beforeFilter) {
		this.beforeFilter = beforeFilter;
		return this;
	} 

	public RpcProcessor setAfterFilter(RpcFilter afterFilter) {
		this.afterFilter = afterFilter;
		return this;
	} 

	public RpcProcessor setAuthFilter(RpcFilter authFilter) {
		this.authFilter = authFilter;
		return this;
	} 

	public boolean isStackTraceEnabled() {
		return stackTraceEnabled;
	}

	public RpcProcessor setStackTraceEnabled(boolean stackTraceEnabled) {
		this.stackTraceEnabled = stackTraceEnabled;
		return this;
	}

	public boolean isDocEnabled() {
		return docEnabled;
	}

	public RpcProcessor setDocEnabled(boolean docEnabled) {
		this.docEnabled = docEnabled;
		return this;
	}
	
	public RpcProcessor setDocAuthRequired(boolean docAuthRequired) {
		this.docAuthRequired = docAuthRequired;
		return this;
	}

	public String getDocModule() {
		return docModule;
	}

	public RpcProcessor setDocModule(String docModule) {
		this.docModule = docModule;
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public void setModuleTable(Map<String, Object> instances){
		if(instances == null) return;
		for(Entry<String, Object> e : instances.entrySet()){
			Object svc = e.getValue();
			if(svc instanceof List) {
				addModule(e.getKey(), (List<Object>)svc); 
			} else {
				addModule(e.getKey(), svc);
			}
		}
	}
	
	public RpcProcessor setDocUrlPrefix(String docUrlPrefix) {
		this.docUrlPrefix = docUrlPrefix;
		return this;
	} 
	 
	
	public List<RpcMethod> rpcMethodList() { 
		List<RpcMethod> res = new ArrayList<>();
		TreeMap<String, Map<String, MethodInstance>> methods = new TreeMap<>(this.module2MethodTable);
		Iterator<Entry<String, Map<String, MethodInstance>>> iter = methods.entrySet().iterator();
		while(iter.hasNext()) {
			TreeMap<String, MethodInstance> objectMethods = new TreeMap<>(iter.next().getValue()); 
			for(MethodInstance m : objectMethods.values()) { 
				res.add(m.info);
			}
		} 
		return res;
	}
	
	public static class MethodInstance {
		public RpcMethod info = new RpcMethod();    
		
		//Mode1 reflection method of class
		public Method reflectedMethod;
		public Object instance;    
		
		//Mode2 proxy to target
		public MethodInvoker target;      
		
		public MethodInstance(RpcMethod info, MethodInvoker target) {
			if(info.method == null) {
				throw new IllegalArgumentException("method required");
			}
			this.info = info;
			if(this.info.urlPath == null) {
				this.info.urlPath = path(info.module, info.method);
			}
			this.target = target;
		}
		
		public MethodInstance(RpcMethod info, Method reflectedMethod, Object instance) {
			this.reflectedMethod = reflectedMethod;
			this.instance = instance; 
			this.info = info; 
			if(info.method == null) {
				this.info.method = reflectedMethod.getName(); 
			}
			if(this.info.urlPath == null) {
				this.info.urlPath = path(info.module, info.method);
			}
		} 
	}
}
