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
package org.eclipse.scanning.sequencer.analysis;

import java.text.MessageFormat;

import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.scanning.api.IScanAttributeContainer;

/**
 * 
 * Class to deal with attribute registration into the nexus file.
 * 
 * Can be used as delegate or static method.
 * 
 * @author Matthew Gerring
 *
 */
public class Attributes {

	private IScanAttributeContainer container;

	public Attributes(IScanAttributeContainer container) {
		this.container = container;
	}
	
	public void registerAttributes(NXobject nexusObject) throws Exception {
		registerAttributes(nexusObject, container);
	}
	
	/**
	 * Add the attributes for the given attribute container into the given nexus object.
	 * @param nexusObject
	 * @param container
	 * @throws NexusException 
	 */
	public static void registerAttributes(NXobject nexusObject, IScanAttributeContainer container) throws NexusException {
		// We create the attributes, if any
		nexusObject.setField("name", container.getName());
		if (container.getScanAttributeNames()!=null) for(String attrName : container.getScanAttributeNames()) {
			try {
				nexusObject.setField(attrName, container.getScanAttribute(attrName));
			} catch (Exception e) {
				throw new NexusException(MessageFormat.format(
						"An exception occurred attempting to get the value of the attribute ''{0}'' for the device ''{1}''",
						container.getName(), attrName));
			}
		}
	}

}
