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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsonKit;
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
	
	protected Map<String, String> headers = new ConcurrentHashMap<String, String>(); 
	protected Object body;  
	
	private Object context;
	
	//URL parser helper
	private UrlInfo urlInfo;
	
	public Message() {
		
	}
	
	public Message(Message msg) {
		replace(msg);
		this.headers = new HashMap<>(this.headers); //copy headers 
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
	
	public Map<String,String> getHeaders() {
		return headers;
	} 
	
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	} 
	
	public String getHeader(String key){
		return this.headers.get(key);
	}
	
	public Integer getHeaderInt(String key){
		String value = this.headers.get(key);
		if(value == null) return null;
		return Integer.valueOf(value);
	} 
	
	public Long getHeaderLong(String key){
		String value = this.headers.get(key);
		if(value == null) return null;
		return Long.valueOf(value);
	} 
	
	public Boolean getHeaderBool(String key){
		String value = this.headers.get(key);
		if(value == null) return null;
		return Boolean.valueOf(value);
	} 
	
	public void setHeader(String key, Object value){
		if(value == null) return;
		this.headers.put(key, value.toString());
	}  
	
	public String removeHeader(String key){
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
		json.put("body", this.body);
		
		return toJSONString(true);
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