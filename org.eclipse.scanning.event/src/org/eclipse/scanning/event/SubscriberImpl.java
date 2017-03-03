/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.event;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.commons.lang.ClassUtils;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.HeartbeatEvent;
import org.eclipse.scanning.api.event.alive.IHeartbeatListener;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.IScanListener;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanEvent;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.scan.event.ILocationListener;
import org.eclipse.scanning.api.scan.event.Location;
import org.eclipse.scanning.api.scan.event.LocationEvent;
import org.eclipse.scanning.event.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
class SubscriberImpl<T extends EventListener> extends AbstractConnection implements ISubscriber<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(SubscriberImpl.class);
	
	private static String DEFAULT_KEY = UUID.randomUUID().toString(); // Does not really matter what key is used for the default collection.

	private Map<String, Collection<T>>    slisteners; // Scan listeners
	private Map<Class, DiseminateHandler> dMap;
	private BlockingQueue<DiseminateEvent>  queue;
	
	private MessageConsumer scanConsumer, hearbeatConsumer;
	
	private boolean synchronous = true;
	
	public SubscriberImpl(URI uri, String topic, IEventConnectorService service) {
		super(uri, topic, service);
		slisteners = new ConcurrentHashMap<String, Collection<T>>(31); // Concurrent overkill?
		dMap       = createDiseminateHandlers();
	}

	@Override
	public void addListener(T listener) throws EventException {
		addListener(DEFAULT_KEY, listener);
	}

	@Override
	public void addListener(String scanID, T listener) throws EventException{
		setConnected(true);
		if (isSynchronous()) createDiseminateThread();
		registerListener(scanID, listener, slisteners);
		if (scanConsumer == null) {
			try {
				Class<?> beanClass = listener instanceof IBeanListener ? ((IBeanListener)listener).getBeanClass() : null;
				scanConsumer = createConsumer(getTopicName(), beanClass);
			} catch (JMSException e) {
				throw new EventException("Cannot subscribe to topic "+getTopicName()+" with URI "+uri, e);
			}
		}
	}
	
	private MessageConsumer createConsumer(final String    topicName, 
			                               final Class<?>  beanClass) throws JMSException {
		
		Topic topic = super.createTopic(topicName);
		

       	final MessageConsumer consumer = session.createConsumer(topic);
    	MessageListener listener = new MessageListener() {
    		public void onMessage(Message message) {
    			
    			TextMessage txt   = (TextMessage)message;
    			try {
	    			String      json  = txt.getText(); 
	    			json = JsonUtil.removeProperties(json, properties);
	    			try {
	
		    			Object bean = service.unmarshal(json, beanClass);
		    			schedule(new DiseminateEvent(bean));
		    			
	    			} catch (Exception ne) {
	    				if (ne.getClass().getName().contains("com.fasterxml.jackson")) {
	        				logger.error("JSON Serialization Error!", ne);
		   				} else {
		    				logger.error("Internal error! - Unable to process an event!", ne);
		   				}
	    				ne.printStackTrace(); // Unit tests without log4j config show this one.
	     			}
    			} catch (JMSException ne) {
    				logger.error("Cannot get text from message "+txt, ne);
    			}
    		}
    	};
    	consumer.setMessageListener(listener);
        return consumer;
	}
	
	private void schedule(DiseminateEvent event) {
		if (isSynchronous()) {
		    if (queue!=null) queue.add(event);
		} else {
			if (event==DiseminateEvent.STOP) return;
			// TODO FIXME Might not be right...
			final Thread thread = new Thread("Execute event "+getTopicName()) {
				public void run() {
					diseminate(event); // Use this JMS thread directly to do work.
				}
			};
			thread.setDaemon(true);
			thread.setPriority(Thread.NORM_PRIORITY+1);
			thread.start();
		}
	}


	private void createDiseminateThread() {
		
		if (!isSynchronous()) return; // If asynch we do not run events in order and wait until they return.
		if (queue!=null) return;
		queue      = new LinkedBlockingQueue<>(); // Small, if they do work and things back-up, exceptions will occur.
		
		final Thread despachter = new Thread(new Runnable() {
			public void run() {
				while(isConnected()) {
					try {
						DiseminateEvent event = queue.take();
						if (event==DiseminateEvent.STOP) return;					
						diseminate(event);
						
					} catch (RuntimeException e) {
						e.printStackTrace();
						logger.error("RuntimeException occured despatching event", e);
						continue;
						
					} catch (Exception e) {
						e.printStackTrace();
						logger.error("Stopping event despatch thread ", e);
						return;
					}
				}
				System.out.println(Thread.currentThread().getName()+" disconnecting events.");
			}
		}, "Submitter despatch thread "+getSubmitQueueName());
		despachter.setDaemon(true);
		despachter.setPriority(Thread.NORM_PRIORITY+1);
		despachter.start();
	}

	
	private final static class DiseminateEvent {

		public static final DiseminateEvent STOP = new DiseminateEvent("STOP");
		
		protected final Object bean;

		public DiseminateEvent(Object bean) {
			this.bean      = bean;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((bean == null) ? 0 : bean.hashCode());
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
			DiseminateEvent other = (DiseminateEvent) obj;
			if (bean == null) {
				if (other.bean != null)
					return false;
			} else if (!bean.equals(other.bean))
				return false;
			return true;
		}

	}
	
	private void diseminate(DiseminateEvent event) {
		Object bean = event.bean;
		diseminate(bean, slisteners.get(DEFAULT_KEY));  // general listeners
		if (bean instanceof IdBean) {
			IdBean idBean = (IdBean)bean;
		    diseminate(bean, slisteners.get(idBean.getUniqueId())); // scan specific listeners, if any
		} else if (bean instanceof INameable) {
			INameable namedBean = (INameable)bean;
		    diseminate(bean, slisteners.get(namedBean.getName())); // scan specific listeners, if any
		}
	}

	private boolean diseminate(Object bean, Collection<T> listeners) {
		
		if (listeners==null)     return false;
		if (listeners.isEmpty()) return false;
		final EventListener[] ls = listeners.toArray(new EventListener[listeners.size()]);
		
		boolean ret = true;
		for (EventListener listener : ls) {
			
			@SuppressWarnings("unchecked")
			List<Class<?>> types = getAllInterfaces(listener.getClass());
			boolean diseminated = false;
			for (Class<?> type : types) {
				DiseminateHandler handler = dMap.get(type);
				if (handler==null) continue;
				handler.diseminate(bean, listener);
				diseminated = true;
			}
			ret =  ret && diseminated;
		}
		return ret;
	}

	private Map<Class<? extends EventListener>,List<Class<?>>> interfaces;
	
	/**
	 * Important to cache the interfaces. Getting them caused a bug where scannable
	 * values were slow to transmit to the client during a scan.
	 * 
	 * @param class1
	 * @return
	 */
	private List<Class<?>> getAllInterfaces(Class<? extends EventListener> class1) {
		if (interfaces==null) interfaces = new HashMap<>();
		if (!interfaces.containsKey(class1)) {
			interfaces.put(class1, ClassUtils.getAllInterfaces(class1));
		}
		return interfaces.get(class1);
	}

	private Map<Class, DiseminateHandler> createDiseminateHandlers() {
		
		Map<Class, DiseminateHandler> ret = Collections.synchronizedMap(new HashMap<Class, DiseminateHandler>(3));
		
		ret.put(IScanListener.class, new DiseminateHandler() {
			public void diseminate(Object bean, EventListener e) {
				
				if (!(bean instanceof ScanBean)) return; 
				// This listener must be used with events publishing ScanBean
				// If your scan does not publish ScanBean events then you
				// may listen to it with a standard IBeanListener.
				
				// Used casting because generics got silly
				ScanBean sbean  = (ScanBean)bean;
				IScanListener l = (IScanListener)e;
				
				DeviceState now = sbean.getDeviceState();
				DeviceState was = sbean.getPreviousDeviceState();
				if (now!=null && now!=was) {
					execute(new DespatchEvent(l, new ScanEvent(sbean), true));
					return;
				} else {
					Status snow = sbean.getStatus();
					Status swas = sbean.getPreviousStatus();
					if (snow!=null && snow!=swas && swas!=null) {
						execute(new DespatchEvent(l, new ScanEvent(sbean), true));
						return;
					}
				}		
				execute(new DespatchEvent(l, new ScanEvent(sbean), false));
			}
		});
		ret.put(IHeartbeatListener.class, new DiseminateHandler() {
			public void diseminate(Object bean, EventListener e) {
				// Used casting because generics got silly
				HeartbeatBean hbean = (HeartbeatBean)bean;
				IHeartbeatListener l= (IHeartbeatListener)e;
				execute(new DespatchEvent(l, new HeartbeatEvent(hbean)));
			}
		});
		ret.put(IBeanListener.class, new DiseminateHandler() {
			public void diseminate(Object bean, EventListener e) {
				// Used casting because generics got silly
				@SuppressWarnings("unchecked")
				IBeanListener<Object> l = (IBeanListener<Object>)e;
				execute(new DespatchEvent(l, new BeanEvent<Object>(bean)));
			}
		});
		ret.put(ILocationListener.class, new DiseminateHandler() {
			public void diseminate(Object bean, EventListener e) {
				// Used casting because generics got silly
				ILocationListener l = (ILocationListener)e;
				execute(new DespatchEvent(l, new LocationEvent((Location)bean)));
			}
		});



		return ret;
	}


	private interface DiseminateHandler {
		public void diseminate(Object bean, EventListener listener) throws ClassCastException;
	}


	private void registerListener(String key, T listener, Map<String, Collection<T>> listeners) {
		Collection<T> ls = listeners.get(key);
		if (ls == null) {
			ls = new LinkedHashSet<T>(3);
			listeners.put(key.toString(), ls);
		}
		ls.add(listener);
	}

	@Override
	public void removeListener(T listener) {
		removeListener(DEFAULT_KEY, listener);
	}

	@Override
	public void removeListener(String id, T listener) {
		if (slisteners.containsKey(id)) {
			slisteners.get(id).remove(listener);
		}
	}
	
	@Override
	public void removeListeners(String id) {
		slisteners.remove(id);
	}
	
	@Override
	public void clear() {
		slisteners.clear();
	}

	@Override
	public void disconnect() throws EventException {
		try {
			clear();
			if (scanConsumer!=null)     scanConsumer.close();
			if (hearbeatConsumer!=null) hearbeatConsumer.close();
			
			super.disconnect();
			
		} catch (JMSException ne) {
			throw new EventException("Internal error - unable to close connection!", ne);
		
		} finally {
			scanConsumer = null;
			hearbeatConsumer = null;
			setConnected(false);
		}
		super.disconnect();
		schedule(DiseminateEvent.STOP);
	}
	
	protected boolean isListenersEmpty() {
		return slisteners.isEmpty();
	}
	
	private boolean connected; 
	
	private void execute(DespatchEvent event) {
		
		if (event.listener instanceof IHeartbeatListener) ((IHeartbeatListener)event.listener).heartbeatPerformed((HeartbeatEvent)event.object);
		if (event.listener instanceof IBeanListener)      ((IBeanListener)event.listener).beanChangePerformed((BeanEvent)event.object);
		if (event.listener instanceof ILocationListener)  ((ILocationListener)event.listener).locationPerformed((LocationEvent)event.object);
		if (event.listener instanceof IScanListener){
			IScanListener l = (IScanListener)event.listener;
			ScanEvent     e = (ScanEvent)event.object;
			if (event.isStateChange()) {
				l.scanStateChanged(e);
			} else {
				l.scanEventPerformed(e);
			}
		}
	}

	/**
	 * Immutable event for queue.
	 * 
	 * @author fcp94556
	 *
	 */
	private static class DespatchEvent {
		
		protected final EventListener listener;
		protected final EventObject   object;
		protected final boolean       isStateChange;
		
		public DespatchEvent(EventListener listener, EventObject object) {
			this(listener, object, false);
		}
		public DespatchEvent(EventListener listener2, EventObject object2, boolean b) {
			this.listener = listener2;
			this.object   = object2;
			this.isStateChange = b;
		}
		public boolean isStateChange() {
			return isStateChange;
		}

	}


	public boolean isConnected() {
		return connected;
	}

	private void setConnected(boolean connected) {
		this.connected = connected;
	}

	public boolean isSynchronous() {
		return synchronous;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	private List<String> properties;


	@Override
	public void addProperty(String name, FilterAction... actions) {
		for (FilterAction fa : actions) if (fa!=FilterAction.DELETE) throw new IllegalArgumentException("It is only possible to remove properties from the subscribed json right now");
	    if (properties == null) properties = new ArrayList<>(7);
	    properties.add(name);
 	}

	@Override
	public void removeProperty(String name) {
	    if (properties == null) return;
	    properties.remove(name);
	}

	@Override
	public List<String> getProperties() {
		return properties;
	}
}
