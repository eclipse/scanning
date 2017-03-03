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
package org.eclipse.scanning.api.event.alive;

import org.eclipse.scanning.api.INameable;

/**
 * 
 * Bean send on the Constants.ALIVE_TOPIC to notify every now and then
 * 
 * 
 * @author Matthew Gerring
 * @deprecated replaced by HeartbeatBean. This is here in case new clients look at old consumers.
 */
public class ConsumerBean implements INameable {

	public static final ConsumerBean EMPTY = new ConsumerBean();
	
	private ConsumerStatus status;
	private String         name;
	private String         version;
	private String         consumerId;
	private long           startTime;
	private long           lastAlive;
	private String         hostName;
	private String         message;
	
	public ConsumerStatus getStatus() {
		return status;
	}
	public void setStatus(ConsumerStatus status) {
		this.status = status;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getConsumerId() {
		return consumerId;
	}
	public void setConsumerId(String consumerId) {
		this.consumerId = consumerId;
	}
	
	public HeartbeatBean toHeartbeat() {
		HeartbeatBean ret = new HeartbeatBean();
		ret.setConsumerName(getName());
		ret.setConsumerStatus(getStatus());
		ret.setConceptionTime(getStartTime());
		ret.setLastAlive(getLastAlive());
		ret.setUniqueId(consumerId);
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((consumerId == null) ? 0 : consumerId.hashCode());
		result = prime * result
				+ ((hostName == null) ? 0 : hostName.hashCode());
		result = prime * result + (int) (lastAlive ^ (lastAlive >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (startTime ^ (startTime >>> 32));
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		ConsumerBean other = (ConsumerBean) obj;
		if (consumerId == null) {
			if (other.consumerId != null)
				return false;
		} else if (!consumerId.equals(other.consumerId))
			return false;
		if (hostName == null) {
			if (other.hostName != null)
				return false;
		} else if (!hostName.equals(other.hostName))
			return false;
		if (lastAlive != other.lastAlive)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (startTime != other.startTime)
			return false;
		if (status != other.status)
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getLastAlive() {
		return lastAlive;
	}
	public void setLastAlive(long lastAlive) {
		this.lastAlive = lastAlive;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
