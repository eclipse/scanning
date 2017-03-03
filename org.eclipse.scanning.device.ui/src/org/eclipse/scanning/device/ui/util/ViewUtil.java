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
package org.eclipse.scanning.device.ui.util;

import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.queues.QueueViews;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.osgi.framework.FrameworkUtil;

public class ViewUtil {
	
    /**
     * Show the top where the mouse is.
     * @param tip
     * @param message
     */
	public static void showTip(ToolTip tip, String message) {
		
		if (tip==null) return;
    	tip.setMessage(message);
		PointerInfo a = MouseInfo.getPointerInfo();
		java.awt.Point loc = a.getLocation();
		
		tip.setLocation(loc.x, loc.y+20);
        tip.setVisible(true);
	}

	/**
	 * 
	 * @param id
	 * @param managers
	 * @param actions
	 */
	public static void addGroups(String id, List<IContributionManager> managers, IAction... actions) {
		for (IContributionManager man : managers) addGroup(id, man, actions);
	}
	
	/**
	 * 
	 * @param id
	 * @param manager
	 * @param actions
	 */
	public static  void addGroup(String id, IContributionManager manager, IAction... actions) {
		manager.add(new Separator(id));
		for (IAction action : actions) {
			if (action==null) continue;
			manager.add(action);
		}
	}

	
	public static void openQueueMonitor(Class<? extends StatusBean> beanClass, String partName) throws PartInitException, UnsupportedEncodingException {
		openQueueMonitor(beanClass, EventConstants.STATUS_SET, EventConstants.STATUS_TOPIC, EventConstants.SUBMISSION_QUEUE, partName);
	}
	
	public static void openQueueMonitor(Class<? extends StatusBean> beanClass, 
			                           final String queueName, 
			                           final String topicName, 
			                           final String submissionQueueName, 
			                           String partName) throws PartInitException, UnsupportedEncodingException {
		
		String bundle = FrameworkUtil.getBundle(beanClass).getSymbolicName();
		String bean   = beanClass.getName();
		String sqn    = queueName;
		String stn    = topicName;
		String submit = submissionQueueName;

		String queueViewId = QueueViews.createSecondaryId(CommandConstants.getScanningBrokerUri(), bundle,bean, sqn, stn, submit);
		if (partName!=null) queueViewId = queueViewId+"partName="+partName;
		try {
			PageUtil.getPage().showView(QueueViews.getQueueViewID(), queueViewId, IWorkbenchPage.VIEW_ACTIVATE);
		} catch (PartInitException e) {
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "Cannot open view", "Cannot open view "+queueViewId, 
					new Status(Status.ERROR, "org.eclipse.scanning.event.ui", e.getMessage()));
			throw e;
		}
	}

	/**
	 * Ensures that certain views exist and work has been done to load them.
	 * @param ids
	 */
	public static void createViews(String... ids) {
		for (String id : ids) {
			// Try to ensure that the model view and regions view are initialized
			IViewReference ref = PageUtil.getPage().findViewReference(id);
			if (ref!=null) ref.getView(true);
		}
	}

}
