package io.zbus.rpc;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import io.zbus.kit.ClassKit;
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
	private Map<String, List<MethodInstance>> urlPath2MethodTable = new HashMap<>();   //path => MethodInstance   
	
	private boolean docEnabled = true;  
	private String docUrl = "/doc"; 
	private String docFile = "static/rpc.html";
	private String rootUrl = "/"; 
	 
	private boolean stackTraceEnabled = true;   
	private boolean threadContextEnabled = true;
	
	private boolean embbedPageResource = true;
	
	private RpcFilter beforeFilter;
	private RpcFilter afterFilter; 
	private RpcFilter exceptionFilter;
	
	private Map<String, RpcFilter> annotationFilterTable = new HashMap<>(); //RpcFilter table referred by key
	
	private Map<String, List<RpcFilter>> urlFilterTable = new HashMap<>();
	private Map<String, List<RpcFilter>> urlExcludedFilterTable = new HashMap<>();
	
	private Set<String> urlExcludedSet = new HashSet<>();
	private Map<String, Object> moduleTable = null;//{module => List<service object> }
	
	/**
	 * Mount internal moduleTable
	 * @return
	 */
	public RpcProcessor mount() {
		if(moduleTable == null) return this;
		for(Entry<String, Object> e : moduleTable.entrySet()){ 
			mount(e.getKey(), e.getValue());
		}
		moduleTable = null; //mount just only once
		return this;
	}
	
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
					RpcFilter rpcFilter = annotationFilterTable.get(name);
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
				info.setGenericReturnType(m.getGenericReturnType());
				info.setReturnType(m.getReturnType());
				
				Route p = m.getAnnotation(Route.class); 
				
				if(p == null && defaultExcluded) continue;  
				if (p != null) { 
					if (p.exclude()) continue; 
					info.docEnabled = enableDoc && p.docEnabled();
					info.urlAnnotation = p;
					info.ignoreResult = p.ignoreResult();
					urlPath = annoPath(p);    
					
					if(urlPath != null) {
						info.urlPath = HttpKit.joinPath(urlPrefix, urlPath);
					} 
				}     
				if(matchUrlExcluded(info.urlPath)) continue; // excluded 
				
				info.filters.addAll(classFiltersIncluded);
				filter = m.getAnnotation(Filter.class);
				if(filter != null) {
					for(String name : filter.value()) {
						RpcFilter rpcFilter = annotationFilterTable.get(name);
						if(rpcFilter != null) {
							if(!info.filters.contains(rpcFilter)) {
								info.filters.add(rpcFilter);
							}
						}
					}
					for(String name : filter.exclude()) {
						RpcFilter rpcFilter = annotationFilterTable.get(name);
						if(rpcFilter != null) {
							info.filters.remove(rpcFilter);
						}
					}
				}
				
				List<RpcFilter> filters = matchFilter(info.urlPath, this.urlFilterTable);
				if(filters != null) {
					for(RpcFilter f : filters) {
						if(info.filters.contains(f)) continue;
						info.filters.add(f);
					} 
				}
				
				filters = matchFilter(info.urlPath, this.urlExcludedFilterTable);
				if(filters != null) {
					for(RpcFilter f : filters) { 
						info.filters.remove(f);
					} 
				}
				
				m.setAccessible(true);  
				Class<?>[] paramTypes = m.getParameterTypes();
				String[] paramNames = ClassKit.getParameterNames(m);
				for (int i = 0; i < paramTypes.length; i++) {
					String paramName = paramNames == null ? "arg"+i : paramNames[i];
					String paramType = paramTypes[i].getName();
					Type paramGenType = m.getGenericParameterTypes()[i];
					String paramGenTypeName = paramGenType != null ? paramGenType.getTypeName() : null;
					info.addParam(paramTypes[i], paramName, paramType, paramGenTypeName);
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
		 
		List<MethodInstance> methodList = urlPath2MethodTable.get(urlPath);
		if (methodList != null && !methodList.isEmpty()) { 
			boolean exists = methodList.contains(mi);
			if(!exists) {
				methodList.add(mi);
			} else {
				logger.warn(urlPath + "[" + mi.reflectedMethod + "] Ignored");   
			}
			
		} else { 
			methodList = new ArrayList<>();
			methodList.add(mi);
			this.urlPath2MethodTable.put(urlPath, methodList); 
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
	
	public void unmount(String urlPath) { 
		this.urlPath2MethodTable.remove(urlPath); 
	} 
	 
	public void unmount(String module, String method) {  
		String urlPath = HttpKit.joinPath(module, method);
		unmount(urlPath); 
	}   
	
	private List<RpcFilter> matchFilter(String url, Map<String, List<RpcFilter>> table){
		for(Entry<String, List<RpcFilter>> e : table.entrySet()) {
			String filterUrl = e.getKey();
			if(HttpKit.urlMatched(url, filterUrl)) {
				return e.getValue();
			}
		} 
		return null;
	}
	
	private boolean matchUrlExcluded(String url){
		for(String pattern : urlExcludedSet) {
			if(HttpKit.urlMatched(url, pattern)) return true;
		}
		return false;
	}
	 
	public int enableUrl(String urlPath, boolean status) { 
		List<MethodInstance> methods = urlPath2MethodTable.get(urlPath);
		if(methods == null || methods.isEmpty()) return 0;
		for(MethodInstance mi : methods) {
			mi.info.enabled = status;
		}
		return methods.size();
	}  
	
	public void rewriteUrl(String rawUrl, String newUrl) { 
		List<MethodInstance> methods = urlPath2MethodTable.get(rawUrl);
		if(methods == null) {
			throw new IllegalArgumentException("Rewrite url error: Not found for " + rawUrl);
		}
		
		List<MethodInstance> newMethods = urlPath2MethodTable.get(rawUrl);
		if(newMethods != null) {
			newMethods.addAll(methods);
			return;
		}
		urlPath2MethodTable.remove(rawUrl);
		urlPath2MethodTable.put(newUrl, methods);  
	}  
	
	private String annoPath(Route p) {
		if(p.path().length() == 0) return p.value();
		return p.path();
	} 
	
	private void defaultExceptionHandler(Throwable t, Message response) {
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
	
	public void process(Message req, Message response) {   
		try {  
			if (req == null) {
				req = new Message();  
			}
			if (this.threadContextEnabled) {
				InvocationContext.set(req, response);
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
			logger.error(t.getMessage(), t);    
			if(exceptionFilter != null) {
				try {
					exceptionFilter.doFilter(req, response, t);
				} catch (Exception e) { 
					defaultExceptionHandler(e, response);
				}
			} else {
				defaultExceptionHandler(t, response);
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
		Entry<String, List<MethodInstance>> matched = null;
		for(Entry<String, List<MethodInstance>> e : urlPath2MethodTable.entrySet()) {
			String key = e.getKey();
			if(url.equals(key) ||  url.startsWith(key+"/") || url.startsWith(key+"?")) {
				if(key.length() > length) {
					length = key.length();
					matched = e; 
				}
			}
		}  
		if(matched == null) return false;
		if(length == 1) { //root
			UrlInfo urlInfo = HttpKit.parseUrl(url);
			if(urlInfo.pathList.size() > 0) { 
				return false; // root / should not with parameters
			}
		} 
		return true;
	}
	
	private boolean isSimpleType(Class<?> c) {  
		return c.isPrimitive() ||
				Number.class.isAssignableFrom(c) ||
				c == String.class ||
				c == Date.class;
	}
	//0 - not matche, 1 - full match, 2 - partial match
	private int classMatch(Class<?> target, Class<?> c) {
		if(target == c) return 1;   
		
		if(target.isAssignableFrom(c)) {
			if(target == Object.class) { //Object special case
				return 2;
			}
			return 1; 
		}
		
		if(isSimpleType(target) && isSimpleType(c)) return 2;   
		
		if(!isSimpleType(target) && !isSimpleType(c)) return 2; //possible to convert through json

		return 0;
	}
	
	private void matchMethod(MethodTarget target, List<MethodInstance> instances) {
		if(instances.size() == 0) {
			target.methodInstance =instances.get(0);
			return;
		}
		List<Object> params = new ArrayList<>(); 
		for(Object p : target.params) params.add(p);
		int paramCount = target.params.length;
		if(target.queryMap != null) {
			paramCount++;
			params.add(target.queryMap);
		}
		  
		MethodInstance defaultMethod = instances.get(0);
		for(MethodInstance mi : instances) {
			if(mi.target != null) continue; 
			if(mi.info.params.size() != paramCount) continue;
			
			boolean matched = true; 
			boolean fullMatch = true; 
			boolean methodParamObjectTyped = false;
			for(int i=0;i<paramCount;i++) {
				Object param = params.get(i);
				if(param == null) continue; 
				
				Class<?> paramType = mi.info.params.get(i).type;
				if(paramType == Object.class) {
					methodParamObjectTyped = true;
				}
				int rc = classMatch(paramType, param.getClass());
				if(rc == 0) {
					matched = false;
					break;
				}
				
				if(rc == 2) {
					fullMatch = false;
				}
			}
			
			if(matched) { 
				if(fullMatch) { //full matched, just return 
					target.methodInstance = mi;
					return;
				}   
				if(methodParamObjectTyped) { //Object typed, as default
					defaultMethod = mi;
				} else {
					target.methodInstance = mi;
				}
			}
		}
		//default to first
		if(target.methodInstance == null) {
			target.methodInstance = defaultMethod;
		}
	}
	
	/* dynamic path
	 * 1. single entity
	 * GET,/users/{id}
	 * POST,/users
	 * GET,/users
	 * PUT,/users/{id}
	 * PATCH,/users/{id}
	 * DELETE,/users/{id}
	 * 
	 * 2. nested entity
	 * GET,/users/{uid}/pets
	 * POST,/users/{uid}/pets
	 * GET,/users/{uid}/pets/{pid}
	 * DELETE,/users/{uid}/pets/{pid}
	 * PUT,/users/{uid}/pets/{pid}
	 * PATCH,/users/{uid}/pets/{pid}
	 * 
	 * 3. nested dynamic path, 注意：**一定是出现在末尾的, 若key前面有问号，则表示可选
	 * GET,/api/{module}/filters/{?filter_paths:**}
	 */
	private static boolean matchDynamicPath(Message req, String routeKey) {
		if (routeKey == null || (routeKey = routeKey.trim()).length() == 0) {
			return false;
		}
		routeKey = routeKey.replace("?", "~");
		String reqMethod = req.getMethod();
		String[] split1 = routeKey.split(",");
		String routeMethod = null;
		String routePath = split1[0];
		if (split1.length > 1) {
			routeMethod = routePath;
			routePath = split1[1];
		}
		// 匹配 Http Method
		if (routeMethod != null && !reqMethod.equalsIgnoreCase(routeMethod)) {
			return false;
		}
		if (!routeKey.contains("{") || !routeKey.contains("}")) {
			if (routeMethod != null) {
				// match <method,path>
				UrlInfo reqUrlInfo = HttpKit.parseUrl(req.getUrl());
				UrlInfo routeUrlInfo = HttpKit.parseUrl(routePath);
				if (routeUrlInfo.urlPath.equals(reqUrlInfo.urlPath)) {
					return true;
				}
			}
			
			return false;
		}
		
		UrlInfo reqUrlInfo = HttpKit.parseUrl(req.getUrl());
		UrlInfo routeUrlInfo = HttpKit.parseUrl(routePath);
		// 通配符 /{key:**} (支持路径) 一定是放到最末尾的
		String urlSuffixParamValue = null;
		String urlSuffixParamName = null;
		boolean urlSuffixParamOptional = false;
		int lastPartIdxOfRoute = routeUrlInfo.pathList.size()-1;
        if (lastPartIdxOfRoute <= reqUrlInfo.pathList.size()) {
            String lastRouteUrlPart = routeUrlInfo.pathList.get(lastPartIdxOfRoute);
            String lrup = lastRouteUrlPart;
            if (lrup.startsWith("{") && lrup.endsWith(":**}") && lrup.length() > 5) {
                urlSuffixParamName = lrup.substring(1, lrup.length() - 4);
                urlSuffixParamOptional = urlSuffixParamName.startsWith("~");
                if (urlSuffixParamOptional) {
                	urlSuffixParamName = urlSuffixParamName.substring(1);
                } else if (lastPartIdxOfRoute == reqUrlInfo.pathList.size()) {
                	// 若动态后缀不是可选的，则严格要求请求的url必须要有后缀部分
                	return false;
                }
                // 获取动态后缀
                if (lastPartIdxOfRoute < reqUrlInfo.pathList.size()) {
                	urlSuffixParamValue = String.join("/", reqUrlInfo.pathList.subList(lastPartIdxOfRoute, reqUrlInfo.pathList.size()));
                	// 截断动态后缀url，让路由和截断后的url分割部分保持一致
                    reqUrlInfo.pathList = reqUrlInfo.pathList.subList(0, lastPartIdxOfRoute);
                } else {
                	urlSuffixParamValue = "";
                }
                
                routeUrlInfo.pathList = routeUrlInfo.pathList.subList(0, lastPartIdxOfRoute);
            }
        }
		// 匹配路径，要求分割部分数量一致
		if (routeUrlInfo.pathList.size() != reqUrlInfo.pathList.size()) {
			return false;
		}
		boolean pathMatched = false;
		Map<String, String> pathParams = new HashMap<String, String>();
		if (urlSuffixParamName != null) {
			pathParams.put(urlSuffixParamName, urlSuffixParamValue);
		}
		for (int i = 0; i < routeUrlInfo.pathList.size(); i++) {
			String p1 = routeUrlInfo.pathList.get(i);
			String p2 = reqUrlInfo.pathList.get(i);
			// 动态部分解析参数名和参数值
			if (p1.startsWith("{") && p1.endsWith("}")) {
				String paramName = p1.substring(1, p1.length()-1);
				pathParams.put(paramName, p2);
				pathMatched = true;
			} else {
				// 非动态部分要求完全相等
				if (p1.equals(p2)) {
					pathMatched = true;
				} else {
					pathMatched = false;
					break;
				}
			}
		}
		if (!pathMatched) {
			return false;
		}
		req.setPathParams(pathParams);
		return true;
	} 
	
	private MethodTarget findMethodByUrl(Message req, Message response) {  
		String url = req.getUrl();  
		int length = -1;
		Entry<String, List<MethodInstance>> matched = null;
		for(Entry<String, List<MethodInstance>> e : urlPath2MethodTable.entrySet()) {
			String key = e.getKey();
			if(key.endsWith("/")) key = key.substring(0, key.length()-1); //for key='/'
			if(url.startsWith(key+"/") || url.equals(key) || url.startsWith(key+"?")) {
				if(key.length() > length) {
					length = e.getKey().length();
					matched = e; 
				}
			} else {
				boolean bool = matchDynamicPath(req, e.getKey());
				if (bool) {
					matched = e;
					break;
				}
			}
		}  
		if(matched == null) {
			reply(response, 404, String.format("URL=%s Not Found", url)); 
			return null;
		}  
		
		String urlPathMatched = matched.getKey();
		req.setPathMatched(urlPathMatched);
		MethodTarget target = new MethodTarget();  
		Object[] params = null;  
		Object body = req.getBody(); //assumed to be params 
		if(body != null) {//body parameters goes first
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
		//Body parameters goes first, only if body has no parameters, check url part
		if(params == null) { 
			String subUrl = url;
			if (url.length() > urlPathMatched.length()) {
				subUrl = url.substring(urlPathMatched.length());
			}
			UrlInfo info = HttpKit.parseUrl(subUrl);
			List<Object> paramList = new ArrayList<>(info.pathList); 
			if(!info.queryParamMap.isEmpty()) {
				target.queryMap = new HashMap<>(info.queryParamMap); 
			}
			params = paramList.toArray();
		} 
		target.params = params;  
		
		matchMethod(target, matched.getValue());
		
		Route anno = target.methodInstance.info.urlAnnotation;
		 
		if(anno != null) {
			boolean httpMethodMatched = httpMethodMatched(req, anno);
			if(!httpMethodMatched) {
				reply(response, 405, String.format("Method(%s) Not Allowd", req.getMethod())); 
				return null;
			}
		}
		
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
		if(target == null || !target.methodInstance.info.enabled) {
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
		
		if(mi.info.ignoreResult) { // method has put result in response
			if(response.getStatus() == null) {
				response.setStatus(200);
			} 
			return;
		}
		
		if(data instanceof Message) {
			response.replace((Message)data);
		} else {
			response.setStatus(200); 
			response.setHeader(Http.CONTENT_TYPE, "application/json; charset=utf8"); 
			response.setBody(data); 
		} 
	}
	
	private Object convert(MethodParam mp, Object value, Class<?> paramType) {
		try {
			return JsonKit.convert(value, paramType);  
		} catch (Exception e) {
			String error = String.format("Bad format: Required parameter type=%s, but value(%s) received", mp.type, value);
			throw new RpcException(400, error); 
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
					invokeParams[i] = convert(mp, req.getContext(), paramType);  
				} catch (Exception e) {
					logger.warn(e.getMessage(), e); //ignore context conversion error, set to null
				}
				continue;
			}
			
			if(mp.name != null) {
				if(target.queryMap != null) {
					Object value = target.queryMap.get(mp.name);
					if(value != null) { 
						invokeParams[i] = convert(mp, value, paramType);    
						continue;
					} 
				}
			}
			
			if(j >= params.length) { 
				if(target.queryMap != null) {
					try {
						invokeParams[i] = convert(mp, target.queryMap, paramType); 
					} catch (Exception e) {
						//ignore
					}
				} else {
					invokeParams[i] = null;
				}
				return;
			} else { 
				invokeParams[i] = convert(mp, params[j++], paramType);   
			}
		}  
	}
	 
	private void invoke(Message req, Message response) {   
		try {    
			invoke0(req, response);
		} catch (Throwable e) {  
			//logger.info(e.getMessage(), e); //no need to print
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
				try {
					exceptionFilter.doFilter(req, response, t);
				}catch (Throwable ex) { 
					defaultExceptionHandler(ex, response);
				}
			} else {
				defaultExceptionHandler(t, response);
			} 
		}  
	}
	 
	public RpcProcessor mountDoc() { 
		if(!this.docEnabled) return this;
		DocRender render = new DocRender(this); 
		render.setRootUrl(rootUrl);
		render.setDocFile(docFile);
		render.setEmbbedPageResource(this.embbedPageResource);
		
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
	 
	public Set<String> getUrlExcludedSet() {
		return urlExcludedSet;
	}
	
	public void setUrlExcludedSet(Set<String> urlExcludedSet) {
		this.urlExcludedSet = urlExcludedSet;
	}
	 
	public void setModuleTable(Map<String, Object> instances){
		this.moduleTable = instances; 
	}   
	
	public Map<String, RpcFilter> getAnnotationFilterTable() {
		return annotationFilterTable;
	}
	
	/** 
	 * 
	 * Used by  @Filter(filterName) 
	 * 
	 * @param filterTable FilterName => Filter
	 */
	public void setAnnotationFilterTable(Map<String, RpcFilter> filterTable) {
		this.annotationFilterTable = filterTable;
	}
	
	public Map<String, List<RpcFilter>> getUrlFilterTable() {
		return urlFilterTable;
	}
	
	public void setUrlFilterTable(Map<String, List<RpcFilter>> urlFilterTable) {
		this.urlFilterTable = urlFilterTable;
	}
	
	public void setUrlExcludedFilterTable(Map<String, List<RpcFilter>> urlExcludedFilterTable) {
		this.urlExcludedFilterTable = urlExcludedFilterTable;
	}
	
	public Map<String, List<RpcFilter>> getUrlExcludedFilterTable() {
		return urlExcludedFilterTable;
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
	
	
	public String getDocFile() {
		return docFile;
	}

	public void setDocFile(String docFile) {
		this.docFile = docFile;
	}

	public void setExceptionFilter(RpcFilter exceptionFilter) {
		this.exceptionFilter = exceptionFilter;
	}

	public void setThreadContextEnabled(boolean threadContextEnabled) {
		this.threadContextEnabled = threadContextEnabled;
	}
	 
	public boolean isEmbbedJavascript() {
		return embbedPageResource;
	}

	public void setEmbbedJavascript(boolean embbedJavascript) {
		this.embbedPageResource = embbedJavascript; 
	}

	public List<RpcMethod> rpcMethodList() { 
		List<RpcMethod> res = new ArrayList<>();
		TreeMap<String, List<MethodInstance>> methods = new TreeMap<>(this.urlPath2MethodTable);
		Iterator<Entry<String, List<MethodInstance>>> iter = methods.entrySet().iterator();
		while(iter.hasNext()) {
			List<MethodInstance> methodList = iter.next().getValue();
			for(MethodInstance mi : methodList) {
				res.add(mi.info);
			} 
		} 
		return res;
	} 
	
	public Map<String, List<MethodInstance>> getUrlPath2MethodTable() {
		return urlPath2MethodTable;
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((info == null) ? 0 : info.hashCode());
			result = prime * result + ((instance == null) ? 0 : instance.hashCode());
			result = prime * result + ((reflectedMethod == null) ? 0 : reflectedMethod.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MethodInstance other = (MethodInstance) obj;
			if (info == null) {
				if (other.info != null)
					return false;
			} else if (!info.equals(other.info))
				return false;
			if (instance == null) {
				if (other.instance != null)
					return false;
			} else if (!instance.equals(other.instance))
				return false;
			if (reflectedMethod == null) {
				if (other.reflectedMethod != null)
					return false;
			} else if (!reflectedMethod.equals(other.reflectedMethod))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		} 
		
		
	}
}
