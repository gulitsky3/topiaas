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
package io.zbus.kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpKit {
	private final static Map<String, String> MIME_TYPES = new HashMap<>(); 
	
	public static class UrlInfo { 
		public List<String> pathList = new ArrayList<String>();
		public Map<String, Object> queryParamMap = new HashMap<String, Object>(); 
		
		public String urlPath;
		public String queryParamString; 
	} 
	
	/**
	 * match pattern only /abc/* or /abc, /abc/
	 * @param url
	 * @param pattern
	 * @return
	 */
	public static boolean urlMatched(String url, String pattern) {
		boolean matched = false;
		if(pattern.endsWith("*")) {
			pattern = pattern.substring(0, pattern.length()-1);
			matched = url.startsWith(pattern);
			if(matched) return true;
			
			if(pattern.endsWith("/")) {
				if(pattern.equals(url+"/")) return true; // ignore last /
			}
			return false; 
		} else { //full match
			if(pattern.endsWith("/")) {
				if(pattern.equals(url+"/")) return true; // ignore last / 
			} else {
				if(url.equals(pattern+"/")) return true;
			}
			
			return url.equals(pattern);
		} 
	}
	
	public static String joinPath(String... paths) {
		String url = "/";
		for(String p : paths) {
			if(p == null) continue;
			url += "/"+p;
		}   
		url = url.replaceAll("[/]+", "/"); 
        if(url.endsWith("/") && url.length()>1) {
        	url = url.substring(0, url.length()-1);
        } 
		return url;
	}
	
	public static UrlInfo parseUrl(String url){
		UrlInfo info = new UrlInfo();
		if("/".equals(url) || StrKit.isEmpty(url)){
			info.urlPath = url;
			return info;
		} 
    	String path = url;
    	String params = null;
    	int idx = url.indexOf('?');
    	info.urlPath =  url;
    	if(idx >= 0){
    		info.urlPath = path = url.substring(0, idx);  
    		info.queryParamString = params = url.substring(idx+1);
    	} 
    	String[] bb = path.split("/");
    	for(String b : bb){
    		if(StrKit.isEmpty(b)) continue;
    		info.pathList.add(b);
    	}
    	info.urlPath = "/"+String.join("/", info.pathList);
    	if(params != null){
    		info.queryParamMap = StrKit.keyValueWithArray(params, "&");
    	} 
		return info;
	}
	
	public static String contentType(String resource){
		int idx = resource.lastIndexOf('.');
		if(idx < 0) {
			return null;
		}
		String mimeType = resource.substring(idx+1).toLowerCase();
		return MIME_TYPES.get(mimeType); 
	}
	
	public static boolean isText(String contentType) { 
		if(contentType == null) return false; 
		if(contentType.startsWith("text")) return true;
		if(contentType.startsWith("application/json")) return true;
		if(contentType.startsWith("application/javascript")) return true;
		if(contentType.startsWith("application/xml")) return true;
		return false;
	}
	 
	static { 
		MIME_TYPES.put("js", "application/javascript"); 
		MIME_TYPES.put("json", "application/json"); 
		MIME_TYPES.put("xml", "application/xml"); 
		MIME_TYPES.put("css", "text/css"); 
		MIME_TYPES.put("htm", "text/html"); 
		MIME_TYPES.put("html", "text/html"); 
		
		MIME_TYPES.put("svg", "image/svg+xml"); 
		MIME_TYPES.put("gif", "image/gif"); 
		MIME_TYPES.put("jpeg", "image/jpeg"); 
		MIME_TYPES.put("jpg", "image/jpg"); 
		MIME_TYPES.put("ico", "image/x-icon"); 
		MIME_TYPES.put("png", "image/png"); 
		MIME_TYPES.put("pdf", "application/pdf");   
		MIME_TYPES.put("zip", "application/zip");   
		MIME_TYPES.put("ttf", "application/x-font-ttf");  
		MIME_TYPES.put("eot", "font/opentype");   
	}
	 
}
