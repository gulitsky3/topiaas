package io.zbus.proxy.http;

import static io.zbus.kit.ConfigKit.valueOf;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.zbus.kit.ConfigKit.XmlConfig;

public class HttpProxyConfig extends XmlConfig { 
	public static class ProxyConfig{
		public String urlPrefix;
		public String urlRewrite;
		public String backend;
	}
	
	public static final String HttpProxyXPath = "/zbus/httpProxy"; 
	
	public List<ProxyConfig> proxyList = new ArrayList<>();  
	
	public HttpProxyConfig() { 
		
	}
	
	public HttpProxyConfig(String configXmlFile) {
		loadFromXml(configXmlFile);
	}

	@Override
	public void loadFromXml(Document doc) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList list = (NodeList) xpath.compile(HttpProxyXPath+"/*").evaluate(doc, XPathConstants.NODESET);
		if (list == null || list.getLength() <= 0)
			return;
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i); 
			ProxyConfig config = new ProxyConfig();  
			config.urlPrefix = valueOf(xpath.evaluate("urlPrefix", node), null); 
			config.urlRewrite = valueOf(xpath.evaluate("urlRewrite", node), null);  
			config.backend = valueOf(xpath.evaluate("backend", node), null);  
			
			proxyList.add(config);
		}
	}  
	
	public Map<String, ProxyTarget> buildProxyTable(){
		Map<String, ProxyTarget> table = new HashMap<>();
		for(ProxyConfig config : proxyList) {
			ProxyTarget target = new ProxyTarget();
			target.urlPrefix = config.urlPrefix;
			target.urlRewrite = config.urlRewrite;
			
			URI uri;
			try {
				uri = new URI(config.backend);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("backend invalid: " + config.backend);
			}   
			target.remoteHost = uri.getHost() == null ? "127.0.0.1" : uri.getHost(); 
			target.remotePort = uri.getPort();
			if (target.remotePort == -1) {
				target.remotePort = 80;
			}
			
			table.put(target.urlPrefix, target);
		}
		return table;
	}
}
