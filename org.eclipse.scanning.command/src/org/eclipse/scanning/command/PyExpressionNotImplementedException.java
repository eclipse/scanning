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
package org.eclipse.scanning.command;

public class PyExpressionNotImplementedException extends Exception {

	private static final long serialVersionUID = 6008079465533652603L;

	public PyExpressionNotImplementedException() { }

	public PyExpressionNotImplementedException(String message) {
		super(message);
	}

	public PyExpressionNotImplementedException(Throwable cause) {
		super(cause);
	}

	public PyExpressionNotImplementedException(String message,
			Throwable cause) {
		super(message, cause);
	}

	public PyExpressionNotImplementedException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
