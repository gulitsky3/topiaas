package io.zbus.rpc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import io.zbus.kit.HttpKit;
import io.zbus.transport.Message;
 
public class Template { 
	private static final Logger logger = LoggerFactory.getLogger(Template.class); 
	private Configuration config; 
	private Map<String, Object> context = new HashMap<>();
	public Template() { 
		this.config = new Configuration(Configuration.VERSION_2_3_28);  
		config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		config.setLogTemplateExceptions(false);
		config.setWrapUncheckedExceptions(true);
	} 
	
	public void setTemplateDir(String dir) {
		try {
			this.config.setDirectoryForTemplateLoading(new File(dir));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public void setDefaultEncoding(String encoding) {
		this.config.setDefaultEncoding(encoding);
	}
	
	public void setContext(Map<String, Object> context) {
		this.context = context;
	}
	
	public void addContext(String key, Object value) {
		this.context.put(key, value);
	}
	
	public void setUrlPrefix(String urlPrefix) {
		this.context.put("urlPrefix", urlPrefix);
	}
	
	public Message render(String templateFile, Map<String, Object> model){
		final String CONTEXT_KEY = "ctx";
		Message message = new Message(); 
		String contentType = HttpKit.contentType(templateFile);
		if(contentType == null) {
			contentType = "text/html";
		}
		String encoding = this.config.getDefaultEncoding();
		contentType += "; charset="+encoding;
		message.setHeader("content-type", contentType); 
        try {
        	freemarker.template.Template t = config.getTemplate(templateFile); 
        	ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Writer out = new OutputStreamWriter(stream);
            Map<String, Object> copyModel = model;
            if(!copyModel.containsKey(CONTEXT_KEY)) {
            	copyModel = new HashMap<>(model);
            	copyModel.put(CONTEXT_KEY, this.context);
            }
			t.process(copyModel, out); 
			message.setBody(stream.toString(encoding));
			out.close();
			stream.close();
			
		} catch (Exception e) { 
			message.setStatus(500);
			message.setBody("<pre>"+e.getMessage()+"</pre>");
		}   
		
		return message;
	} 
}
