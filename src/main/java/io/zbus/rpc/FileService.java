package io.zbus.rpc;

import java.io.IOException;
import java.util.Map;

import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.rpc.annotation.Auth;
import io.zbus.rpc.annotation.Path;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http; 

/**
 * 
 * Static file serving via RPC protocol
 * 
 * <p>moudle: static(configurable)
 * <p>method: file,
 * <p>params: ['a','b','c.js'] ==> a/b/c.js (path split)
 * 
 * 
 * @author leiming.hong
 *
 */
@Auth(exclude=true)
public class FileService {    
	private String basePath = ".";
	private FileKit kit = new FileKit();  
	
	@Path(exclude=true)
	public void setCacheEnabled(boolean cache) {
		kit.setCache(cache);
	}
	
	@Path(exclude=true)
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	} 

	public Message file(Map<String, Object> request) {
		Message res = new Message(); 
		
		Object[] params = (Object[])request.get(Protocol.PARAMS);  
		try {
			String resource = "";
			for(Object param : params) { 
				if(".".equals(param) || "..".equals(param)) { //Prevent path traversal attack
					throw new IllegalArgumentException(resource + " access NOT allowed");
				}
				resource += "/" + param;
			}
			byte[] data = kit.loadFileBytes(basePath + resource);
			if (data == null) {
				res.setBody("404: " + resource + " Not Found"); 
				res.setStatus(404);
			} else {
				res.setStatus(200);
				res.addHeader(Http.CONTENT_TYPE, HttpKit.contentType(resource));
				res.setBody(data);
			}
		} catch (IOException e) {
			res.setStatus(500);
			res.setBody(e.getMessage());
		}
		return res; 
	} 
}

 