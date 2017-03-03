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
package org.eclipse.scanning.api.event.queues;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.eclipse.scanning.api.event.EventConstants;

/**
 * A class to manage the e3 ids of views which will look at the queue.
 * At the moment the connection options for which queue to open are
 * configured into the secondary id for the view. 
 * 
 * A better design in future would be to have a service which manages
 * the connection options and secondary id simply becomes a name for the
 * options in the service. For instance 'ScanQueue' might give a StatusQueueView
 * looking at the control machine messaging URL, looking for ScanBeans and using
 * the default scan queue name and topic.
 * 
 * @author Matthew Gerring
 *
 */
public class QueueViews {

	/**
	 * 
	 * @return
	 */
	public static final String getQueueViewID() {
		return "org.eclipse.scanning.event.ui.queueView"; // Might need to move this to a preference or other property.
	}
	
	/**
	 * 
	 * @param uri
	 * @param bundle
	 * @param className
	 * @param partName
	 * @return
	 */
	public static String createId(String uri, String bundle, String className, String partName) {
		return createId(uri, bundle, className, EventConstants.STATUS_SET, EventConstants.STATUS_TOPIC, EventConstants.SUBMISSION_QUEUE, partName);
	}
	
	/**
	 * 
	 * @param uri
	 * @param bundle
	 * @param bean
	 * @param queueName
	 * @param topicName
	 * @param submissionQueueName
	 * @param partName
	 * @return
	 */
	public static String createId(String uri, String bundle, String bean, 
								final String queueName, 
								final String topicName, 
								final String submissionQueueName, 
								String partName)  {
							
	
		String queueViewId = QueueViews.createSecondaryId(uri, bundle,bean, queueName, topicName, submissionQueueName);
		if (partName!=null) queueViewId = queueViewId+"partName="+partName;
		
		final StringBuilder buf = new StringBuilder();
		buf.append(getQueueViewID());
		buf.append(":");
		buf.append(queueViewId);
		return buf.toString();
	}
	
	/**
	 * 
	 * @param beanBundleName
	 * @param beanClassName
	 * @param queueName
	 * @param topicName
	 * @param submissionQueueName
	 * @return
	 */
	public static String createSecondaryId(final String beanBundleName, final String beanClassName, final String queueName, final String topicName, final String submissionQueueName) {
        return createSecondaryId(null, beanBundleName, beanClassName, queueName, topicName, submissionQueueName);
	}
	
	/**
	 * 
	 * @param uri
	 * @param beanBundleName
	 * @param beanClassName
	 * @param queueName
	 * @param topicName
	 * @param submissionQueueName
	 * @return
	 */
	public static String createSecondaryId(String uri, final String beanBundleName, final String beanClassName, final String queueName, final String topicName, final String submissionQueueName) {
		
		final StringBuilder buf = new StringBuilder();
		if (uri!=null) {
			try {
				uri = URLEncoder.encode(uri, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace(); // Not fatal
			} 
			append(buf, "uri",      uri);
		}
		append(buf, "beanBundleName",      beanBundleName);
		append(buf, "beanClassName",       beanClassName);
		append(buf, "queueName",           queueName);
		append(buf, "topicName",           topicName);
		append(buf, "submissionQueueName", submissionQueueName);
		return buf.toString();
	}
	

	protected static String createSecondaryId(String uri, String requestName, String responseName) {
		final StringBuilder buf = new StringBuilder();
		if (uri!=null) {
			try {
				uri = URLEncoder.encode(uri, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace(); // Not fatal
			} 
			append(buf, "uri",      uri);
		}
		append(buf, "requestName",  requestName);
		append(buf, "responseName", responseName);
		return buf.toString();
	}


	protected static void append(StringBuilder buf, String name, String value) {
		buf.append(name);
		buf.append("=");
		buf.append(value);
		buf.append(";");
	}

}
