package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import io.zbus.kit.FileKit;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Route; 
  
@Route(exclude=true)
public class JavascriptInvoker {    
	ScriptEngineManager factory = new ScriptEngineManager();
	ScriptEngine engine = factory.getEngineByName("nashorn"); 
	private String jsBasePath = "js";  
	
	private Map<String, Object> context = new HashMap<>();
	
	private FileKit fileKit = new FileKit(false);
	
	@Route("/") 
	public Object invoke(@Param("funcName") String funcName) throws Exception {   
		if(funcName == null) {
			throw new IllegalArgumentException("funcName required");
		}
		String filePath = String.format("%s/%s.js", jsBasePath, funcName);
		String js = fileKit.loadFile(filePath); 
		
		engine.eval(js); 
		Invocable inv = (Invocable) engine; 
		return inv.invokeFunction("main", context, InvocationContext.getRequest(), InvocationContext.getResponse()); 
	} 
	
	public void setCacheEnabled(boolean cacheEnabed) {
		fileKit.setCacheEnabled(cacheEnabed);
	}
	
	public void setJsBasePath(String jsBasePath) {
		this.jsBasePath = jsBasePath;
	}
	
	public void setContext(Map<String, Object> context) {
		this.context = context;
	}
}
