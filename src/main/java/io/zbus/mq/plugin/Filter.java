package io.zbus.mq.plugin;

import io.zbus.mq.MqServerAdaptor;
import io.zbus.transport.Message;
 
public interface Filter {
	
	void init(MqServerAdaptor mqServerAdaptor);
	
	/** 
	 * @param req
	 * @param resp
	 * @return true if next filter execution required
	 */
	boolean doFilter(Message req, Message resp);
}
