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
 

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Message {   
	public static final String ID = "id";
	
	protected String url; 
	protected String method;   
	
	protected Integer status; //null: request, otherwise: response 
	protected String statusText;  
	
	protected Map<String, String> headers = new ConcurrentHashMap<String, String>(); 
	protected Object body;    
	
	public Message() {
		
	}
	
	public Message(Message msg) {
		this.url = msg.url;
		this.method = msg.method;
		this.status = msg.status;
		this.statusText = msg.statusText;
		this.headers = new HashMap<>(msg.headers);
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
	
	public String getStatusText() {
		return statusText;
	}
	
	public void setStatusText(String statusText) {
		this.statusText = statusText;
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
	
	public void addHeader(String key, Object value){
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
}