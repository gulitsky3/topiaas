package io.zbus.mq.plugin;

import io.zbus.mq.MessageQueueManager;

/**
 * 
 * Map message's url to MQ
 * 
 * @author leiming.hong Jul 9, 2018
 *
 */
public interface UrlMqRouter {
	/**
	 * Dispatch request message by its URL
	 * @param req message in request
	 * @param sess where message from
	 * @return true if message is handled well -- no need to do next, false otherwise.
	 */
	String match(MessageQueueManager mqManager, String url);
}
