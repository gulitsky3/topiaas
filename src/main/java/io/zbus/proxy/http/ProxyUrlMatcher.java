package io.zbus.proxy.http;

import java.util.Map;
import java.util.Map.Entry;

public class ProxyUrlMatcher {
	private Map<String, ProxyTarget> targetTable;
	public ProxyUrlMatcher(Map<String, ProxyTarget> targetTable) {
		this.targetTable = targetTable;
	} 
	
	public ProxyTarget match(String url) {
		ProxyTarget target = null;
		for(Entry<String, ProxyTarget> e : targetTable.entrySet()) { 
			String urlPrefix = e.getValue().urlPrefix;
			if(!urlPrefix.endsWith("/")) {
				urlPrefix += "/";
			} 
			if(url.startsWith(urlPrefix) || url.equals(e.getValue().urlPrefix)) { // /abc match(/abc or /abc/d)  /abcd not match
				target = e.getValue();
				break;
			}  
		} 
		return target;
	}
}
