package io.zbus.transport.inproc;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.transport.AbastractClient;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class InprocClient extends AbastractClient implements Session { 
	private static final Logger logger = LoggerFactory.getLogger(InprocClient.class); 
	private ConcurrentMap<String, Object> attributes = null;
	
	private IoAdaptor ioAdaptor;
	private final String id = StrKit.uuid();
	private boolean active = false;
	private Object lock = new Object();
	
	public InprocClient(IoAdaptor ioAdaptor) {
		this.ioAdaptor = ioAdaptor; 
		
	}
	
	@Override
	public void connect() { 
		synchronized (lock) {
			if(active) return;
		}
		active = true;
		try {
			ioAdaptor.sessionCreated(this);
			if(onOpen != null) {
				runner.submit(()->{
					try {
						onOpen.handle();
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				});
			} 
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void close() throws IOException { 
		super.close(); 
		
		active = false;
		ioAdaptor.sessionToDestroy(this); 
	} 

	@Override
	public String id() { 
		return id;
	}

	@Override
	public String remoteAddress() { 
		return "InprocServer";
	}

	@Override
	public String localAddress() {
		return "Inproc-"+id;
	}
	
 
	@Override
	public void write(Object msg) {  //Session received message  
		try {
			Message data = null;
			if(msg instanceof Message) {
				data = (Message)msg; 
			} else if(msg instanceof byte[]) {
				data = JsonKit.parseObject((byte[])msg, Message.class);
			} else if(msg instanceof String) {
				data = JsonKit.parseObject((String)msg, Message.class);
			} else {
				throw new IllegalArgumentException("type of msg not support: " + msg.getClass());
			}
			if(onMessage != null) {
				onMessage.handle(data);
			} 
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	protected void sendMessage0(Message data) {  //Session send out message
		synchronized (lock) {
			if(!active) {
				connect();
			}
		}
		
		try {
			ioAdaptor.onMessage(data, this);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean active() { 
		return active;
	}  
	
	@SuppressWarnings("unchecked")
	public <V> V attr(String key) {
		if (this.attributes == null) {
			return null;
		}

		return (V) this.attributes.get(key);
	}

	public <V> void attr(String key, V value) {
		if(value == null){
			if(this.attributes != null){
				this.attributes.remove(key);
			}
			return;
		}
		if (this.attributes == null) {
			synchronized (this) {
				if (this.attributes == null) {
					this.attributes = new ConcurrentHashMap<String, Object>();
				}
			}
		}
		this.attributes.put(key, value);
	} 
	
	@Override
	public String toString() { 
		return localAddress();
	}
}
