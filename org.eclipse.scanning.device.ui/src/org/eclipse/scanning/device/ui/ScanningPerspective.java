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
package org.eclipse.scanning.device.ui;

import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.queues.QueueViews;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.device.ui.device.DetectorView;
import org.eclipse.scanning.device.ui.model.ModelView;
import org.eclipse.scanning.device.ui.points.ScanRegionView;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.scanning.device.ui.vis.VisualiseView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.osgi.framework.FrameworkUtil;

public class ScanningPerspective implements IPerspectiveFactory {

	/**
	 * Creates the initial layout for a page.
	 */
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		addFastViews(layout);
		addViewShortcuts(layout);
		addPerspectiveShortcuts(layout);
		{
			IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.RIGHT, 0.7f, IPageLayout.ID_EDITOR_AREA);
			folderLayout.addView("org.eclipse.scanning.device.ui.scanEditor");
			final String detectorId = DetectorView.createId(getUriString(), IEventService.DEVICE_REQUEST_TOPIC, IEventService.DEVICE_RESPONSE_TOPIC);
			folderLayout.addView(detectorId);
			folderLayout.addView("org.eclipse.scanning.device.ui.device.MonitorView");
		}
		
		IFolderLayout folder = layout.createFolder("folder_3", IPageLayout.LEFT, 0.22f, IPageLayout.ID_EDITOR_AREA);
		folder.addView("org.dawnsci.mapping.ui.mappeddataview:mapview=org.eclipse.scanning.device.ui.vis.visualiseView;spectrumview=org.eclipse.scanning.device.ui.spectrumview");
		
		folder = layout.createFolder("folder_4", IPageLayout.RIGHT, 0.05f, IPageLayout.ID_EDITOR_AREA);
		folder.addView("org.eclipse.scanning.device.ui.spectrumview");
		folder.addView(getQueueViewId());
		{
			IFolderLayout folderLayout = layout.createFolder("folder_0", IPageLayout.TOP, 0.66f, "org.eclipse.scanning.device.ui.spectrumview");
			folderLayout.addView("org.eclipse.scanning.device.ui.vis.visualiseView");
			folderLayout.addView("org.eclipse.scanning.device.ui.vis.StreamView");
		}
		{
			IFolderLayout folderLayout = layout.createFolder("folder_1", IPageLayout.BOTTOM, 0.26f, "org.eclipse.scanning.device.ui.scanEditor");
			folderLayout.addView("org.eclipse.scanning.device.ui.modelEditor");
			folderLayout.addView("org.eclipse.scanning.device.ui.device.ControlView");
			folderLayout.addView("org.eclipse.scanning.device.ui.points.scanRegionView");
		}
		{
			IFolderLayout folderLayout = layout.createFolder("folder_2", IPageLayout.BOTTOM, 0.54f, "org.eclipse.scanning.device.ui.modelEditor");
			folderLayout.addView("org.eclipse.scanning.device.ui.scan.executeView");
			folderLayout.addView(getConsumerViewId());
		}
	}

	private String getUriString() {
		String broker = CommandConstants.getScanningBrokerUri();
		if (broker==null) broker = "tcp://localhost:61616";
		return broker;
	}

	/**
	 * Add fast views to the perspective.
	 */
	private void addFastViews(IPageLayout layout) {
	}

	/**
	 * Add view shortcuts to the perspective.
	 */
	private void addViewShortcuts(IPageLayout layout) {
	}

	/**
	 * Add perspective shortcuts to the perspective.
	 */
	private void addPerspectiveShortcuts(IPageLayout layout) {
	}

	/**
	 * Hard codes certain views to life so that the perspective tends to work.
	 */
	public static void createKeyPlayers() {	
		ViewUtil.createViews(ScanRegionView.ID, ModelView.ID, getQueueViewId(), getConsumerViewId(), VisualiseView.ID);
	}
	
	private static String getQueueViewId() {
		try {
			String bundle = FrameworkUtil.getBundle(ScanBean.class).getSymbolicName();
			return QueueViews.createId(CommandConstants.getScanningBrokerUri(), bundle, ScanBean.class.getName(), "Scans");
		} catch (Exception ne) {
			return QueueViews.getQueueViewID();
		}
	}
	
	private static String getConsumerViewId() {
		return "org.eclipse.scanning.event.ui.consumerView:partName=Consumers";
	}

}
