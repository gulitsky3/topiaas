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
 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
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
	private Map<String, String> responseCookies; 
	
	@JSONField(serialize=false)
	private int serverPort;
	
	@JSONField(serialize=false)
	private String pathMatched;
	
	@JSONField(serialize=false)
	private Map<String, String> pathParams;
	
	public Message() {
		
	}
	
	public Message(Message msg) {
		replace(msg);
		this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); //copy headers 
		this.headers.putAll(msg.headers);
	}
	
	public void replace(Message msg) {
		this.url = msg.url;
		this.method = msg.method;
		this.status = msg.status; 
		this.headers = msg.headers;
		this.body = msg.body; 
		this.bodyAsRawString = msg.bodyAsRawString;
		this.pathParams = msg.pathParams;
	}  
	
	public String getUrl(){
		if(urlInfo == null) return this.url;
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
	public void setResponseCookie(String key, String value) {
		Map<String, String> m = responseCookies();
		m.put(key, value);  
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
		List<String> cookieList = new ArrayList<>();
		for(Entry<String, String> e : responseCookies.entrySet()) {
			String key = e.getKey();
			String val = e.getValue();
			cookieList.add(String.format("%s=%s", key, val));
		}
		
		String cookieString = String.join("; ", cookieList);
		setHeader("Set-Cookie", cookieString);
	}
	
	
	@JSONField(deserialize=false, serialize=false)
	public void setCookies(Map<String, String> cookies) {
		this.cookies = cookies;
		calculateCookieHeader();
	}
	
	@JSONField(deserialize=false, serialize=false)
	public void setResponseCookies(Map<String, String> cookies) {
		this.responseCookies = cookies;
		calculateResponseCookieHeader();
	}
	
	@JSONField(deserialize=false, serialize=false)
	public Map<String, String> getCookies() {
		return new HashMap<>(cookies()); 
    } 
	@JSONField(deserialize=false, serialize=false)
	public Map<String, String> getResponseCookies() {
		return new HashMap<>(responseCookies()); 
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
	
	private Map<String, String> responseCookies(){
		if(responseCookies != null) return responseCookies;
		
		String cookieString = getHeader("set-cookie");
		responseCookies = new HashMap<>();
        if (StrKit.isEmpty(cookieString)) {
            return responseCookies;
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
            responseCookies.put(key, value); 
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
}