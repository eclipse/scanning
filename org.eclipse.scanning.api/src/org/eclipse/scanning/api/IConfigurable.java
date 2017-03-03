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
package org.eclipse.scanning.api;

import org.eclipse.scanning.api.scan.ScanningException;

/**
 * 
 * Implementors of this interface may be configured with a model.
 * 
 * @author Matthew Gerring
 *
 */
public interface IConfigurable<M> {

	/**
	 * Call to configure the device. If the model provided is
	 * invalid, a scanning exception will be thrown.
	 * 
	 * @param model
	 * @throws ScanningException
	 */
	void configure(M model) throws ScanningException;
}
