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
package org.eclipse.scanning.event.ui.view;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Properties;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.queues.QueueViews;
import org.eclipse.scanning.event.ui.Activator;
import org.eclipse.ui.part.ViewPart;

public abstract class EventConnectionView extends ViewPart {

	protected Properties                        idProperties;
	
	protected String getRequestName() {
		final String rName = getSecondaryIdAttribute("requestName");
		if (rName != null) return rName;
		return IEventService.DEVICE_REQUEST_TOPIC;
	}

	protected String getResponseName() {
		final String rName = getSecondaryIdAttribute("responseName");
		if (rName != null) return rName;
		return IEventService.DEVICE_RESPONSE_TOPIC;
	}

	protected String getTopicName() {
		final String topicName = getSecondaryIdAttribute("topicName");
		if (topicName != null) return topicName;
		return "scisoft.default.STATUS_TOPIC";
	}

    protected URI getUri() throws Exception {
		return new URI(getUriString());
	}
    
    protected String getUriString() {
		final String uri = getSecondaryIdAttribute("uri");
		if (uri != null) {
			try {
				return URLDecoder.decode(uri, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return uri;
			}
		}
		return Activator.getJmsUri();
	}
  
    protected String getUserName() {
		final String name = getSecondaryIdAttribute("userName");
		if (name != null) return name;
		return System.getProperty("user.name");
	}
   
    protected String getCommandPreference(String key) {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    	return store.getString(key);
    }

	protected String getQueueName() {
		final String qName =  getSecondaryIdAttribute("queueName");
		if (qName != null) return qName;
		return "scisoft.default.STATUS_QUEUE";
	}
	
	protected String getSubmissionQueueName() {
		final String qName =  getSecondaryIdAttribute("submissionQueueName");
		if (qName != null) return qName;
		return "scisoft.default.SUBMISSION_QUEUE";
	}

	protected String getSubmitOverrideSetName() {
		return getSubmissionQueueName()+".overrideSet";
	}

	public void setIdProperties(String propertiesId) {
		Properties idProperties = parseString(propertiesId);
		setIdProperties(idProperties);
	}

	public void setIdProperties(Properties properties) {
		idProperties = properties;
	}
	
	public static String createSecondaryId(final String beanBundleName, final String beanClassName, final String queueName, final String topicName, final String submissionQueueName) {
        return QueueViews.createSecondaryId(beanBundleName, beanClassName, queueName, topicName, submissionQueueName);
	}
	
	public static String createSecondaryId(final String uri, final String beanBundleName, final String beanClassName, final String queueName, final String topicName, final String submissionQueueName) {
		return QueueViews.createSecondaryId(uri, beanBundleName, beanClassName, queueName, topicName, submissionQueueName);
	}

	
	protected String getSecondaryIdAttribute(String key) {
		if (idProperties!=null) return idProperties.getProperty(key);
		if (getViewSite()==null) return null;
		final String secondId = getViewSite().getSecondaryId();
		if (secondId == null) return null;
		idProperties = parseString(secondId);
		return idProperties.getProperty(key);
	}

	/**
	 * String to be parsed to properties. In the form of key=value pairs
	 * separated by semi colons. You may not use semi-colons in the 
	 * keys or values. Keys and values are trimmed so extra spaces will be
	 * ignored.
	 * 
	 * @param secondId
	 * @return map of values extracted from the 
	 */
	protected static Properties parseString(String properties) {
		
		if (properties==null) return new Properties();
		Properties props = new Properties();
		final String[] split = properties.split(";");
		for (String line : split) {
			final String[] kv = line.split("=", 2);
			props.setProperty(kv[0].trim(), kv[1].trim());
		}
		return props;
	}

}
