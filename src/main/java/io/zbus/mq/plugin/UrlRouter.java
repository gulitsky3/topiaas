package io.zbus.mq.plugin;

import io.zbus.mq.MessageQueueManager;

/**
 * 
 * Map message's url to MQ
 * 
 * @author leiming.hong Jul 9, 2018
 *
 */
public interface UrlRouter {
	/**
	 * Dispatch request message by its URL
	 * @param req message in request
	 * @param sess where message from
	 * @return mq name of matched from URL
	 */
	String match(MessageQueueManager mqManager, String url);
}
