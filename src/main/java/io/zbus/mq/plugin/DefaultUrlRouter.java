package io.zbus.mq.plugin;

import io.zbus.mq.MqManager;

public class DefaultUrlRouter implements UrlRouter { 
	public String match(MqManager mqManager, String url) {
		int length = 0; 
		String matched = null;
		for(String mq : mqManager.mqNames()) { 
			if(url.startsWith(mq)) {
				if(mq.length() > length) {
					length = mq.length();
					matched = mq; 
				}
			}
		}  
		return matched;
	}
}
