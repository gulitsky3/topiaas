package org.zbus.server.mq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.logging.Logger;
import org.logging.LoggerFactory;
import org.remoting.Message;
import org.zbus.common.ConsumerInfo;
import org.znet.Session;

public class PubSub extends AbstractMQ {   
	private static final Logger log = LoggerFactory.getLogger(PubSub.class);	
	
	//保留所有的订阅Session
	final ConcurrentMap<String, PullSession> sessMap = new ConcurrentHashMap<String, PullSession>(); 
	public PubSub(String name, ExecutorService executor, int mode){
		super(name, executor, mode); 
	}
	
	@Override
	public void consume(Message msg, Session sess) throws IOException{ 
		PullSession pull = sessMap.get(sess.id());
		if(pull != null){
			pull.setPullMsg(msg); 
		} else {
			pull = new PullSession(sess, msg); 
			sessMap.putIfAbsent(sess.id(), pull);
		} 
		this.dispatch();
	} 
	
	@Override
	void doDispatch() throws IOException{ 
		Message msg = null;
		while((msg = msgQ.poll()) != null){
			String topic = msg.getTopic();
			Iterator<Entry<String, PullSession>> iter = sessMap.entrySet().iterator();
			while(iter.hasNext()){
				PullSession sess = iter.next().getValue();
				if(sess == null || !sess.getSession().isActive()){
					iter.remove();
					continue;
				} 
				if(sess.isTopicMatched(topic)){ 
					Message copy = Message.copyWithoutBody(msg);
					copy.setStatus("200"); 
					sess.getMsgQ().offer(copy);
				}
			}
		} 
	 
		Iterator<Entry<String, PullSession>> iter = sessMap.entrySet().iterator();
		while(iter.hasNext()){
			PullSession sess = iter.next().getValue();
			if(sess == null || !sess.getSession().isActive()){
				iter.remove();
				continue;
			} 
			try{
				sess.pullMsgLock.lock();
				Message pullMsg = sess.getPullMsg();
				if(pullMsg == null) continue; //无消息读取请求
				
				msg = sess.getMsgQ().poll();
				if(msg == null) continue; //消息未到达
				
				sess.setPullMsg(null);
				msg.setMsgIdRaw(pullMsg.getMsgId()); //保留原始消息ID
				msg.setMsgId(pullMsg.getMsgId());    //配对订阅消息！
				sess.getSession().write(msg);
			} catch(IOException ex){
				log.error(ex.getMessage(), ex);
			} finally{
				sess.pullMsgLock.unlock();
			}
		}  
	}

	public List<ConsumerInfo> getConsumerInfoList() {
		List<ConsumerInfo> res = new ArrayList<ConsumerInfo>();
		Iterator<Entry<String, PullSession>> iter = sessMap.entrySet().iterator();
		while(iter.hasNext()){
			PullSession value = iter.next().getValue();
			Session sess = value.getSession(); 
			ConsumerInfo info = new ConsumerInfo();
			info.setStatus(sess.getStatus().toString());
			info.setRemoteAddr(sess.getRemoteAddress());
			if(value.getTopics() != null){
				info.setTopics(new ArrayList<String>(value.getTopics()));
			}
			res.add(info);
		}
		return res;
	}
}