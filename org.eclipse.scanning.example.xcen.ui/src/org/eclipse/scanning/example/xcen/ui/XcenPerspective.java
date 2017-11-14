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
package org.eclipse.scanning.example.xcen.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class XcenPerspective implements IPerspectiveFactory {

	/**
	 * Creates the initial layout for a page.
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {

		layout.setEditorAreaVisible(false);
		layout.addView("org.eclipse.scanning.example.xcen.ui.views.XcenDiagram", IPageLayout.LEFT, 0.40f, IPageLayout.ID_EDITOR_AREA);
		layout.addView("org.eclipse.scanning.example.xcen.ui.views.XcenView", IPageLayout.RIGHT, 0.60f, IPageLayout.ID_EDITOR_AREA);

		/*
		    -submit dataacq.xcen.SUBMISSION_QUEUE
		    -topic dataacq.xcen.STATUS_TOPIC
		    -status dataacq.xcen.STATUS_QUEUE
		    -bundle org.eclipse.scanning.example.xcen
		    -consumer org.eclipse.scanning.example.xcen.consumer.XcenConsumer
		 */
		IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.BOTTOM, 0.5f, "org.eclipse.scanning.example.xcen.ui.views.XcenView");
		folderLayout.addView(XcenServices.getQueueViewSecondaryId());
		folderLayout.addView("org.eclipse.scanning.event.ui.consumerView");

	}
}
