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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsonKit;
import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcMethod.MethodParam;
import io.zbus.rpc.annotation.Filter;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Route;
import io.zbus.rpc.doc.DocRender;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;
import io.zbus.transport.http.Http.FormData;

public class RpcProcessor {
	private static final Logger logger = LoggerFactory.getLogger(RpcProcessor.class);   
	private Map<String, MethodInstance> urlPath2MethodTable = new HashMap<>();   //path => MethodInstance   
	
	private boolean docEnabled = true;  
	private String docUrl = "/doc"; 
	private String rootUrl = "/"; 
	 
	private boolean stackTraceEnabled = true;   
	private boolean threadContextEnabled = true;
	
	private RpcFilter beforeFilter;
	private RpcFilter afterFilter; 
	private RpcFilter exceptionFilter;
	
	private Map<String, RpcFilter> filterTable = new HashMap<>(); //RpcFilter table referred by key
	
	public RpcProcessor mount(String urlPrefix, Object service) { 
		return mount(urlPrefix, service, true, true, true);
	}
	
	public RpcProcessor mount(String urlPrefix, Object service, boolean defaultAuth) {
		return mount(urlPrefix, service, defaultAuth, true, true);
	} 
	
	@SuppressWarnings("unchecked")
	public RpcProcessor mount(String urlPrefix, Object service, boolean defaultAuth, boolean enableDoc, boolean overrideMethod) {  
		if(service instanceof List) {
			List<Object> svcList = (List<Object>)service;
			for(Object svc : svcList) {
				mount(urlPrefix, svc, defaultAuth, enableDoc, overrideMethod);
			}
			return this;
		} 
		try {
			if(service instanceof Class<?>) {
				service = ((Class<?>)service).newInstance();
			} 
			
			List<RpcFilter> classFiltersIncluded = new ArrayList<>();  
			Filter filter = service.getClass().getAnnotation(Filter.class); 
			if(filter != null) {
				for(String name : filter.value()) {
					RpcFilter rpcFilter = filterTable.get(name);
					if(rpcFilter != null) {
						classFiltersIncluded.add(rpcFilter);
					}
				}
			}
			
			Method[] methods = service.getClass().getMethods(); 
			Route r = service.getClass().getAnnotation(Route.class);
			boolean defaultExcluded = false;
			if(r != null) defaultExcluded = r.exclude();
			
			for (Method m : methods) {
				if (m.getDeclaringClass() == Object.class) continue;  
				
				if(Modifier.isStatic(m.getModifiers())) {
					continue;
				}
				
				RpcMethod info = new RpcMethod();
				String methodName =  m.getName();
				//default path
				String urlPath = HttpKit.joinPath(urlPrefix, methodName);
				
				info.urlPath = urlPath;
				info.method = methodName; 
				info.docEnabled = enableDoc;
				info.setReturnType(m.getReturnType());
				
				Route p = m.getAnnotation(Route.class);
				if(p == null && defaultExcluded) continue;
				if (p != null) { 
					if (p.exclude()) continue; 
					info.docEnabled = enableDoc && p.docEnabled();
					info.urlAnnotation = p;
					urlPath = annoPath(p);    
					if(urlPath != null) {
						info.urlPath = HttpKit.joinPath(urlPrefix, urlPath);
					}
				}    
				info.filters.addAll(classFiltersIncluded);
				filter = m.getAnnotation(Filter.class);
				if(filter != null) {
					for(String name : filter.value()) {
						RpcFilter rpcFilter = filterTable.get(name);
						if(rpcFilter != null) {
							if(!info.filters.contains(rpcFilter)) {
								info.filters.add(rpcFilter);
							}
						}
					}
					for(String name : filter.exclude()) {
						RpcFilter rpcFilter = filterTable.get(name);
						if(rpcFilter != null) {
							info.filters.remove(rpcFilter);
						}
					}
				}
				
				m.setAccessible(true);  
				 
				for (Class<?> t : m.getParameterTypes()) {
					info.addParam(t); 
				} 
				Annotation[][] paramAnnos = m.getParameterAnnotations(); 
				int size = info.params.size(); 
				for(int i=0; i<size; i++) {
					Annotation[] annos = paramAnnos[i];
					for(Annotation annotation : annos) {
						if(Param.class.isAssignableFrom(annotation.getClass())) {
							Param param = (Param)annotation;  
							String paramName = param.name();
							if("".equals(paramName)) paramName = param.value();
							info.params.get(i).name = paramName; 
							info.params.get(i).fromContext = param.ctx();
							break;
						}
					} 
				}  
				
				//register in tables
				mount(new MethodInstance(info, m, service), overrideMethod);  
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	} 
	
	public RpcProcessor mount(RpcMethod spec, MethodInvoker service) {
		return mount(spec, service, true);
	}
	
	public RpcProcessor mount(RpcMethod spec, MethodInvoker service, boolean overrideMethod) {
		MethodInstance mi = new MethodInstance(spec, service);  
		return mount(mi, overrideMethod);
	}
	
	public RpcProcessor mount(MethodInstance mi) {
		return mount(mi, true);
	}
	
	public RpcProcessor mount(MethodInstance mi, boolean overrideMethod) { 
		RpcMethod spec = mi.info;
		String urlPath = spec.getUrlPath();
		if(urlPath == null) {
			throw new IllegalArgumentException("urlPath can not be null");
		}   
		
		boolean exists = this.urlPath2MethodTable.containsKey(urlPath);
		if (exists) {
			MethodInstance existsMethod = urlPath2MethodTable.get(urlPath);
			if(overrideMethod) {
				logger.warn(urlPath + "[" + mi.reflectedMethod + "] override " +  existsMethod.reflectedMethod); 
				this.urlPath2MethodTable.put(urlPath, mi); 
			} else {
				logger.warn(urlPath + "[" + mi.reflectedMethod + "] Ignored, Active= " +  existsMethod.reflectedMethod);  
			}
		} else {
			this.urlPath2MethodTable.put(urlPath, mi); 
		} 
		return this;
	} 
	
	public RpcProcessor unmount(String module, Object service) {
		try {
			Method[] methods = service.getClass().getMethods();
			for (Method m : methods) {
				String path = HttpKit.joinPath(module, m.getName());
				Route p = m.getAnnotation(Route.class);
				if (p != null) {
					if (p.exclude()) continue; 
					path = annoPath(p); 
				} 
				this.unmount(path); 
				this.unmount(module, m.getName());
			}
		} catch (SecurityException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}  
	
	public RpcProcessor unmount(String urlPath) { 
		this.urlPath2MethodTable.remove(urlPath);  
		return this;
	} 
	
	public RpcProcessor unmount(String module, String method) {  
		String urlPath = HttpKit.joinPath(module, method);
		unmount(urlPath);
		return this;
	}  
	
	private String annoPath(Route p) {
		if(p.path().length() == 0) return p.value();
		return p.path();
	} 
	
	public void process(Message req, Message response) {   
		try {  
			if (req == null) {
				req = new Message();  
			}   
			
			if(beforeFilter != null) {
				boolean next = beforeFilter.doFilter(req, response, null);
				if(!next) return;
			} 
			
			invoke(req, response);
			
			if(afterFilter != null) {
				afterFilter.doFilter(req, response, null);
			} 
		} catch (Throwable t) {
			logger.info(t.getMessage(), t);   
			
			if(exceptionFilter != null) {
				exceptionFilter.doFilter(req, response, t);
				
			} else {
				Object errorMsg = t.getMessage();
				if(errorMsg == null) errorMsg = t.getClass().toString(); 
				response.setBody(errorMsg);
				response.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
				response.setStatus(500); 
				
				if(t instanceof RpcException) {
					RpcException ex = (RpcException)t;
					response.setStatus(ex.getStatus());
				}  
			} 
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
	
	private class MethodTarget{
		public MethodInstance methodInstance;
		public Object[] params;
		public Map<String, Object> queryMap;
	}
	 
	private boolean httpMethodMatched(Message req, Route anno) { 
		if(anno.method().length == 0) {
			return true;
		}
		String httpMethod = req.getMethod();
		for(String m : anno.method()) {
			if(m.equalsIgnoreCase(httpMethod)) return true;
		}
		return false;
	}
	
	public boolean matchUrl(String url) {
		int length = 0;
		Entry<String, MethodInstance> matched = null;
		for(Entry<String, MethodInstance> e : urlPath2MethodTable.entrySet()) {
			String key = e.getKey();
			if(url.equals(key)||  url.startsWith(key+"/") || url.startsWith(key+"?")) {
				if(key.length() > length) {
					length = key.length();
					matched = e; 
				}
			}
		}  
		if(matched == null) return false;
		if(length == 1) { //root
			UrlInfo info = HttpKit.parseUrl(url);
			if(info.pathList.size()>0) { 
				return false; // root / should not with parameters
			}
		} 
		return true;
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
			reply(response, 404, String.format("URL=%s Not Found", url)); 
			return null;
		}  
		
		String urlPathMatched = matched.getKey();
		
		MethodTarget target = new MethodTarget(); 
		target.methodInstance = matched.getValue();
		Object[] params = null; 
		 
		Route anno = target.methodInstance.info.urlAnnotation;
		if(anno != null) {
			boolean httpMethodMatched = httpMethodMatched(req, anno);
			if(!httpMethodMatched) {
				reply(response, 405, String.format("Method(%s) Not Allowd", req.getMethod())); 
				return null;
			}
		}
		
		Object body = req.getBody(); //assumed to be params 
		if(body != null) {
			if(body instanceof JSONObject || body instanceof FormData) { 
				FormData form = JsonKit.convert(body, FormData.class); 
				req.setBody(form);
				if(form.files.isEmpty()) { //if no files upload, attributes same as queryString
					target.queryMap = form.attributes;
				} 
			} else {  
				JSON paramObject = JsonKit.convert(body, JSON.class);
				if(paramObject instanceof JSONArray) {
					params = JsonKit.convert(paramObject, Object[].class);
				} else {
					target.queryMap = (JSONObject)paramObject; 
				}
			}
		}   
		if(params == null) { 
			String subUrl = url.substring(urlPathMatched.length());
			UrlInfo info = HttpKit.parseUrl(subUrl);
			List<Object> paramList = new ArrayList<>(info.pathList); 
			if(!info.queryParamMap.isEmpty()) {
				target.queryMap = new HashMap<>(info.queryParamMap); 
			}
			params = paramList.toArray();
		} 
		target.params = params; 
		return target;
	}
	
	@SuppressWarnings("unchecked")
	private void invoke0(Message req, Message response) throws Exception {    
		String url = req.getUrl();   
		if(url == null) { 
			reply(response, 400, "url required");
			return;
		}  
		
		MethodTarget target = findMethodByUrl(req, response); 
		if(target == null) {
			reply(response, 404, String.format("URL=%s Not Found", url));
			return;   
		}
		
		Object[] params = target.params; 
		MethodInstance mi = target.methodInstance; 
		for(RpcFilter filter : mi.info.filters) {
			boolean next = filter.doFilter(req, response, null); //TODO Exception chain?
			if(!next) return;
		} 
		
		Object data = null;
		if(mi.reflectedMethod != null) {
			Class<?>[] targetParamTypes = mi.reflectedMethod.getParameterTypes();
			Object[] invokeParams = new Object[targetParamTypes.length];  
			
			applyParams(req, response, target, invokeParams); 
			
			data = mi.reflectedMethod.invoke(mi.instance, invokeParams);
			
		} else if(mi.target != null) {
			Map<String, Object> mapParams = new HashMap<>();  
			if(params != null) {
				if(params.length == 1 && params[0] instanceof Map) {
					mapParams = (Map<String, Object>)params[0]; 
				} else {
					RpcMethod m = mi.info;
					for(int i=0; i<m.params.size();i++) {
						String paramName = m.params.get(i).name;
						if(paramName == null) continue; //missing
						if(i<params.length) {
							mapParams.put(paramName, params[i]); 
						}  
					}  
					if(target.queryMap != null) {
						for(Entry<String, Object> e : target.queryMap.entrySet()) {
							if(mapParams.containsKey(e.getKey())) continue; //path match first
							mapParams.put(e.getKey(), e.getValue());
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
			response.setHeader(Http.CONTENT_TYPE, "application/json; charset=utf8"); 
			response.setBody(data); 
		} 
	}
	
	private Object convert(Object value, Class<?> paramType) {
		try {
			return JsonKit.convert(value, paramType);  
		} catch (Exception e) {
			throw new RpcException(400, String.format("Required parameter type(%s) bad format", paramType.getName())); 
		}
	}
	
	private void applyParams(Message req, Message res, MethodTarget target, Object[] invokeParams) { 
		Object[] params = target.params;  
		Method method = target.methodInstance.reflectedMethod; 
		Class<?>[] targetParamTypes = method.getParameterTypes();  
		
		List<MethodParam> declaredParams = target.methodInstance.info.params;
		if(declaredParams.size() != targetParamTypes.length) {
			throw new IllegalStateException("Mehtod param size mismatched");
		}
		int j = 0;
		for (int i = 0; i < targetParamTypes.length; i++) { 
			Class<?> paramType = targetParamTypes[i];
			if(Message.class.isAssignableFrom(paramType)) { //handle Message context injection
				invokeParams[i] = req;
				continue;
			}   
			
			MethodParam mp = declaredParams.get(i);
			if(mp.fromContext) {
				try {
					invokeParams[i] = convert(req.getContext(), paramType);  
				} catch (Exception e) {
					logger.warn(e.getMessage(), e); //ignore context conversion error, set to null
				}
				continue;
			}
			
			if(mp.name != null) {
				if(target.queryMap != null) {
					Object value = target.queryMap.get(mp.name);
					if(value != null) { 
						invokeParams[i] = convert(value, paramType);    
						continue;
					} 
				}
			}
			
			if(j >= params.length) { 
				if(target.queryMap != null) {
					try {
						invokeParams[i] = convert(target.queryMap, paramType); 
					} catch (Exception e) {
						//ignore
					}
				} else {
					invokeParams[i] = null;
				}
				return;
			} else { 
				invokeParams[i] = convert(params[j++], paramType);   
			}
		}  
	}
	 
	private void invoke(Message req, Message response) {   
		try {    
			if(threadContextEnabled) {
				InvocationContext.set(req, response);
			}
			invoke0(req, response);
		} catch (Throwable e) {  
			logger.info(e.getMessage(), e); //no need to print
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
			
			if(exceptionFilter != null) {
				exceptionFilter.doFilter(req, response, t);
			} else {
				Object errorMsg = t.getMessage();
				if(errorMsg == null) errorMsg = t.getClass().toString(); 
				response.setBody(errorMsg);
				response.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
				response.setStatus(500); 
				
				if(t instanceof RpcException) {
					RpcException ex = (RpcException)t;
					response.setStatus(ex.getStatus());
				}  
			} 
		}  
	}
	 
	public RpcProcessor mountDoc() { 
		if(!this.docEnabled) return this;
		DocRender render = new DocRender(this); 
		render.setRootUrl(rootUrl);
		mount(docUrl, render, false, false, false);
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
	
	@SuppressWarnings("unchecked")
	public void setModuleTable(Map<String, Object> instances){
		if(instances == null) return;
		for(Entry<String, Object> e : instances.entrySet()){
			Object svc = e.getValue();
			if(svc instanceof List) {
				mount(e.getKey(), (List<Object>)svc); 
			} else {
				mount(e.getKey(), svc);
			}
		}
	}  
	
	public Map<String, RpcFilter> getFilterTable() {
		return filterTable;
	}
	
	public void setFilterTable(Map<String, RpcFilter> filterTable) {
		this.filterTable = filterTable;
	}
	
	public String getDocUrl() {
		return docUrl;
	}

	public void setDocUrl(String docUrl) {
		this.docUrl = docUrl;
	}  

	public String getRootUrl() {
		return rootUrl;
	}

	public void setRootUrl(String rootUrl) {
		this.rootUrl = rootUrl;
	}
	
	public void setExceptionFilter(RpcFilter exceptionFilter) {
		this.exceptionFilter = exceptionFilter;
	}

	public void setThreadContextEnabled(boolean threadContextEnabled) {
		this.threadContextEnabled = threadContextEnabled;
	}
	
	public List<RpcMethod> rpcMethodList() { 
		List<RpcMethod> res = new ArrayList<>();
		TreeMap<String, MethodInstance> methods = new TreeMap<>(this.urlPath2MethodTable);
		Iterator<Entry<String, MethodInstance>> iter = methods.entrySet().iterator();
		while(iter.hasNext()) {
			MethodInstance mi = iter.next().getValue();
			res.add(mi.info); 
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
