/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zbus.transport;
 

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.function.Function;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.transport.http.Http.FormData;
/**
 * Message takes format of standard HTTP:
 * <p> key-value headers  
 * <p> body of any time which way serialized is controlled in headers's 'content-type' value  
 * 
 * <p> When Message parsed as request, url and method are in use.
 * <p> When Message parsed as response, status of HTTP is in use, 
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public class Message {   
	public static final String ID = "id";
	
	protected String url; 
	protected String method;   
	
	protected Integer status; //null: request, otherwise: response  
	
	protected TreeMap<String, Object> headers = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
	protected Object body; 
	
	@JSONField(serialize=false)
	private boolean bodyAsRawString = false;
	
	@JSONField(serialize=false)
	private Object context;
	
	//URL parser helper
	@JSONField(serialize=false)
	private UrlInfo urlInfo;
	
	@JSONField(serialize=false)
	private Map<String, String> cookies; 
	
	@JSONField(serialize=false)
	private List<Map<String, String>> responseCookies = new ArrayList<Map<String, String>>(); 
	
	@JSONField(serialize=false)
	private int serverPort;
	
	@JSONField(serialize=false)
	private String pathMatched;
	
	@JSONField(serialize=false)
	private Map<String, String> pathParams;

	@JSONField(serialize=false)
	private Function<Void, Void> closeFn;
	
	public Message() {
		
	}
	
	public Message(Message msg) {
		replace(msg);
		this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); //copy headers 
		this.headers.putAll(msg.headers);
	}

	public void setCloseFn(Function<Void, Void> closeFn) {
		this.closeFn = closeFn;
	}

	public void close() {
		if (this.closeFn != null) {
			this.closeFn.apply(null);
		}
	}

	public void replace(Message msg) {
		this.url = msg.url;
		this.method = msg.method;
		this.status = msg.status; 
		this.headers = msg.headers;
		this.body = msg.body; 
		this.bodyAsRawString = msg.bodyAsRawString;
		this.pathParams = msg.pathParams;
		this.serverPort = msg.serverPort;
		this.pathMatched = msg.pathMatched;
	}  
	
	public String getUrl(){
		if(urlInfo == null || urlInfo.urlPath == null) return this.url;// 当 url=/ 时 urlInfo != null 会存在问题 by vivi
		return String.format("%s?%s", urlInfo.urlPath, getQueryString()); 
	} 
	
	public void setUrl(String url) {
		this.url = url;   
		this.urlInfo = null;
	} 
	
	public void setStatus(Integer status) { 
		this.status = status; 
	} 
	
	public Integer getStatus(){
		return status;
	}  
	
	public String getMethod(){
		return this.method;
	}
	
	public void setMethod(String method){
		this.method = method;
	} 
	
	public Map<String, Object> getHeaders() {
		return headers;
	}  
	
	public void setHeaders(Map<String, Object> headers) {
		this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); //copy headers 
		this.headers.putAll(headers); 
		cookies = null; //clear cookie to recalculate
	} 
	
	public String getHeader(String key){
		Object value = this.headers.get(key);
		if(value == null) return null;
		if(value instanceof String) return (String)value;  
		
		return value.toString();
	}
	
	public Object getHeaderObject(String key){
		return this.headers.get(key);
	}
	 
	public Integer getHeaderInt(String key){
		Object value = getHeaderObject(key);
		if(value == null) return null;
		if(value instanceof Integer) return (Integer) value;
		return Integer.valueOf(value.toString());
	}  
	
	public Long getHeaderLong(String key){
		Object value = getHeaderObject(key);
		if(value == null) return null;
		if(value instanceof Long) return (Long) value;
		return Long.valueOf(value.toString());
	} 
	
	public Boolean getHeaderBool(String key){
		Object value = getHeaderObject(key);
		if(value == null) return null;
		if(value instanceof Boolean) return (Boolean) value;
		return Boolean.valueOf(value.toString());
	} 
	
	public void setHeader(String key, Object value){
		if(value == null) return;
		if(key.toLowerCase().equals("cookie")) {
			cookies = null; //invalidate cookie cache
		}
		if(key.toLowerCase().equals("set-cookie")) {
			responseCookies = null; //invalidate response cookie cache
		}
		this.headers.put(key, value);
	}  
	
	public String removeHeader(String key){
		Object exists = removeHeaderObject(key);
		if(exists == null) return null;  
		
		if(exists instanceof String) return (String)exists; 
		return exists.toString();
	}
	
	public Object removeHeaderObject(String key){
		if(key.toLowerCase().equals("cookie")) {
			cookies = null; //invalidate cookie cache
		}
		if(key.toLowerCase().equals("set-cookie")) {
			responseCookies = null; //invalidate response cookie cache
		}
		
		return this.headers.remove(key);
	}
	
	public Object getBody() {
		return body;
	}  
	
	public void setBody(Object body) { 
		this.body = body; 
	}   
	
	public void setBodyString(String body) { 
		this.body = body; 
		this.bodyAsRawString = true;
	}   
	 
	
	public Object getParam(String key) {
		Map<String, Object> p = params();
		return p.get(key); 
	} 
	
	public <T> T getParam(String key, Class<T> clazz){ 
		Object value = getParam(key);
		if(value == null) return null;
		return JsonKit.convert(value, clazz);
	} 
	
	public void setParam(String key, String value) {
		Map<String, Object> m = params();
		m.put(key, value); 
		calculateUrl();
	}
	
	public void setParam(String key, List<String> values) {
		Map<String, Object> m = params();
		m.put(key, values);
		calculateUrl();
	}
	
	public void setParam(String key, String[] values) {
		List<String> valueList = new ArrayList<>();
		for(String v : values) valueList.add(v); 
		setParam(key, valueList);
	}
	
	@JSONField(deserialize=false, serialize=false)
	public void setParam(String key) {
		setParam(key, (String)null);
	}
	
	@JSONField(deserialize=false, serialize=false)
	public void setPathParams(Map<String, String> pathParams) {
		this.pathParams = pathParams;
	}
	@JSONField(deserialize=false, serialize=false)
	public Map<String, String> getPathParams() {
		return this.pathParams;
	}
	
	private void calculateUrl() {
		if(urlInfo == null) return;
		url = String.format("%s?%s", urlInfo.urlPath, getQueryString());
	}
	
	@JSONField(deserialize=false, serialize=false)
	public String getQueryString() { 
		if(urlInfo == null) {
			urlInfo = HttpKit.parseUrl(url); 
			return urlInfo.queryParamString;
		}  
		
		List<String> queryParts = new ArrayList<>();
		for(Entry<String, Object> e : urlInfo.queryParamMap.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			if(val == null) {
				queryParts.add(key);
				continue;
			}
			if(val instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>)val;
				for(Object item : list) {
					queryParts.add(key+"="+item.toString());
				}
			} else {
				queryParts.add(key+"="+val);
			}
		}
		return String.join("&", queryParts);
	}   
	
	@JSONField(deserialize=false, serialize=false)
	public Map<String, Object> getParams() {  
		return new HashMap<>(params());
	} 
	
	@JSONField(deserialize=false, serialize=false)
	public void setParams(Map<String, Object> params) {  
		if(urlInfo == null) { 
			urlInfo = HttpKit.parseUrl(url); //null support 
		}  
		urlInfo.queryParamMap = new HashMap<>(params);
		calculateUrl();
	} 
	
	private Map<String, Object> params(){
		if(urlInfo == null) { 
			urlInfo = HttpKit.parseUrl(url); //null support 
		}  
		return urlInfo.queryParamMap;
	}
	
	public String getCookie(String key) {
		Map<String, String> m = cookies();
		return m.get(key); 
	} 
	public void setCookie(String key, String value) {
		Map<String, String> m = cookies();
		m.put(key, value);  
		calculateCookieHeader();
	}
	public void setRequestCookie(String key, String value) {
		this.setCookie(key, value);
	}
	public void setResponseCookie(String name, String value) {
		this.setResponseCookie(name, value, null, null, (Long)null, null, null, null);
	}
	public void setResponseCookie(String name, String value, Boolean httpOnly) {
		this.setResponseCookie(name, value, null, null, (Long)null, null, null, httpOnly);
	}
	public void setResponseCookie(String name, String value, Boolean secure, Boolean httpOnly) {
		this.setResponseCookie(name, value, null, null, (Long)null, null, secure, httpOnly);
	}
	
	public void setResponseCookie(
		String name, 
		String value,
		String domain,
		String path,
		Long expires, 
		Integer maxAgeSeconds, 
		Boolean secure, 
		Boolean httpOnly
	) {
		this.setResponseCookie(
			name, value, domain, path, 
			expires != null && expires >= 0 ? new Date(expires) : null, 
			maxAgeSeconds, secure, httpOnly);
	}
	public void setResponseCookie(
		String name, 
		String value,
		String domain,
		String path,
		Date expires, 
		Integer maxAgeSeconds, 
		Boolean secure, 
		Boolean httpOnly
	) {
		List<Map<String, String>> cookies = responseCookies();
		Map<String, String> m = new HashMap<String, String>();
		cookies.add(m);
		
		m.put("Name", name);
		m.put("Value", value);
		
		if (domain != null) {
			m.put("Domain", domain);
		}
		if (path == null) {
			path = "/";
		}
		m.put("Path", path);
		
		if (expires != null) {
			DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US);
			df.setTimeZone(TimeZone.getTimeZone("GMT"));
			String gmtStr = df.format(expires);
			m.put("Expires", gmtStr);
		}
		if (maxAgeSeconds != null) {
			m.put("Max-Age", String.valueOf(maxAgeSeconds));
		}
		if (secure != null) {
			m.put("Secure", String.valueOf(secure));
		}
		if (httpOnly != null) {
			m.put("HttpOnly", String.valueOf(httpOnly));
		}
		calculateResponseCookieHeader();
	}
	
	private void calculateCookieHeader() {
		if(cookies == null) return;
		List<String> cookieList = new ArrayList<>();
		for(Entry<String, String> e : cookies.entrySet()) {
			String key = e.getKey();
			String val = e.getValue();
			cookieList.add(String.format("%s=%s", key, val));
		}
		
		String cookieString = String.join("; ", cookieList);
		setHeader("Cookie", cookieString);
	}
	
	private void calculateResponseCookieHeader() {
		if(responseCookies == null) return;
		
		List<String> setCookies = new ArrayList<>();
		for (Map<String, String> responseCookie : responseCookies) {
			String Name = responseCookie.get("Name");
			if (Name == null || Name.trim().length() == 0) {
				return;
			}
			String Value = responseCookie.get("Value");
			String Expires = responseCookie.get("Expires");
			String MaxAge = responseCookie.get("Max-Age");
			String Domain = responseCookie.get("Domain");
			String Path = responseCookie.get("Path");
			String Secure = responseCookie.get("Secure");
			String HttpOnly = responseCookie.get("HttpOnly");
	//		String SameSite = responseCookie.get("SameSite");
			
			List<String> cookieAttrs = new ArrayList<>();
			cookieAttrs.add(Name+"="+Value);
			if (Expires != null) {
				cookieAttrs.add("Expires="+Expires);
			}
			if (MaxAge != null) {
				cookieAttrs.add("Max-Age="+MaxAge);
			}
			if (Domain != null) {
				cookieAttrs.add("Domain="+Domain);
			}
			if (Path != null) {
				cookieAttrs.add("Path="+Path);
			}
			if ("true".equals(Secure))	 {
				cookieAttrs.add("Secure");
			}
			if ("true".equals(HttpOnly))	 {
				cookieAttrs.add("HttpOnly");
			}
			
			String cookieString = String.join("; ", cookieAttrs);
			setCookies.add(cookieString);
		}
			
		setHeader("Set-Cookie", setCookies);
	}
	
	@JSONField(deserialize=false, serialize=false)
	public void setCookies(Map<String, String> cookies) {
		this.cookies = cookies;
		calculateCookieHeader();
	}
	
	@JSONField(deserialize=false, serialize=false)
	public void setResponseCookie(Map<String, String> cookie) {
		this.responseCookies.add(cookie);
		calculateResponseCookieHeader();
	}
	
	@JSONField(deserialize=false, serialize=false)
	public Map<String, String> getCookies() {
		return new HashMap<>(cookies()); 
    } 
	@JSONField(deserialize=false, serialize=false)
	public List<Map<String, String>> getResponseCookies() {
		return responseCookies(); 
    } 
	
	private Map<String, String> cookies(){
		if(cookies != null) return cookies;
		
		String cookieString = getHeader("cookie");
		cookies = new HashMap<>();
        if (StrKit.isEmpty(cookieString)) {
            return cookies;
        } 
        String[] cookieStrings = cookieString.split(";"); 
        for (String cookie : cookieStrings) {
            if (StrKit.isEmpty(cookie)) {
                continue;
            } 
            int idx = cookie.indexOf("=");
            String key = cookie.substring(0, idx);
            String value = cookie.substring(idx+1);
            if(key != null) key = key.trim();
            if(value != null) value = value.trim();
            cookies.put(key, value); 
        } 
        return cookies;
	} 
	
	@SuppressWarnings("unchecked")
	private List<Map<String, String>> responseCookies(){
		if(responseCookies != null) return responseCookies;
		
		responseCookies = new ArrayList<Map<String, String>>();
		Collection<String> cookieList = null;
		Object setCookie = this.getHeaderObject("set-cookie");
		if (setCookie == null) {
            return responseCookies;
        } 
		if (!(setCookie instanceof Collection)) {
			cookieList = new ArrayList<String>();
			String cookieStr = String.valueOf(setCookie);
			if (StrKit.isEmpty(cookieStr)) {
				return responseCookies;
			}
			cookieList.add(cookieStr);
		} else {
			cookieList = (Collection<String>)setCookie;
		}
		for (String cookieString : cookieList) {
	        Map<String, String> responseCookie = new HashMap<String, String>();
	        responseCookies.add(responseCookie);
	        
	        String[] cookieAttrs = cookieString.split(";"); 
	        for (int i = 0; i < cookieAttrs.length; i++) {
	        	String cookieAttr = cookieAttrs[i];
	            if (StrKit.isEmpty(cookieAttr)) {
	                continue;
	            } 
	            int idx = cookieAttr.indexOf("=");
	            String key = cookieAttr.substring(0, idx);
	            String value = cookieAttr.substring(idx+1);
	            if(key != null) key = key.trim();
	            if(value != null) value = value.trim();
	            if (i == 0) {
	            	responseCookie.put("Name", key);
	            	responseCookie.put("Value", value);
	            } else {
	            	responseCookie.put(key, value);
	            }
	        } 
		}
        return responseCookies;
	} 
	
	@SuppressWarnings("unchecked")  
	public <T> T getContext() {
		return (T)context;
	}
	
	public void setContext(Object context) {
		this.context = context;
	}
	
	@JSONField(deserialize=false, serialize=false)
	public boolean isBodyAsRawString() {
		return bodyAsRawString;
	}
	
	public void setServerPort(int port) {
		this.serverPort = port;
	}
	
	public int getServerPort() {
		return this.serverPort;
	}
	
	public void setPathMatched(String pathMatched) {
		this.pathMatched = pathMatched;
	}
	public String getPathMatched() {
		return this.pathMatched;
	}
	
	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		json.put("status", this.status);
		json.put("url", this.url);
		json.put("method", this.method);
		json.put("headers", this.headers);
		
		Object body = null;
		final int maxBodyLengthPrint = 10240;
		if(this.body instanceof String) {
			String s = (String)this.body;
			body = s;
			if(s.length() > maxBodyLengthPrint) {
				body = s.substring(0, maxBodyLengthPrint) + " ...";
			}
		} else if(this.body instanceof byte[]){
			body = "<binary data>";
		} else {
			String s = JsonKit.toJSONString(this.body);
			if(s.length() < maxBodyLengthPrint) {
				body = this.body;
			} else {
				body = "<json object too large> " + s.substring(0, maxBodyLengthPrint) + " ....";
			}
		}
		json.put("body", body);
		
		return JSON.toJSONString(json, true);
	}
	
	public String toJSONString() {
		return toJSONString(false);
	}
	
	public String toJSONString(boolean prettyFormat) {
		JSONObject json = new JSONObject();
		json.put("status", this.status);
		json.put("url", this.url);
		json.put("method", this.method);
		json.put("headers", this.headers); 
		json.put("body", this.body);
		 
		return JsonKit.toJSONString(json, prettyFormat);
	} 
	
	@JSONField(serialize=false, deserialize=false)
	public Map<String, Object> getMergedParams() {
		Map<String, Object> params = new HashMap<String, Object>();
		
		// Headers
		Map<String, Object> headerParams = this.getHeaders();
		// Body(JSONObject、FormData(Attrs, Files)、QueryString)
		Object body = this.getBody();
		Map<String, Object> bodyParams = new HashMap<>();
		if (body instanceof FormData) {// Body FormData、QueryString
			FormData fd = (FormData) body;
			if (fd.attributes != null) {
				bodyParams.putAll(fd.attributes);
			}
			if (fd.files != null) {
				bodyParams.putAll(fd.files);
			}
			body = bodyParams;
		} else {// Body JSONObject
			try {
				JSONObject bodyMap = JsonKit.convert(body, JSONObject.class);
				if (bodyMap != null) {
					bodyParams.putAll(bodyMap);
					body = bodyMap;
				}
			} catch (Throwable e) {
				// ignore
				try {
					JSONArray bodyList = JsonKit.convert(body, JSONArray.class);
					if (bodyList != null) {
						body = bodyList;
					}
				} catch (Throwable e2) {
					// ignore
				}
			}
		}
		// Url(QueryString、PathParam)
		UrlInfo urlInfo = HttpKit.parseUrl(this.getUrl());
		Map<String, Object> queryParams = new HashMap<>();
		if (urlInfo != null && urlInfo.queryParamMap != null) {
			queryParams.putAll(urlInfo.queryParamMap);
		}
		Map<String, String> pathParams = this.getPathParams();
		// Cookies
		Map<String, String> cookieParams = this.getCookies();
		
		// 计算params
		
		// params 包括以下这些（相同参数名的时候，后面可以覆盖前面）
		if (headerParams != null)
			params.putAll(headerParams);// Headers
		if (bodyParams != null && !bodyParams.isEmpty()) {
			params.putAll(bodyParams);// Body Map
		}
		if (body != null) {
			params.put("body", body);// Body Object ( Map, List, String, etc. )
		}
		if (queryParams != null)
			params.putAll(queryParams);// Url QueryString
		if (pathParams != null)
			params.putAll(pathParams);// Url PathVariable
		if (cookieParams != null)
			params.putAll(cookieParams);// Cookies (安全性要求更高，所以它的优先级最高)
		
		return params;
	}
	
}