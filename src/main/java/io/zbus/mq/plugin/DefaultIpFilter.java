package io.zbus.mq.plugin;

import io.zbus.transport.Session;

public class DefaultIpFilter implements IpFilter {

	@Override
	public boolean doFilter(Session sess) { 
		return true;
	}

}
