package io.zbus.rpc.doc;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zbus.kit.FileKit;
import io.zbus.rpc.RpcMethod;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.annotation.Path;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;

public class DocRender { 
	private FileKit fileKit = new FileKit();
	private final RpcProcessor rpcProcessor;  
	private final String docRootPath;
	public DocRender(RpcProcessor rpcProcessor, String docRootPath) {
		this.rpcProcessor = rpcProcessor; 
		this.docRootPath = docRootPath;
	}

	@Path("/")
	public Message index() throws IOException { 
		Message result = new Message(); 
		Map<String, Object> model = new HashMap<String, Object>(); 
		
		List<RpcMethod> methods = this.rpcProcessor.rpcMethodList();
		String doc = "<div>";
		int rowIdx = 0;  
		for(RpcMethod m : methods) {
			doc += rowDoc(m, rowIdx++);
		}
		doc += "</div>";
		model.put("content", doc); 
		String urlPrefix = docRootPath;
		if(urlPrefix.endsWith("/")) {
			urlPrefix = urlPrefix.substring(0, urlPrefix.length()-1);
		}
		model.put("urlPrefix", urlPrefix);
		
		String body = fileKit.loadFile("rpc.htm", model);
		result.setBody(body);
		result.addHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
		return result;
	}
	
	private String rowDoc(RpcMethod m, int idx) {  
		String fmt = 
				"<tr>" +   
				"<td class=\"urlPath\"><a href=\"%s\">%s</a></td>" +
				"<td class=\"module\">%s</td>" +
				"<td class=\"returnType\">%s</td>" +  
				"<td class=\"methodParams\"><code><strong><a href=\"%s\">%s</a></strong>(%s)</code>" +  
				"</td>" + 
				"</tr>"; 
		String methodLink = m.urlPath;
		if(!docRootPath.equals("/")) {
		     methodLink = docRootPath + methodLink; 
		}
		String method = m.method;
		String paramList = "";
		int size = m.paramNames.size();
		if(size < m.paramTypes.size()) {
			size = m.paramTypes.size();
		}
		for(int i=0;i<size;i++) { 
			if(i<m.paramTypes.size()) { 
				paramList += m.paramTypes.get(i);
			}
			if(i<m.paramNames.size()) { 
				if(i<m.paramTypes.size()) paramList += " ";
				paramList += m.paramNames.get(i) ;
			}
			paramList += ", ";
		} 
		if(paramList.length() > 0) {
			paramList = paramList.substring(0, paramList.length()-2);
		}    
		
		return String.format(fmt, methodLink, methodLink, m.module, m.returnType, methodLink, method,
				paramList);
	} 
	
	
}