package io.zbus.mq;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class MessageDispatcher {
	private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

	private SubscriptionManager subscriptionManager;
	private Map<String, Session> sessionTable;
	private Map<String, Long> loadbalanceTable = new ConcurrentHashMap<String, Long>(); // channel => index

	private int batchReadSize = 10;
	private ExecutorService dispatchRunner = Executors.newFixedThreadPool(64);

	public MessageDispatcher(SubscriptionManager subscriptionManager, Map<String, Session> sessionTable) {
		this.subscriptionManager = subscriptionManager;
		this.sessionTable = sessionTable;
	}

	public void dispatch(MessageQueue mq, String channel) {
		dispatchRunner.submit(() -> {
			dispatch0(mq, channel);
		});
	}

	protected void dispatch0(MessageQueue mq, String channel) {
		List<Subscription> subs = subscriptionManager.getSubscriptionList(mq.name(), channel);
		if (subs == null || subs.size() == 0)
			return;

		synchronized (subs) {
			Long index = loadbalanceTable.get(channel);
			if (index == null) {
				index = 0L;
			}
			while (true) {
				List<Message> data;
				try {
					data = mq.read(channel, batchReadSize);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					break;
				}
				for (Message message : data) {
					String filter = (String) message.getHeader(Protocol.TOPIC);
					int N = subs.size();
					long max = index + N;
					while (index < max) {
						Subscription sub = subs.get((int) (index % N));
						index++;
						if (index < 0)
							index = 0L;
						if (sub.topics.isEmpty() || sub.topics.contains(filter)) {
							Session sess = sessionTable.get(sub.clientId);
							if (sess == null)
								continue;
							message.addHeader(Protocol.CHANNEL, channel); 
							sess.write(message); 
							break;
						}
					}
				}
				if (data.size() < batchReadSize)
					break;
			}
			loadbalanceTable.put(channel, index);
		}
	}

	public void dispatch(MessageQueue mq) {
		Iterator<String> iter = mq.channelIterator();
		while(iter.hasNext()) {
			String channel = iter.next();
			dispatch(mq, channel);
		} 
	}

	public void take(MessageQueue mq, String channel, int count, String reqMsgId, Session sess) throws IOException { 
		List<Message> data = mq.read(channel, count);
		Message message = new Message();  
		int status = data.size()>0? 200 : 604;//Special status code, no DATA
		message.setStatus(status); 
		message.addHeader(Protocol.ID, reqMsgId);
		message.addHeader(Protocol.MQ, mq.name());
		message.addHeader(Protocol.CHANNEL, channel);   
		message.setBody(data); 
		
		
		sess.write(message); 
	} 
}
