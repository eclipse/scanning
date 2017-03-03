/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.scanning.server.application;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IMessagingService;
import org.eclipse.scanning.server.servlet.Services;

/**
 * 
 * This application is used to start and stop data acquisition objects
 * inside an OSGi container. For instance one could start various
 * servlets for easily testing server functionality.
 * 
 * Arguments:
 * 
 * -xml   Simple spring file that creates objects
 * 
 * 
 * @author Matthew Gerring
 *
 */
public class Application implements IApplication {


	private Map<String, Object> objects;
	private CountDownLatch latch;

	@Override
	public Object start(IApplicationContext context) throws Exception {
			
		this.latch   = new CountDownLatch(1);

		final Map<?, ?>      args    = context.getArguments();
		final String[] configuration = (String[])args.get("application.args");
        
		Map<String, String> conf = new HashMap<String, String>(7);
		for (int i = 0; i < configuration.length; i++) {
			final String pkey = configuration[i];
			if (pkey.startsWith("-")) {
				if (i<configuration.length-1) {
				    conf.put(pkey.substring(1), configuration[i+1]);
				} else {
					conf.put(pkey.substring(1), null);
				}
			}
		}
		
		if (conf.containsKey("startActiveMQ")) {
			IMessagingService mservice = Services.getMessagingService();
			String uri = System.getProperty("org.eclipse.scanning.broker.uri");
			mservice.start(uri);
		}
		
		PseudoSpringParser parser = new PseudoSpringParser();
		this.objects = parser.parse(conf.get("xml"));
		latch.await();
		
		return objects;
	}


	@Override
	public void stop() {
		
		if (objects!=null) for (Object object : objects.values()) {
			try {
				Method disconnect = object.getClass().getMethod("disconnect");
				disconnect.invoke(object);
			} catch (Exception ne) {
				continue;
			}
		}
		IMessagingService mservice = Services.getMessagingService();
		try {
			mservice.stop();
		} catch (EventException e) {
			e.printStackTrace();
		}
		latch.countDown();
	}
	

}
