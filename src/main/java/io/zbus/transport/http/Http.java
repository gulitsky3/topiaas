package io.zbus.transport.http;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zbus.kit.JsonKit;
import io.zbus.transport.Message;

/**
 * 
 * HTTP conversion for <code>Message</code>.
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public class Http { 
	public static final String URL     = "url";
	public static final String STATUS  = "status";
	public static final String BODY    = "body";
	public static final String HEADERS = "headers";
	
	public static final String CONTENT_LENGTH        = "Content-Length";
	public static final String CONTENT_TYPE          = "Content-Type";     
	public static final String CONTENT_TYPE_JSON     = "application/json; charset=utf8"; 
	public static final String CONTENT_TYPE_FORM     = "multipart/form-data"; 
	
	public static byte[] body(Message msg) {
		String contentType = msg.getHeader(CONTENT_TYPE);
		if(contentType != null) contentType = contentType.toLowerCase();
		byte[] body = new byte[0];
		Object bodyObj = msg.getBody();
		String charset = "utf8";
		if(contentType != null) { 
			charset = charset(contentType);
		}  
		
		if(contentType != null && contentType.startsWith("application/json")) {
			if(msg.isBodyAsRawString() && msg.getBody() != null && msg.getBody() instanceof String) { //body is raw string
				body = ((String)msg.getBody()).getBytes(Charset.forName(charset));
			} else {
				body = JsonKit.toJSONBytes(msg.getBody(), charset);
			}
		}  else {
			if(bodyObj != null) {
				if(bodyObj instanceof byte[]) { 
					body = (byte[])bodyObj;
				} else if(bodyObj instanceof String) { 
					body = msg.getBody().toString().getBytes();
				} else { 
					body = JsonKit.toJSONBytes(msg.getBody(), charset);
				}
			}   
		} 
		return body;
	}
	 
	public static String charset(String contentType) {
		String charset="utf-8";
		String[] bb = contentType.split(";"); 
		if(bb.length>1){
			String[] bb2 = bb[1].trim().split("=");
			if(bb2[0].trim().equalsIgnoreCase("charset")){
				charset = bb2[1].trim();
			} 
		} 
		return charset;
	}
	 
	
	   

	public static class FileUpload { 
		public String fileName;
		public String contentType;
		public byte[] data;
	}
	
	public static class FormData {
		public Map<String, Object> attributes = new HashMap<String, Object>();
		public Map<String, List<FileUpload>> files = new HashMap<String, List<FileUpload>>();
		
		public Map<String, Object> getAttributes() {
			return attributes;
		}
		
		public Map<String, List<FileUpload>> getFiles() {
			return files;
		}
		
		@SuppressWarnings("unchecked")
		public void addAttribute(String key, Object value) {
			Object exists = attributes.get(key); 
			if(exists != null) {
				List<Object> values;
				if(exists instanceof List) {
					values = (List<Object>)exists;
				} else{
					values = new ArrayList<>();
					values.add(exists);
				} 
				values.add(value);
				value = values;
			}
			attributes.put(key, value);
		}
	}
}
