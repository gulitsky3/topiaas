package io.zbus.rpc;

import java.io.IOException;

import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;

public class StaticResource {
	private String basePath = ".";
	private FileKit fileKit = new FileKit();
	
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
	public void setCacheEnabled(boolean cacheEnabled) {
		this.fileKit.setCache(cacheEnabled);
	}
	@RequestMapping("/")
	public Message file(Message req) {
		Message res = new Message();
		
		UrlInfo info = HttpKit.parseUrl(req.getUrl());
		String file = basePath + info.urlPath;
		String contentType = HttpKit.contentType(file);
		if(contentType == null) {
			contentType = "application/octet-stream";
		}
		
		res.setHeader(Http.CONTENT_TYPE, contentType);   
		res.setStatus(200); 
		try {
			byte[] data = fileKit.loadFileBytes(file);
			res.setBody(data);
		} catch (IOException e) {
			res.setStatus(404);
			res.setBody(info.urlPath + " Not Found");
		}  
		return res;
	}
}
