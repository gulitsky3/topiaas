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
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http; 


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

	@Route(path="/", ignoreResult=true)
	public void entry() throws Exception {  
		callJs("filter.js", "doFilter"); 
	}
	
	public void invoke() throws Exception { 
		Message req = InvocationContext.getRequest();
		Message res = InvocationContext.getResponse();
		
		String url = req.getUrl();
		if(url.startsWith(this.urlPrefix)) {
			url = url.substring(this.urlPrefix.length());
		}
		UrlInfo info = HttpKit.parseUrl(url);
		String urlFile = info.urlPath;
		if(urlFile == null) {  
			res.setStatus(400);
			res.setBody("Missing function path");
			return;
		}    
		if(info.pathList.size() < 2) { 
			res.setStatus(400);
			res.setBody("Missing function name");
			return;
		} 
		
		urlFile = "";
		for(int i=0;i<info.pathList.size()-1;i++) {
			urlFile += info.pathList.get(i) + File.separator; 
		}
		urlFile = urlFile.substring(0, urlFile.length()-1); 
		if(!urlFile.endsWith(".js")) {
			urlFile += ".js";
		} 
		final String method = info.pathList.get(info.pathList.size()-1); 
		callJs(urlFile, method);
	}
	
	private void loadScript(String jsFile) throws Exception {   
		File fullPathFile = new File(absoluteBasePath, jsFile);   
		String file = fullPathFile.getPath(); 
		String js = new String(fileKit.loadFileBytes(file)); 
		engine.eval(js);
	} 
	
	public void callJs(String jsFile, String jsFunc) throws Exception { 
		Message req = InvocationContext.getRequest();
		Message res = InvocationContext.getResponse();  
		try {
			loadScript(jsFile);
		} catch (FileNotFoundException e) {   
			res.setStatus(404);
			res.setBody(jsFile + " Not Found");
			logger.info(jsFile + " Not Found");
			return;
		} 
		
		Invocable inv = (Invocable) engine;
		Map<String, Object> ctx = new HashMap<>(context);
		if(context.containsKey("request")) {
			logger.debug("java context.request override, should change java context.request name.");
		}
		if(context.containsKey("response")) {
			logger.debug("java context.response override, should change java context.response name.");
		}
		
		ctx.put("request", req);
		ctx.put("response", res);
		final Object jsRes = inv.invokeFunction(
				jsFunc,
				ctx,
				this
			);  
		if(jsRes != null) {
			if(jsRes instanceof Message) {
				res.replace((Message)jsRes);
			} else {
				if(res.getHeader(Http.CONTENT_TYPE) == null) {
					res.setHeader(Http.CONTENT_TYPE, "application/json; charset=utf8");  //default to json
				}
				res.setBody(JsKit.convert(jsRes));
			}
		}
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