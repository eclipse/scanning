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
package org.eclipse.scanning.api.malcolm.event;

import java.util.EventObject;

/**
 * Event object used notify of Malcolm events inside
 * same VM as IMalcolmService. A workaround for connecting to 
 * JMS and getting the topic if one does not want to deal with
 * JMS.
 * 
 * @author Matthew Gerring
 *
 */
public class MalcolmEvent<T> extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1945090670224642819L;

	public MalcolmEvent(T bean) {
		super(bean);
	}
	
	/**
	 * Convenience method
	 * @return
	 */
	public T getBean() {
		return (T)getSource();
	}

}
