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
package org.eclipse.scanning.api.scan.process;

import org.eclipse.scanning.api.event.scan.ScanRequest;

/**
 * 
<pre>
<b>pre·proc·es·sor</b>
<i>noun</i>
A computer program that modifies data to conform with the input requirements of another program.
</pre>
 * <p>
 * A preprocessor which makes a ScanRequest into one which matches the available hardware for a given beamline.
 * <p>
 * Preprocessors can be registered as OSGi services (perhaps in Spring config files, using the OSGiServiceRegister
 * class) and will then be used by the ScanServlet to process scan requests before they are run.
 * 
 * @author Matthew Gerring
 *
 */
public interface IPreprocessor {

	/**
	 * Preprocessor name.
	 * 
	 * @return
	 */
	// TODO decide if this is still useful when not using extension points
	String getName();
	
	/**
	 * Call to run the preprocessor, read the available hardware and construct a legal
	 * ScanRequest
	 * 
	 * @param req The request sent by the user interface
	 * @return The processed request. The processor might modify the ScanRequest in place and return it or return an entirely new request.
	 */
	<T> ScanRequest<T> preprocess(ScanRequest<T> req) throws ProcessingException;
}
