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
package org.zbus.net.simple;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.zbus.kit.NetKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.simple.DefaultSession.SessionStatus;
import org.zbus.net.simple.SelectorGroup.IoStack; 

/**
 * 
 * 
 * @author rushmore (洪磊明)
 *
 */
public class SelectorThread extends Thread {
	private static final Logger log = Logger.getLogger(SelectorThread.class);
	
	protected volatile java.nio.channels.Selector selector = null;
	protected final SelectorGroup selectorGroup;
	private final Queue<Object[]> registerSess = new LinkedBlockingQueue<Object[]>();
	private final Queue<DefaultSession> unregisterSess = new LinkedBlockingQueue<DefaultSession>(); 
	
	private final Queue<Object[]> interestOps = new LinkedBlockingQueue<Object[]>();
	
	public SelectorThread(SelectorGroup selectorGroup, String name) throws IOException{
		super(name);
		this.selectorGroup = selectorGroup;
		this.selector = java.nio.channels.Selector.open();
	}
	
	public SelectorThread(SelectorGroup dispatcher) throws IOException{
		this(dispatcher, "Selector");
	}
	
	
	public void registerChannel(SelectableChannel channel, int ops) throws IOException{
		registerChannel(channel, ops, null); 
	}
	
	public void registerSession(int ops, DefaultSession sess) throws IOException{
		registerChannel(sess.getChannel(), ops, sess);
	}
	
	public void registerChannel(SelectableChannel channel, int ops, DefaultSession sess) 
			throws IOException{
		if(sess != null){
			sess.setSelectorThread(this);
		}
		if(Thread.currentThread() == this){
			SelectionKey key = channel.register(this.selector, ops, sess); 
			if(sess != null){ 
				sess.setRegisteredKey(key);
				sess.setStatus(SessionStatus.CONNECTED);
				sess.getIoAdaptor().onSessionRegistered(sess);
			} 
		} else { 
			this.registerSess.offer(new Object[]{channel, ops, sess});
			wakeup();
		}
	}
	
	public void interestOps(SelectionKey key, int ops){
		if(Thread.currentThread() == this){
			key.interestOps(ops);
		} else { 
			this.interestOps.offer(new Object[]{key, ops});
			wakeup();
		}
	}
	
	public void unregisterSession(DefaultSession sess){
		if(this.unregisterSess.contains(sess)){
			return;
		}
		this.unregisterSess.add(sess);
		wakeup();
	}
	
	private AtomicBoolean wakeup = new AtomicBoolean(false);
	
	public void wakeup(){
		if(wakeup.compareAndSet(false, true)){
			this.selector.wakeup();
		}
	}
	
	
	@Override
	public void interrupt() {  
		super.interrupt();
		try {
			this.selector.close();
		} catch (IOException e) { 
			log.error(e.getMessage(), e);
		}
	} 
   
	@Override
	public void run() { 
		try{
			while(!isInterrupted()){ 
				wakeup.getAndSet(false); 
				selector.select();
				if (wakeup.get()) { 
                    selector.wakeup();
                }
				
				handleRegister(); 
				handleInterestOps();
				
				Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				while(iter.hasNext()){
					SelectionKey key = iter.next();
					iter.remove();
					if(!key.isValid()) continue;
					
					Object att = key.attachment();
					if(att != null && att instanceof DefaultSession){
						((DefaultSession)att).updateLastOperationTime();
					}
					try{ 
						if(key.isAcceptable()){ 
							handleAcceptEvent(key);
						} else if (key.isConnectable()){
							handleConnectEvent(key);
						} else if (key.isReadable()){
							handleReadEvent(key);
						} else if (key.isWritable()){
							handleWriteEvent(key);
						}
					} catch(Throwable e){ 
						disconnectWithException(key, e); 
					}
				} 
				handleUnregister();
			}
		} catch(Exception e) { 
			if(log.isDebugEnabled()){
				log.debug("Ignore: " + e.getMessage());
			} 
		}
	}
	
