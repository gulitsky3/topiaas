package io.zbus.mq.plugin;

import io.zbus.transport.Session;

/**
 * 
 * Filter on session when created
 * 
 * @author leiming.hong Jul 11, 2018
 *
 */
public interface IpFilter { 
	/**
	 * Control logic when session created, such as white/black list
	 * 
	 * @param sess Session just created
	 * @return true if session is allowed, false otherwise
	 */
	boolean doFilter(Session sess);
}
