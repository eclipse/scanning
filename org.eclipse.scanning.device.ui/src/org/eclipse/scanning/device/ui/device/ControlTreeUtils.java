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
package org.eclipse.scanning.device.ui.device;

import org.eclipse.core.runtime.Platform;
import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.scanning.api.ISpringParser;
import org.eclipse.scanning.api.scan.ui.ControlTree;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlTreeUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(ControlTreeUtils.class);

	/** 
	 * We ensure that the xml is parsed, if any
	 * Hopefully this has already been done by
	 * the client spring xml configuration but
	 * if not we check if there is an xml argument
	 * here and attempt to load its path.
	 * This step is done for testing and to make
	 * the example client work. 
	 **/
	public static final ControlTree parseDefaultXML() {
		
		if (ControlTree.getInstance()!=null) return ControlTree.getInstance();
		String[] args = Platform.getApplicationArgs();
		for (int i = 0; i < args.length; i++) {
			final String arg = args[i];
			if (arg.equals("-xml")) {
				String path = args[i+1];
				ISpringParser parser = ServiceHolder.getSpringParser();
				try {
					parser.parse(path);
				} catch (Exception e) {
					logger.error("Unabled to parse: "+path, e);
				}
				break;
			}
		}
		return ControlTree.getInstance();
	}

    
	public static final <T> T clone(T tree) throws Exception {
		IMarshallerService mservice = ServiceHolder.getMarshallerService();
		String      json = mservice.marshal(tree);
		T clone = mservice.unmarshal(json, (Class<T>)tree.getClass());
		return clone;
	}

}
