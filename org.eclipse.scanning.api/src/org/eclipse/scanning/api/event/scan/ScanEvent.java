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
package org.eclipse.scanning.api.event.scan;

import java.util.EventObject;

public class ScanEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6787226667503629937L;

	
	public ScanEvent(ScanBean bean) {
		super(bean);
	}

	public ScanBean getBean() {
		return (ScanBean)getSource();
	}
	
	public String toString() {
		return getBean().toString();
	}
}
