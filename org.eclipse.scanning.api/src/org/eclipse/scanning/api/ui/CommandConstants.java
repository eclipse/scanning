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
package org.eclipse.scanning.api.ui;

public class CommandConstants {

	public static final String JMS_URI               = "org.dawnsci.commandserver.URI";
	public static final String DIR_CHECKING_URI      = "org.dawnsci.commandserver.xia2.directory.checkingURL";
	public static final String DEFAULT_JMS_URI       = "tcp://sci-serv5.diamond.ac.uk:61616";
	public static final String DEFAULT_CHECKING_URI  = "http://cs04r-sc-vserv-45.diamond.ac.uk:8619";
	
	
	/**
	 * Attempts to read the probable broker URI from the system configuration.
	 * May return null.
	 * @return
	 */
	public static final String getScanningBrokerUri() {
		String uri = null;
	    if (uri == null) uri = System.getProperty("org.eclipse.scanning.broker.uri");
	    if (uri == null) uri = System.getProperty("GDA/gda.activemq.broker.uri"); // GDA specific but not a compilation dependency.
	    if (uri == null) uri = System.getProperty("gda.activemq.broker.uri"); // GDA specific but not a compilation dependency.		
		return uri;
	}

}
