package io.zbus.mq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;

public class SubscriptionManager {  
	private static final Logger logger = LoggerFactory.getLogger(SubscriptionManager.class); 
	private Map<String, Subscription> clientId2Subscription = new ConcurrentHashMap<>(); 
	//{ MQ=>{ channel=>List<Subscription> } }
	private Map<String, Map<String, List<Subscription>>> subTable = new ConcurrentHashMap<>();
	
	private MqManager messageQueueManager;
	
	public SubscriptionManager(MqManager messageQueueManager) {
		this.messageQueueManager = messageQueueManager;
	}
	
	public synchronized Subscription get(String clientId) {
		return clientId2Subscription.get(clientId);
	}
	
	public synchronized void add(Subscription subscription) {
		clientId2Subscription.put(subscription.clientId, subscription); 
		Map<String, List<Subscription>> mqSubTable = subTable.get(subscription.mq);
		if(mqSubTable == null) {
			mqSubTable = new ConcurrentHashMap<>();
			subTable.put(subscription.mq, mqSubTable);
		} 
		List<Subscription> subs = mqSubTable.get(subscription.channel);
		if(subs == null) {
			subs = new ArrayList<>();
			mqSubTable.put(subscription.channel, subs);
		}
		subs.add(subscription);   
	}
	
	
	public synchronized List<Subscription> getSubscriptionList(String mq, String channel){
		Map<String, List<Subscription>> mqSubTable = subTable.get(mq);
		if(mqSubTable == null) return null;
		return mqSubTable.get(channel);
	}
	
	private int subscriptionCount(String mq){
		Map<String, List<Subscription>> mqSubTable = subTable.get(mq);
		if(mqSubTable == null) return 0; 
		int c = 0;
		for(List<Subscription> subs : mqSubTable.values()) {
			c += subs.size();
		}
		return c;
	}
	
	public synchronized void removeByClientId(String clientId) { 
		Subscription sub = clientId2Subscription.remove(clientId);
		if(sub == null) return;
		
		MessageQueue mq = messageQueueManager.get(sub.mq);
		if(mq != null) {
			Integer mask = mq.getMask();
			if(mask != null && (Protocol.MASK_DELETE_ON_EXIT & mask) != 0) { 
				int subCount = subscriptionCount(sub.mq);
				if(subCount<=1) {
					try { 
						messageQueueManager.removeQueue(mq.name(), null); 
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		}
		
		Map<String, List<Subscription>> mqSubTable = subTable.get(sub.mq);
		if(mqSubTable == null) return;  
		for(List<Subscription> subs : mqSubTable.values()) { //TODO make it efficient
			subs.remove(sub);
		} 
	}
	
	public synchronized void removeByChannel(String mq, String channel) {
		Map<String, List<Subscription>> mqSubTable = subTable.get(mq);
		if(mqSubTable == null) return;
		List<Subscription> subs = mqSubTable.remove(channel); 
		if(subs == null) return;
		
		for(Subscription sub : subs) {
			clientId2Subscription.remove(sub.clientId);
		}
	}
	
	
	public synchronized void removeByMq(String mq) { 
		Iterator<Entry<String, Subscription>> iter = clientId2Subscription.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, Subscription> entry = iter.next(); 
			if(mq.equals(entry.getValue().mq)) {
				iter.remove();
			}
		} 
		subTable.remove(mq); 
	} 
}
