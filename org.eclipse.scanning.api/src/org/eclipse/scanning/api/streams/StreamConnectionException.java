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
package org.eclipse.scanning.api.streams;

/**
 * A check exception to define stream connection problems.
 * @author fcp94556
 *
 */
public class StreamConnectionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3374901989954371016L;

	public StreamConnectionException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public StreamConnectionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	public StreamConnectionException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public StreamConnectionException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public StreamConnectionException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
