package io.zbus.rpc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsKit;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;

@Route(exclude = true)
public class JavascriptInvoker {
	ScriptEngineManager factory = new ScriptEngineManager();
	ScriptEngine engine = factory.getEngineByName("nashorn");
	
	private Map<String, Object> context = new HashMap<>(); 
	private FileKit fileKit = new FileKit(false);
	private String basePath = "."; 
	private String urlPrefix = "";
	 
	private File absoluteBasePath = new File(basePath).getAbsoluteFile();  
	public void setBasePath(String basePath) {
		if(basePath == null) {
			basePath = ".";
		}
		this.basePath = basePath; 
		File file = new File(this.basePath);
		if(file.isAbsolute()) {
			absoluteBasePath = file;
		} else {
			absoluteBasePath = new File(System.getProperty("user.dir"), basePath);
		}
	} 

	@Route("/")
	public Object invoke(Message req) throws Exception { 
		String url = req.getUrl();
		if(url.startsWith(this.urlPrefix)) {
			url = url.substring(this.urlPrefix.length());
		}
		UrlInfo info = HttpKit.parseUrl(url);
		String urlFile = info.urlPath;
		if(urlFile == null) { 
			Message res = new Message();
			res.setStatus(400);
			res.setBody("Missing function path");
			return res;
		}  
		if(!urlFile.endsWith(".js")) {
			urlFile += ".js";
		}
		File fullPath = new File(absoluteBasePath, urlFile);
		String file = fullPath.getPath();  
		String js = null;
		try {
			js = new String(fileKit.loadFileBytes(file));  
		} catch (FileNotFoundException e) {
			Message res = new Message();
			res.setStatus(404);
			res.setBody(urlFile + " Not Found");
			return res;
		}
		
		engine.eval(js);
		Invocable inv = (Invocable) engine;
		Object res = inv.invokeFunction("main", context, InvocationContext.getRequest(),
				InvocationContext.getResponse());
		return JsKit.convert(res);
	}

	public void setCacheEnabled(boolean cacheEnabed) {
		fileKit.setCacheEnabled(cacheEnabed);
	} 

	public void setContext(Map<String, Object> context) {
		this.context = context;
	}
	
	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}
} 