package io.zbus.mq;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * URL mapping to MQ, when mq is missing in request message.
 * 
 * If no mapping is provided, /{mq}/xx/... format of url is assume, and the first part of url is
 * assumed to be mq
 * 
 * @author leiming.hong Jul 3, 2018
 *
 */

public class UrlMapping {
	private Map<String, List<Entry>> mq2url = new ConcurrentHashMap<>();
	private Map<String, String> url2mq = new ConcurrentHashMap<>();
	
	public void addMapping(Entry entry) {
		
	}
	
	public void addMapping(List<Entry> entryList) {
		
	}
	
	public static class Entry{
		public String url; //url path
		public String mq;  
	} 
}
