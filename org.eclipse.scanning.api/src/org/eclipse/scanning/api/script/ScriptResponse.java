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
package org.eclipse.scanning.api.script;

/**
 * Class which encapsulates a response one, none or all of the return
 * information may be set.
 * 
 * @author Matthew Gerring
 * @param <T> must be type of object that the script returns. 
 *        Many script types cannot return an object, so this is optional to declare.
 *        The object returned should json serialize, it may be sent in events to the client
 *        and should ideally encode information rather than provide callable server methods. 
 */
public class ScriptResponse<T> {

	private int    returnCode=0;
	private T      returnObject;
	private String message;
	
	public int getReturnCode() {
		return returnCode;
	}
	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
	public T getReturnObject() {
		return returnObject;
	}
	public void setReturnObject(T returnObject) {
		this.returnObject = returnObject;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + returnCode;
		result = prime * result + ((returnObject == null) ? 0 : returnObject.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		ScriptResponse other = (ScriptResponse) obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (returnCode != other.returnCode)
			return false;
		if (returnObject == null) {
			if (other.returnObject != null)
				return false;
		} else if (!returnObject.equals(other.returnObject))
			return false;
		return true;
	}
}
