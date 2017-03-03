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
package org.eclipse.scanning.api.malcolm.message;

import java.util.Map;

/**
 * Class used to define malcolm object which 
 * sends information to the server.
 * 
 * @author Matthew Gerring
 * 
 * @internal Internal use only
 * 
 * TODO FIXME Tom Cobb changed the spec around a lot.
 * Some methods may not be required anymore.
 * 
 */
public class MalcolmMessage {

	private Type   type;
	
	private long   id;
	private String param;
	private String endpoint;
	private String method;
	private String message;
	private Object arguments;
	private Object value;
	private Object rawValue;
	
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getParam() {
		return param;
	}
	public void setParam(String param) {
		this.param = param;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arguments == null) ? 0 : arguments.hashCode());
		result = prime * result
				+ ((endpoint == null) ? 0 : endpoint.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((param == null) ? 0 : param.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((rawValue == null) ? 0 : rawValue.hashCode());
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
		MalcolmMessage other = (MalcolmMessage) obj;
		if (arguments == null) {
			if (other.arguments != null)
				return false;
		} else if (!arguments.equals(other.arguments))
			return false;
		if (endpoint == null) {
			if (other.endpoint != null)
				return false;
		} else if (!endpoint.equals(other.endpoint))
			return false;
		if (id != other.id)
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (param == null) {
			if (other.param != null)
				return false;
		} else if (!param.equals(other.param))
			return false;
		if (type != other.type)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		if (rawValue == null) {
			if (other.rawValue != null)
				return false;
		} else if (!rawValue.equals(other.rawValue))
			return false;
		return true;
	}
	public Object getArguments() {
		return arguments;
	}
	public void setArguments(Object args) {
		this.arguments = args;
	}
	@Override
	public String toString() {
		return "MalcolmMessage [type=" + type + ", id=" + id + ", param="
				+ param + ", endpoint=" + endpoint + ", method=" + method
				+ ", message=" + message + ", args=" + arguments + ", value="
				+ value + "]";
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object val) {
		this.value = val;
	}
	
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getMessage() {
		if (message!=null) return message;
		if (value!=null && value instanceof Map) {
			if (((Map)value).containsKey("message")) {
				return (String)((Map)value).get("message");
			}
		}
		return null;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endPoint) {
		this.endpoint = endPoint;
	}
	public Object getRawValue() {
		return rawValue;
	}
	public void setRawValue(Object rawValue) {
		this.rawValue = rawValue;
	}
}
