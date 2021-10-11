package org.zbus.remoting;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.zbus.common.Helper;

class RequestThread extends Thread{
	final int count;
	final ClientDispatcherManager mgr;
	String host;
	int port;  
	AtomicLong currentRequestCount;
	long startTime;
	
	RequestThread(int count, ClientDispatcherManager mgr){
		this.count = count;
		this.mgr = mgr; 
	}
	
	@Override
	public void run() {
		final RemotingClient client = new RemotingClient(host, port, mgr);
		Message req = new Message(); 
		req.setCommand("test");
		req.setBody("hello"); 
	
		
		for(int i=0;i<count;i++){ 
			try {
				Message reply = client.invokeSync(req, 10000);
				if(reply == null){
					reply = null;
				} 
				currentRequestCount.incrementAndGet();
			} catch (IOException e) { 
				e.printStackTrace();
				break;
			}
			if(i%1000 == 0){
				long elapsed = System.currentTimeMillis()-startTime;
				System.out.format("QPS: %.2f\n", 1000.*currentRequestCount.get()/elapsed);
			}
		}
	}
	
}

public class PerfClient { 
	public static void main(String[] args) throws Exception {
		final int count = Helper.option(args, "-n", 1000000);
		final int threadCount = Helper.option(args, "-c", 8);
		final String zbusHost = Helper.option(args, "-h", "127.0.0.1");
		final int zbusPort = Helper.option(args, "-p", 80);  
		
		final ClientDispatcherManager mgr = new ClientDispatcherManager(); 
		mgr.start();
		
		
		final long start = System.currentTimeMillis();
		final AtomicLong currentRequestCount = new AtomicLong(0);
		 
		Thread[] threads = new Thread[threadCount];
		for(int i=0;i<threads.length;i++){
			RequestThread t = new RequestThread(count, mgr);
			t.host = zbusHost;
			t.port = zbusPort; 
			t.currentRequestCount = currentRequestCount;
			t.startTime = start;
			
			threads[i] = t;
		}  
		for(Thread t : threads){
			t.start();
		}
		for(Thread t: threads){
			t.join();
		} 
		
	} 
}
