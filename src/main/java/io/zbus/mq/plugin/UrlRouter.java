package io.zbus.mq.plugin;

import io.zbus.mq.MqServerAdaptor;
import io.zbus.transport.Message;
import io.zbus.transport.Session;
 
public interface UrlRouter {
	
	void init(MqServerAdaptor mqServerAdaptor);
	
	boolean route(Message req, Session sess);
}
