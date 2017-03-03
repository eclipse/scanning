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

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IResponder;
import org.eclipse.scanning.api.event.core.IResponseCreator;
import org.eclipse.scanning.api.event.core.IResponseProcess;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponderImpl<T extends IdBean> extends AbstractRequestResponseConnection implements IResponder<T> {
	
	private static Logger logger = LoggerFactory.getLogger(ResponderImpl.class);
	
	private ISubscriber<IBeanListener<T>>      subscriber;
	private IPublisher<T>                      publisher;
	private IResponseCreator<T>                creator;
	private Class<T>                           beanClass;

	ResponderImpl(URI uri, String reqTopic, String resTopic, IEventService eservice) {
		super(uri, reqTopic, resTopic, eservice);
	}

	@Override
	public void setResponseCreator(IResponseCreator<T> res) throws EventException {
		
		if (subscriber!=null) throw new EventException("This responder is already connected with an IResponseCreator! Please call disconnect to stop it.");
	
		subscriber = eservice.createSubscriber(getUri(), getRequestTopic());
		publisher  = eservice.createPublisher(getUri(), getResponseTopic());
	    
		this.creator = res;

		subscriber.setSynchronous(res.isSynchronous());
		subscriber.addListener(new IBeanListener<T>() {
			@Override
			public void beanChangePerformed(BeanEvent<T> evt) {
				T request = evt.getBean();
				try {
					IResponseProcess<T> process = creator.createResponder(request, publisher);
					T response = process.process(request);
					publisher.broadcast(response);
					
				} catch (EventException ne) {
					if (ne.getCause()!=null) System.out.println(ne.getCause().getMessage()); // Sometimes logging not working here!
					logger.error("Request unable to be processed! "+request, ne);
				}
			}
			@Override
			public Class<T> getBeanClass() {
				return ResponderImpl.this.getBeanClass();
			}
		});
		
	}

	@Override
	public void disconnect() throws EventException {
		if (subscriber!=null) subscriber.disconnect();
		subscriber = null;
		if (publisher!=null) publisher.disconnect();
		publisher = null;
	}

	@Override
	public Class<T> getBeanClass() {
		return beanClass;
	}

	@Override
	public void setBeanClass(Class<T> beanClass) {
		this.beanClass = beanClass;
	}
}
