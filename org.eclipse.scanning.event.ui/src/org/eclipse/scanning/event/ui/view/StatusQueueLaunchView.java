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

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.scanning.api.event.queues.QueueViews;
import org.eclipse.scanning.event.ui.Activator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches a queue monitor which the appropriate queue and topic names.
 * 
 * @author Matthew Gerring
 *
 */
public class StatusQueueLaunchView extends ViewPart {
	
	private static final Logger logger = LoggerFactory.getLogger(StatusQueueLaunchView.class);
	
	
	private static final String BUNDLE       = "org.dawnsci.commandserver.ui.launchBundle";
	private static final String BEAN         = "org.dawnsci.commandserver.ui.launchBean";
	private static final String STATUS_QUEUE = "org.dawnsci.commandserver.ui.launchStatusQueue";
	private static final String STATUS_TOPIC = "org.dawnsci.commandserver.ui.launchStatusTopic";
	private static final String SUBMIT_QUEUE = "org.dawnsci.commandserver.ui.launchSubmitQueue";
	private static final String PART_NAME    = "org.dawnsci.commandserver.ui.launchPartName";


	public StatusQueueLaunchView() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(BUNDLE, "org.dawnsci.commandserver.SOMETHING");
		store.setDefault(BEAN,   "org.dawnsci.commandserver.SOMETHING.YourStatusBean");
		store.setDefault(STATUS_QUEUE, "scisoft.SOMETHING.STATUS_QUEUE");
		store.setDefault(STATUS_TOPIC, "scisoft.SOMETHING.STATUS_TOPIC");
		store.setDefault(SUBMIT_QUEUE, "scisoft.SOMETHING.SUBMISSION_QUEUE");
		store.setDefault(PART_NAME,    "Queue Monitor");
	}

	@Override
	public void createPartControl(Composite parent) {
		
		final Composite content = new Composite(parent, SWT.NONE);
		content.setLayout(new GridLayout(2, false));
		createTextControl("Bundle Name", BUNDLE, content);
		createTextControl("Bean Name",   BEAN, content);
		createTextControl("Status Queue Name", STATUS_QUEUE, content);
		createTextControl("Status Topic Name", STATUS_TOPIC, content);
		createTextControl("Submit Queue Name", SUBMIT_QUEUE, content);
		createTextControl("Part Name", PART_NAME, content);
		
		final Button launch = new Button(content, SWT.PUSH);
		launch.setText("Launch");
		launch.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 2, 1));
		launch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					openQueueMonitor();
				} catch (UnsupportedEncodingException e1) {
					logger.error("Cannot open monitor", e1);
				}
			}
		});
	}

	private void createTextControl(String slabel, final String propName, Composite content) {
		
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		final Label label = new Label(content, SWT.NONE);
		label.setText(slabel);
		
		final Text text = new Text(content, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		text.setText(store.getString(propName));
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				store.setValue(propName, text.getText());
			}
		});
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	
	private void openQueueMonitor() throws UnsupportedEncodingException {
		
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String bundle = store.getString(BUNDLE);
		String bean   = store.getString(BEAN);
		String sqn    = store.getString(STATUS_QUEUE);
		String stn    = store.getString(STATUS_TOPIC);
		String submit = store.getString(SUBMIT_QUEUE);
		String part   = store.getString(PART_NAME);

		String queueViewId = QueueViews.createSecondaryId(bundle,bean, sqn, stn, submit);
		queueViewId = queueViewId+"partName="+part;
		try {
			Util.getPage().showView(StatusQueueView.ID, queueViewId, IWorkbenchPage.VIEW_VISIBLE);
		} catch (PartInitException e) {
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "Cannot open view", "Cannot open view "+queueViewId, 
					new Status(Status.ERROR, "org.eclipse.scanning.event.ui", e.getMessage()));
			logger.error("Cannot open view", e);
		}
	}
}
