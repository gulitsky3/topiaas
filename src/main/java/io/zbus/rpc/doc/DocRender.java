package io.zbus.rpc.doc;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.rpc.RpcMethod;
import io.zbus.rpc.RpcMethod.MethodParam;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.annotation.Route;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;

@Route(exclude=true) //Exclude by default
public class DocRender { 
	private FileKit fileKit = new FileKit();
	private final RpcProcessor rpcProcessor;   
	private String rootUrl;
	private String docFile = "static/rpc.html";
	private boolean embbedJavascript = true;
	
	public DocRender(RpcProcessor rpcProcessor) {
		this.rpcProcessor = rpcProcessor;  
	}   
	 
	public void setRootUrl(String rootUrl) {
		this.rootUrl = rootUrl;
	}
	
	@Route(path="/apiList", docEnabled=false)
	public List<RpcMethod> apiList() {  
		return this.rpcProcessor.rpcMethodList();
	} 
	
	@Route(path="/", docEnabled=false)
	public Message index() throws IOException { 
		Message result = new Message(); 
		Map<String, Object> model = new HashMap<String, Object>(); 
		
		List<RpcMethod> methods = this.rpcProcessor.rpcMethodList();
		String doc = "<div>";
		int rowIdx = 0;  
		for(RpcMethod m : methods) {
			if(!m.docEnabled) continue;
			doc += rowDoc(m, rowIdx++);
		}
		doc += "</div>";
		String js = "";
		if(embbedJavascript) {
			js = fileKit.loadFile("static/zbus.min.js");
		}
		model.put("content", doc); 
		model.put("zbusjs", js);  
		String urlPrefix = this.rootUrl;
		if(urlPrefix.endsWith("/")) {
			urlPrefix = urlPrefix.substring(0, urlPrefix.length()-1);
		}
		model.put("urlPrefix", urlPrefix);  
		
		
		String body = fileKit.loadFile(docFile, model);
		result.setBody(body);
		result.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
		return result;
	}
	
	private String rowDoc(RpcMethod m, int idx) {  
		String fmt = 
				"<tr>" +   
				"<td class=\"urlPath\"><a href=\"%s\">%s</a></td>" + 
				"<td class=\"returnType\">%s</td>" +  
				"<td class=\"methodParams\"><code><strong><a href=\"%s\">%s</a></strong>(%s)</code>" +  
				"</td>" + 
				"</tr>"; 
		String methodLink = HttpKit.joinPath(rootUrl, m.getUrlPath()); 
		String method = m.method;
		String paramList = "";
		int size = m.params.size(); 
		for(int i=0;i<size;i++) { 
			MethodParam p = m.params.get(i);
			paramList += p.type.getName(); 
			if(p.name != null)  paramList += " " + p.name;  
			paramList += ", "; 
		} 
		if(paramList.length() > 0) {
			paramList = paramList.substring(0, paramList.length()-2);
		}     
		return String.format(fmt, methodLink, methodLink, m.returnType, methodLink, method, paramList);
	}

	public String getDocFile() {
		return docFile;
	}

	public void setDocFile(String docFile) {
		this.docFile = docFile;
	}  
	
	public void setEmbbedJavascript(boolean embbedJavascript) {
		this.embbedJavascript = embbedJavascript;
	}
}