package io.zbus.rpc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsKit;
import io.zbus.rpc.InvocationContext;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message; 
 
@Route(exclude = true)
public class JavascriptInvoker {
	private static final Logger logger = LoggerFactory.getLogger(JavascriptInvoker.class);
	
	ScriptEngineManager factory = new ScriptEngineManager();
	ScriptEngine engine = factory.getEngineByName("javascript"); 
	
	private Map<String, Object> context = new HashMap<>(); 
	private FileKit fileKit = new FileKit(false);
	private String basePath = "."; 
	private String urlPrefix = ""; 
	private String initJsFile = null;
	 
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
	
	public void init() {
		if(initJsFile == null) return; 
	}
	
	@Route("/cache")
	public void cache(boolean cacheEnabled) {
		fileKit.setCacheEnabled(cacheEnabled);
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
		if(info.pathList.size() < 2) {
			Message res = new Message();
			res.setStatus(400);
			res.setBody("Missing function name");
			return res;
		}
		
		
		urlFile = "";
		for(int i=0;i<info.pathList.size()-1;i++) {
			urlFile += info.pathList.get(i) + File.separator; 
		}
		urlFile = urlFile.substring(0, urlFile.length()-1); 
		if(!urlFile.endsWith(".js")) {
			urlFile += ".js";
		}
		File fullPathFile = new File(absoluteBasePath, urlFile); 
		final String method = info.pathList.get(info.pathList.size()-1);
		
		String file = fullPathFile.getPath(); 
		String js = null;
		try {
			js = new String(fileKit.loadFileBytes(file));  
		} catch (FileNotFoundException e) {  
			Message res = new Message();
			res.setStatus(404);
			res.setBody(urlFile + " Not Found");
			logger.info(file + " Not Found");
			return res;
		}
		
		engine.eval(js);
		Invocable inv = (Invocable) engine;
		Map<String, Object> ctx = new HashMap<>(context);
		if(context.containsKey("request")) {
			logger.debug("java context.request override, should change java context.request name.");
		}
		if(context.containsKey("response")) {
			logger.debug("java context.response override, should change java context.response name.");
		}
		
		ctx.put("request", InvocationContext.getRequest());
		ctx.put("response", InvocationContext.getResponse());
		final Object res = inv.invokeFunction(
				method,
				ctx
			); 
		
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
	
	public void setInitJsFile(String initJsFile) {
		this.initJsFile = initJsFile;
	}
} 