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
package org.eclipse.scanning.test;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URI;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.usage.SystemUsage;

public class BrokerDelegate {

	private URI           uri;     
	private BrokerService service;

	public void start() throws Exception {
		uri = createUri(); // Each test uses a new port if the port is running on another test.
		System.setProperty("org.eclipse.scanning.broker.uri", uri.toString());
        service = new BrokerService();
        service.addConnector(uri);
        service.setPersistent(false); 
        SystemUsage systemUsage = service.getSystemUsage();
        systemUsage.getStoreUsage().setLimit(1024 * 1024 * 8);
        systemUsage.getTempUsage().setLimit(1024 * 1024 * 8);
        service.start();
		boolean ok = service.waitUntilStarted();
		if (!ok) throw new Exception("Broker was not started properly!");
	}

	public void stop() throws Exception {
		
		if (service!=null) {
			service.stop();
			service.waitUntilStopped();
			service = null;
		}
	}
	
	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	
	private static URI createUri() {
		try {
			return new URI("tcp://localhost:"+getFreePort());
		} catch (Exception ne) {
			ne.printStackTrace();
		    return null;
		}
	}

	private static int getFreePort() {
		final int start = 8619+Math.round((float)Math.random()*100);
		return getFreePort(start);
	}

	
	private static int getFreePort(final int startPort) {

	    int port = startPort;
	    while(!isPortFree(port)) port++;

	    return port;
	}
	/**
	 * Checks if a port is free.
	 * @param port
	 * @return
	 */
	public static boolean isPortFree(int port) {

	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	    return false;
	}


}
