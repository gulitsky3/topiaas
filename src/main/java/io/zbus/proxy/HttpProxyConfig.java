package io.zbus.proxy;

import static io.zbus.kit.ConfigKit.valueOf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.zbus.kit.ConfigKit.XmlConfig;

public class HttpProxyConfig extends XmlConfig { 
	public static class ProxyConfig{
		public String location;
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
			String location = valueOf(xpath.evaluate("location", node), null); 
			String backend = valueOf(xpath.evaluate("backend", node), null); 
			ProxyConfig config = new ProxyConfig();
			config.location = location;
			config.backend = backend; 
			
			proxyList.add(config);
		}
	}  
}
