package io.zbus.mq;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.transport.Message;
import io.zbus.transport.Session;
import io.zbus.transport.http.Http;

/**
 * 
 * DMZ proxy, sending notification to proxy client
 * 
 * @author Rushmore
 *
 */
public class NotifyManager { 
	private static final Logger logger = LoggerFactory.getLogger(NotifyManager.class); 
	private Map<NotifyTarget, Map<String, Session>> targetSessionTable = new ConcurrentHashMap<>();
	
	public NotifyManager() {
		
	} 
	
	public void notifyClients(String port) {
		notifyClients(port, null);
	}
	
	public Map<NotifyTarget, Map<String, Session>> getTargetSessionTable() {
		return this.targetSessionTable;
	}
	
	public synchronized void notifyClients(String port, String urlPrefix) {
		Iterator<Entry<NotifyTarget, Map<String, Session>>> iter = targetSessionTable.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<NotifyTarget, Map<String, Session>> e = iter.next();
			NotifyTarget target = e.getKey();
			Map<String, Session> table = e.getValue(); 
			for(Session session : table.values()) {
				Message msg = new Message();
				msg.setBody(target);
				msg.setHeader(Http.CONTENT_TYPE, "application/json; charset=utf8");
				try {
					session.write(msg);
				} catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
		}
	}

	public void addNotifyTarget(String port, Session session) {
		addNotifyTarget(port, null , session);
	}
	
	public synchronized void addNotifyTarget(String port, String urlPrefix, Session session) {
		NotifyTarget target = new NotifyTarget();
		target.port = port;
		target.urlPrefix = urlPrefix;
		
		Map<String, Session> sessionTable = targetSessionTable.get(target);
		if(sessionTable == null) {
			sessionTable = new ConcurrentHashMap<>();
		}
		sessionTable.put(session.id(), session);
		
		System.out.println(sessionTable);
	}
	
	public synchronized void removeSession(Session session) {  
		Iterator<Entry<NotifyTarget, Map<String, Session>>> iter = targetSessionTable.entrySet().iterator();
		while(iter.hasNext()) {
			Map<String, Session> table = iter.next().getValue();
			table.remove(session.id());
			if(table.isEmpty()) {
				iter.remove();
			}
		}
	}
	
	public static class NotifyTarget {
		public String port;
		public String urlPrefix;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((port == null) ? 0 : port.hashCode());
			result = prime * result + ((urlPrefix == null) ? 0 : urlPrefix.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NotifyTarget other = (NotifyTarget) obj;
			if (port == null) {
				if (other.port != null)
					return false;
			} else if (!port.equals(other.port))
				return false;
			if (urlPrefix == null) {
				if (other.urlPrefix != null)
					return false;
			} else if (!urlPrefix.equals(other.urlPrefix))
				return false;
			return true;
		}
	}
}
