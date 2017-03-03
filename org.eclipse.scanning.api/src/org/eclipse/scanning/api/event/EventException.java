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
package org.eclipse.scanning.api.event;

public class EventException extends Exception {

	private static final long serialVersionUID = -6644630244716886256L;

	public EventException() {
		super();
	}

	public EventException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public EventException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public EventException(String message) {
		super(message);
	}

	public EventException(Throwable throwable) {
		super(throwable);
	}

}
