package io.zbus.rpc;

import java.io.File;
import java.io.IOException;

import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;

public class StaticResource {
	private String basePath = "";
	private FileKit fileKit = new FileKit();
	
	@Route(exclude=true)
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
	@Route(exclude=true)
	public void setCacheEnabled(boolean cacheEnabled) {
		this.fileKit.setCacheEnabled(cacheEnabled);
	}
	
	@Route("/")
	public Message file(Message req) {
		Message res = new Message();
		
		UrlInfo info = HttpKit.parseUrl(req.getUrl());
		String urlFile = info.urlPath;
		if(urlFile == null) { //missing replace with default
			urlFile = "index.html";
		}
		//String file = HttpKit.joinPath(basePath ,urlFile); //TODO security issue
		File fullPath = new File(basePath, urlFile);
		String file = fullPath.getPath();
		 
		String contentType = HttpKit.contentType(file);
		if(contentType == null) {
			contentType = "application/octet-stream";
		}
		
		res.setHeader(Http.CONTENT_TYPE, contentType);   
		res.setStatus(200); 
		try {
			byte[] data = fileKit.loadFileBytes(file);
			if(HttpKit.isText(contentType)) {
				res.setBody(new String(data, "utf8")); //TODO
			} else {
				res.setBody(data);
			}
		} catch (IOException e) {
			res.setStatus(404);
			res.setHeader(Http.CONTENT_TYPE, "text/plain; charset=utf8");
			res.setBody(urlFile + " Not Found");
		}  
		return res;
	}
}