	private void disconnectWithException(final SelectionKey key, final Throwable e){  
		final DefaultSession sess = (DefaultSession)key.attachment();
		if(sess == null){
			try{ 
				key.channel().close();   
				key.cancel();
			} catch(Throwable ex){
				log.error(e.getMessage(), ex);
			} 
			return;
		} 
		
		sess.setStatus(SessionStatus.ON_ERROR);
		selectorGroup.asyncRun(new Runnable() { 
			@Override
			public void run() {
				try{   
					sess.getIoAdaptor().onException(e, sess);
				} catch (Throwable ex){
					if(!selectorGroup.isStarted()){
						log.debug(ex.getMessage(), ex);
					} else {
						log.error(ex.getMessage(), ex);
					}
				} 
			}
		}); 
		
		try{ 
			sess.close(); 
			key.cancel();
		} catch(Throwable ex){
			log.error(e.getMessage(), ex);
		}

	}
	
	protected void handleRegister(){
		Object[] item = null;
		while( (item=this.registerSess.poll()) != null){
			try{
				SelectableChannel channel = (SelectableChannel) item[0];
				if (!channel.isOpen() ) continue;
				int ops = (Integer)item[1];
				DefaultSession sess = (DefaultSession) item[2]; 
				
				SelectionKey key = channel.register(this.selector, ops, sess);
				if(sess != null){
					sess.setRegisteredKey(key);
					sess.getIoAdaptor().onSessionRegistered(sess);
				} 
				
			}catch(Exception e){
				log.error(e.getMessage(), e);
			}
		} 
	}
	
	protected void handleUnregister(){
		DefaultSession sess = null;
		while( (sess = this.unregisterSess.poll()) != null ){
			try { 
				sess.close(); 
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}
	
	protected void handleInterestOps(){
		Object[] item = null;
		while( (item=this.interestOps.poll()) != null){
			try{
				SelectionKey key = (SelectionKey) item[0];
				if (!key.isValid()) continue;
				int ops = (Integer)item[1]; 
				key.interestOps(ops); 
			}catch(Exception e){
				log.error(e.getMessage(), e);
			}
		} 
	}
	
	protected void handleAcceptEvent(SelectionKey key) throws Exception{ 
		ServerSocketChannel server = (ServerSocketChannel) key.channel();
		
		SocketChannel channel = server.accept();
		channel.configureBlocking(false); 
		
		SocketAddress serverAddress = server.socket().getLocalSocketAddress();
		if(log.isDebugEnabled()){ 
			log.debug("ACCEPT: server(%s) %s=>%s", serverAddress, NetKit.remoteAddress(channel), NetKit.localAddress(channel));
		} 
		
		IoStack stack = selectorGroup.ioAdaptor(serverAddress);
		if(stack == null){
			log.warn("Missing IoAdaptor/Codec for %s", serverAddress);
			return;
		}
		
		DefaultSession sess = new DefaultSession(selectorGroup, channel, stack.ioAdaptor, stack.codec); 
		sess.setStatus(SessionStatus.CONNECTED); //set connected 
		
		if(sess.selectorGroup() != null){
			sess.selectorGroup().registerSession(SelectionKey.OP_READ, sess); 
		}
		sess.getIoAdaptor().onSessionAccepted(sess);
		
	} 
	
	protected void handleConnectEvent(SelectionKey key) throws Exception{
		final SocketChannel channel = (SocketChannel) key.channel();
		
		DefaultSession sess = (DefaultSession) key.attachment();
		if(sess == null){
			throw new IOException("Session not attached yet to SelectionKey");
		} 
		try{
			if(channel.finishConnect()){ 
				sess.finishConnect();
				if(log.isDebugEnabled()){
					log.debug("CONNECT: %s=>%s", NetKit.localAddress(channel), NetKit.remoteAddress(channel));
				}
			} 
		} catch(ConnectException ex){ 
			String msg = String.format("Connection failed: %s", sess.getRemoteAddress());
			throw new IOException(msg, ex);
		}
		sess.setStatus(SessionStatus.CONNECTED);  
		key.interestOps(0); //!!!clear interest of OP_CONNECT to avoid looping CPU !!!
		
		sess.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
		sess.getIoAdaptor().onSessionConnected(sess);
	
	}
	
	protected void handleReadEvent(SelectionKey key) throws Exception{
		DefaultSession sess = (DefaultSession) key.attachment();
		if(sess == null){
			throw new IOException("Session not attached yet to SelectionKey");
		}
		sess.doRead();
	}
	
	protected void handleWriteEvent(SelectionKey key) throws Exception{
		DefaultSession sess = (DefaultSession) key.attachment();
		if(sess == null){
			throw new IOException("Session not attached yet to SelectionKey");
		} 
		sess.doWrite();
	}

}