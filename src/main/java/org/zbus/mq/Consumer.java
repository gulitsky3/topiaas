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
package org.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import org.zbus.broker.Broker;
import org.zbus.broker.Broker.BrokerHint;
import org.zbus.kit.log.Logger;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;

public class Consumer extends MqAdmin implements Closeable {
	private static final Logger log = Logger.getLogger(Consumer.class); 
	
	private MessageInvoker client;  
	private String topic = null;  
	private int consumeTimeout = 300000; // 5 minutes

	public Consumer(Broker broker, String mq, MqMode... mode) {
		super(broker, mq, mode);
	}

	public Consumer(MqConfig config) {
		super(config);
		this.topic = config.getTopic();
	}

	private BrokerHint brokerHint() {
		BrokerHint hint = new BrokerHint();
		hint.setEntry(this.mq);
		return hint;
	}

	private void ensureClient() throws IOException {
		if (this.client == null) {
			synchronized (this) {
				if (this.client == null) {
					this.client = broker.getClient(brokerHint());
				}
			}
		}
	}
	
	/**
	 * @deprecated use take instead
	 * 
	 * @param timeout
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Message recv(int timeout) throws IOException, InterruptedException {
		return take(timeout);
	}

	public Message take(int timeout) throws IOException, InterruptedException {
		ensureClient();

		Message req = new Message();
		req.setCmd(Protocol.Consume);
		req.setMq(mq);
		req.setHead("token", accessToken);
		if (MqMode.isEnabled(this.mode, MqMode.PubSub)) {
			if (this.topic != null) {
				req.setTopic(this.topic);
			}
		}

		Message res = null;
		try { 
			res = client.invokeSync(req, timeout);
			if (res == null)
				return res;
			res.setId(res.getRawId());
			res.removeHead(Message.RAWID);
			if (res.isStatus200())
				return res;

			if (res.isStatus404()) {
				if (!this.createMQ()) {
					throw new MqException(res.getBodyString());
				}
				return take(timeout);
			}
			throw new MqException(res.getBodyString());
		} catch (ClosedByInterruptException e) {
			throw new InterruptedException(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			try {
				broker.closeClient(client);
				client = broker.getClient(brokerHint());
			} catch (IOException ex) {
				log.error(ex.getMessage(), ex);
			}
		}
		return res;
	}

	public Message take() throws InterruptedException, IOException {
		while (true) {
			Message message = take(consumeTimeout);
			if (message == null)
				continue;
			return message; 
		}
	} 

	@Override
	protected Message invokeSync(Message req) throws IOException, InterruptedException {
		ensureClient();
		return client.invokeSync(req, 10000);
	}

	private MessageInvoker replyClient; // Linux优化，Reply使用分离的Client

	private void ensureReplyClient() throws IOException {
		if (this.replyClient == null) {
			synchronized (this) {
				if (this.replyClient == null) {
					this.replyClient = broker.getClient(brokerHint());
				}
			}
		}
	}
	 
	public void routeMessage(Message msg) throws IOException {
		ensureReplyClient();
		msg.setCmd(Protocol.Route);
		msg.setAck(false);

		replyClient.invokeAsync(msg, null);
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		if (!MqMode.isEnabled(this.mode, MqMode.PubSub)) {
			throw new IllegalStateException("topic require PubSub mode");
		}
		this.topic = topic;
	}

	private volatile Thread consumerThread = null;
	private volatile ConsumerHandler consumerHandler;
	private final Runnable consumerTask = new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					final Message msg;
					try {
						msg = take();
					} catch (InterruptedException e) {
						Consumer.this.close();
						break;
					} catch (MqException e) {
						log.error(e.getMessage(), e);
						Consumer.this.close();
						break;
					} 
					if (consumerHandler == null) {
						log.warn("Missing consumerHandler, call onMessage first");
						continue;
					}
					try {
						consumerHandler.handle(msg, Consumer.this);
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	};

	public void onMessage(final ConsumerHandler handler) {
		this.consumerHandler = handler;
	}

	public void close() throws IOException {
		stop(); 
	}
	
	public synchronized void stop() {
		if (consumerThread != null) {
			consumerThread.interrupt();
			consumerThread = null;
		}
		try {
			if (this.client != null) {
				this.broker.closeClient(this.client);
			} 
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}

		try {
			if (this.replyClient != null) {
				this.broker.closeClient(this.replyClient);
			} 
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public synchronized void start(ConsumerHandler handler){
		onMessage(handler);
		start();
	}

	public synchronized void start() {
		if (consumerThread == null) {
			consumerThread = new Thread(consumerTask);
			consumerThread.setName("ConsumerThread");
		}

		if (consumerThread.isAlive())
			return;
		consumerThread.start();
	}

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	}

	public static interface ConsumerHandler{
		void handle(Message msg, Consumer consumer) throws IOException;
	}
}
