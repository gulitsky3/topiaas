package io.zbus.transport;

import java.io.Closeable;

public interface Session extends Closeable {
	public static final String TYPE_KEY = "type";

	public static enum SessionType {
		Websocket,
		HTTP,
		Inproc,
		IPC
	}
	
	String id(); 
	
	String remoteAddress();
	
	String localAddress();
	
	void write(Object msg); 
	
	boolean active(); 
	
	<V> V attr(String key);
	
	<V> void attr(String key, V value);
}
