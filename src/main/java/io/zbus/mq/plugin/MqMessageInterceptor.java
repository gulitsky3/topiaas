package io.zbus.mq.plugin;

import io.zbus.transport.Message;

/**
 * 
 * Intercept message before pub or sub
 * 
 * @author leiming.hong Jul 11, 2018
 *
 */
public interface MqMessageInterceptor {
	/**
	 * intercept message before pub or sub(sending to subscribers)
	 * @param message
	 * @return false if message discarded, true otherwise
	 */
	boolean intercept(Message message);
}
