/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zbus.transport.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session; 

public class HttpWsServerAdaptor extends ServerAdaptor{     
	protected MessageHandler<Message> filterHandler;   
	protected Map<String, MessageHandler<Message>> handlerMap = new ConcurrentHashMap<>();  
	
	public HttpWsServerAdaptor(){ 
		this(null);
	}
	
	public HttpWsServerAdaptor(Map<String, Session> sessionTable){
		super(sessionTable); 
	} 
	
	public void url(String url, MessageHandler<Message> handler){ 
    	this.handlerMap.put(url, handler);
    }
	 
    public void registerFilterHandler(MessageHandler<Message> filterHandler) {
		this.filterHandler = filterHandler;
	}  
    
    public void onMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	final String msgId = msg.getHeader(Message.ID); 
    	
    	if(this.filterHandler != null){
    		this.filterHandler.handle(msg, sess);
    	}
    	
    	String url = msg.getUrl();
    	MessageHandler<Message> handler = handlerMap.get(url);
    	if(handler != null){
    		handler.handle(msg, sess);
    		return;
    	}  
    	
    	Message res = new Message();
    	res.addHeader(Message.ID, msgId);
    	res.setStatus(404);
    	String text = String.format("404: %s Not Found", url);
    	res.setBody(text); 
    	sess.write(res); 
    }   
}

