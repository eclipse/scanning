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
package org.eclipse.scanning.api.event.core;

import java.net.URI;

/**
 * 
 * A simple bean to hold the information about a queue.
 * 
 * @author Matthew Gerring
 *
 */
public class ConsumerConfiguration {

	private URI    uri;
	private String submissionQueue;
	private String statusTopic;
	private String statusSet;
	
	public ConsumerConfiguration() {
		
	}
	
	public ConsumerConfiguration(URI uri, String submissionQueue, String statusTopic, String statusSet) {
		super();
		this.uri = uri;
		this.submissionQueue = submissionQueue;
		this.statusTopic = statusTopic;
		this.statusSet = statusSet;
	}

	public URI getUri() {
		return uri;
	}
	public void setUri(URI uri) {
		this.uri = uri;
	}
	public String getSubmissionQueue() {
		return submissionQueue;
	}
	public void setSubmissionQueue(String submissionQueue) {
		this.submissionQueue = submissionQueue;
	}
	public String getStatusTopic() {
		return statusTopic;
	}
	public void setStatusTopic(String statusTopic) {
		this.statusTopic = statusTopic;
	}
	public String getStatusSet() {
		return statusSet;
	}
	public void setStatusSet(String statusSet) {
		this.statusSet = statusSet;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((statusSet == null) ? 0 : statusSet.hashCode());
		result = prime * result + ((statusTopic == null) ? 0 : statusTopic.hashCode());
		result = prime * result + ((submissionQueue == null) ? 0 : submissionQueue.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		ConsumerConfiguration other = (ConsumerConfiguration) obj;
		if (statusSet == null) {
			if (other.statusSet != null)
				return false;
		} else if (!statusSet.equals(other.statusSet))
			return false;
		if (statusTopic == null) {
			if (other.statusTopic != null)
				return false;
		} else if (!statusTopic.equals(other.statusTopic))
			return false;
		if (submissionQueue == null) {
			if (other.submissionQueue != null)
				return false;
		} else if (!submissionQueue.equals(other.submissionQueue))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
	
	
}
