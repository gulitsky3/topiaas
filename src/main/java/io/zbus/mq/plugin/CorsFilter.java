package io.zbus.mq.plugin;

import io.zbus.mq.MqServerAdaptor;
import io.zbus.transport.Message;

public class CorsFilter implements Filter {

	@Override
	public void init(MqServerAdaptor mqServerAdaptor) {
	}

	@Override
	public boolean doFilter(Message req, Message res) {
		if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
			res.setHeader("Access-Control-Allow-Origin", "*");
			res.setHeader("Access-Control-Allow-Headers", "X-Requested-With,content-type");
			res.setHeader("Access-Control-Allow-Methods", "PUT,POST,GET,DELETE,OPTIONS");
			res.setStatus(200); 
			return false;
		}
		
		return true;
	}
}
