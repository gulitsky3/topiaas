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
 

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private Object context;
	
	//URL parser helper
	@JSONField(serialize=false)
	private UrlInfo urlInfo;
	
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
	}  
	
	public String getUrl(){
		return this.url;
	} 
	
	public void setUrl(String url) {
		this.url = url;  
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
		this.headers.put(key, value);
	}  
	
	public String removeHeader(String key){
		Object exists = removeHeaderObject(key);
		if(exists == null) return null; 
		if(exists instanceof String) return (String)exists;
		
		return exists.toString();
	}
	
	public Object removeHeaderObject(String key){
		return this.headers.remove(key);
	}
	
	public Object getBody() {
		return body;
	}  
	
	public void setBody(Object body) { 
		this.body = body; 
	}  
	
	/**
	 * Get URL query string param
	 * @param key
	 * @return
	 */
	public String getParam(String key) {
		if(url == null) return null;
		if(urlInfo == null) {
			urlInfo = HttpKit.parseUrl(url);
		}
		String value = urlInfo.queryParamMap.get(key);
		return value;
	}
	
	public String param(String key) {
		return getParam(key);
	}
	
	public List<String> paramArray(String key) {
		return getParamArray(key);
	}
	
	public String queryString() {
		if(url == null) return null;
		if(urlInfo == null) {
			urlInfo = HttpKit.parseUrl(url);
		}
		return urlInfo.queryParamString;
	}
	
	public Map<String, Object> cookies() {
		String cookieString = getHeader("cookie");
        Map<String, Object> ret = new HashMap<>();
        if (StrKit.isEmpty(cookieString)) {
            return ret;
        } 
        String[] cookies = cookieString.split(";"); 
        for (String cookie : cookies) {
            if (StrKit.isEmpty(cookie)) {
                continue;
            } 
            String[] kv = cookie.split("="); 
            if (kv.length < 2) {
                continue;
            }  
            ret.put(kv[0].trim(), kv[1].trim()); 
        } 
        return ret;
    }
	
	public List<String> getParamArray(String key) {
		if(url == null) return new ArrayList<>();
		if(urlInfo == null) {
			urlInfo = HttpKit.parseUrl(url);
		}
		return StrKit.getArrayValue(urlInfo.queryParamString, key); 
	}
	
	public <T> T getParam(String key, Class<T> clazz){ 
		String value = getParam(key);
		if(value == null) return null;
		return JsonKit.convert(value, clazz);
	}
	
	@SuppressWarnings("unchecked") 
	public <T> T getContext() {
		return (T)context;
	}
	
	public void setContext(Object context) {
		this.context = context;
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
		
		return JSON.toJSONString(json, prettyFormat);
	}
	
	public byte[] toJSONBytes() {
		return toJSONString().getBytes();
	}
	
	public byte[] toJSONBytes(String charset) {
		String s = toJSONString();
		try {
			return s.getBytes(charset);
		} catch (UnsupportedEncodingException e) { 
			return s.getBytes();
		}
	}
}