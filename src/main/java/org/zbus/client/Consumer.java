package org.zbus.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zbus.protocol.MessageMode;
import org.zbus.protocol.Proto;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.callback.MessageCallback;


public class Consumer extends MqAdmin implements Closeable{    
	private static final Logger log = LoggerFactory.getLogger(Consumer.class);  
	private RemotingClient client;  //消费者拥有一个物理链接
	//为发布订阅者的主题，当Consumer的模式为发布订阅时候起作用
	private String topic = null;
	
	public Consumer(Broker broker, String mq, MessageMode... mode){  
		super(broker, mq, mode);
	} 
	
	public Consumer(MqConfig config){
		super(config);
		this.topic = config.getTopic();
	} 
	
	
    public Message recv(int timeout) throws IOException{ 
    	if(this.client == null){
	    	this.client = broker.getClient(myClientHint());
    	}
    	Message req = new Message();
    	req.setCommand(Proto.Consume);
    	req.setMq(mq);
    	req.setToken(accessToken); 
    	if(MessageMode.isEnabled(this.mode, MessageMode.PubSub)){
    		if(this.topic != null){
    			req.setTopic(this.topic);
    		}
    	}
    	
    	Message res = null;
    	try{
	    	res = client.invokeSync(req, timeout);
			if(res != null && res.isStatus404()){
				if(!this.createMQ()){
					throw new IllegalStateException("register error");
				}
				return recv(timeout);
			}
    	} catch(IOException e){
    		log.error(e.getMessage(), e);
    		try{
    			broker.closeClient(client);
    			client = broker.getClient(myClientHint());
    		} catch(IOException ex){
    			log.error(e.getMessage(), e);
    		}
    	}
    	return res;
    }  
     
	public void close() throws IOException { 
		if(this.client != null){
			this.broker.closeClient(this.client);
		}
	}
	
	@Override
	protected Message invokeCreateMQ(Message req) throws IOException {
		if(this.client == null){
	    	this.client = broker.getClient(myClientHint());
    	}
		return client.invokeSync(req, invokeTimeout);
	}
	
    
    public void reply(Message msg) throws IOException{ 
    	if(msg.getStatus() != null){
    		msg.setReplyCode(msg.getStatus());
    	}
    	msg.setCommand(Proto.Produce); 
    	msg.setAck(false);
    	client.getSession().write(msg); 
    }
    
    
    
    protected ScheduledExecutorService executorService = null;
    private MessageCallback callback;
    public void onMessage(MessageCallback cb) throws IOException{
    	this.callback = cb;
    	if(executorService == null){
    		executorService = Executors.newSingleThreadScheduledExecutor(); 
    	} else {  
    		return;
    	}
  
    	executorService.submit(new Runnable() { 
			public void run() { 
				for(;;){
					try {
						Message msg = recv(10000);
						if(msg == null){
							continue;
						}
						callback.onMessage(msg, client.getSession());
					} catch (IOException e) { 
						//
					}
				}
			}
		});
    }
    
    
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		if(!MessageMode.isEnabled(this.mode, MessageMode.PubSub)){
			throw new IllegalStateException("topic support for none-PubSub mode");
		}
		this.topic = topic;
	}
}
