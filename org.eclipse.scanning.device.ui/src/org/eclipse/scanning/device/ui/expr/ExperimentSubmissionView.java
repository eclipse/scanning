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
package org.eclipse.scanning.device.ui.expr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.richbeans.widgets.shuffle.ShuffleConfiguration;
import org.eclipse.richbeans.widgets.shuffle.ShuffleViewer;
import org.eclipse.scanning.api.database.ISampleDescriptionService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.queues.IQueueSpoolerService;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentSubmissionView {

	public static final String ID = "org.eclipse.scanning.device.ui.expr.experimentSubmissionView"; //$NON-NLS-1$
	private static final Logger logger = LoggerFactory.getLogger(ExperimentSubmissionView.class);
	
	private ShuffleConfiguration<SampleEntry> conf;
	private ShuffleViewer<SampleEntry>        viewer;
	private String proposalCode;
	private long proposalNumber;
	private Map <Long, String> sampleIdNames;
	private ISampleDescriptionService sampleDescriptionService = new MockSampleDescriptionService();
	private @Inject IQueueSpoolerService queueSpoolerService;	

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@PostConstruct
	public void createView(Composite parent) {
		
		conf = new ShuffleConfiguration<>();
		conf.setFromLabel("Available Experiments");
		conf.setToLabel("Submission List");
		conf.setFromReorder(true);
		conf.setToReorder(true);
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		viewer = new ShuffleViewer<>(conf);
		viewer.createPartControl(container);
		viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		final Color white = container.getDisplay().getSystemColor(SWT.COLOR_WHITE);
		
		final Composite buttons = new Composite(container, SWT.NONE);
		buttons.setBackground(white);
		buttons.setLayout(new RowLayout(SWT.HORIZONTAL));
		buttons.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		Button refresh = new Button(buttons, SWT.PUSH|SWT.FLAT);
		refresh.setText("Refresh");
		refresh.setBackground(white);
		refresh.setImage(Activator.getImageDescriptor("icons/recycle.png").createImage());
		refresh.addSelectionListener(				
				new SelectionAdapter() { 
					@Override
					public void widgetSelected(SelectionEvent e) {
						refresh();
					}
				});
		
		Button submit = new Button(buttons, SWT.PUSH|SWT.FLAT);
		submit.setText("Submit");
		submit.setBackground(white);
		submit.setImage(Activator.getImageDescriptor("icons/shoe--arrow.png").createImage());
		submit.addSelectionListener(				
				new SelectionAdapter() { 
					@Override
					public void widgetSelected(SelectionEvent e) {
						submit();
					}
				});
		
		processVisitID();
		refresh();
	}

	private void submit() {
		if (proposalCode == null) {
			logger.error("Absent or invalid visit ID");
			return;
		}
		try {
			submitExperiments();
			conf.setToList(new ArrayList<SampleEntry>());
			refresh();
		}
	    catch (QueueModelException e) {
		    logger.error("Error while queueing the experiment", e);
	    } 
		catch (EventException e) {
		    logger.error("Error detected in the event system", e);
	    }
	}

	private void refresh() {
		getSampleIdNamesForView();
	}

	@Focus
	public void setFocus() {
		viewer.setFocus();
	}
	
	@PreDestroy
	public void dispose() {
		viewer.dispose();
	}
	
	public void processVisitID() {
		String visitID;
		try {
			visitID = ServiceHolder.getFilePathService().getVisit();
		}
		catch (Exception e) {
			logger.error("Cannot get visit ID", e);
			return;
		}
		try(Scanner scanner = new Scanner(visitID)) {
			proposalCode = scanner.findInLine("\\D+");
			if (proposalCode == null) {
				logger.error("Error while parsing visit ID: invalid proposal code");
				return;
			}
			String lineProposalNumber = scanner.findInLine("\\d+");
			if (lineProposalNumber == null) {
				proposalCode = null;
				logger.error("Error while parsing visit ID: invalid proposal number");
				return;
			}
			proposalNumber = Long.parseLong(lineProposalNumber);
		}
		catch (Exception e) {
			logger.error("Cannot parse visit ID", e);
		}
		}
	
	/**
	 * Get the samples information and show it in the UI
	 */
	private void getSampleIdNamesForView() {
		if (proposalCode == null) {
			logger.error("Absent or invalid visit ID");
			return;
		}
		sampleIdNames = sampleDescriptionService.getSampleIdNames(proposalCode, proposalNumber);
		ArrayList <SampleEntry> fromList = new ArrayList<>();
		HashSet <SampleEntry> shuffleToList = new HashSet<>(conf.getToList());
		sampleIdNames.forEach((k, v) -> {
			SampleEntry sampleEntry = new SampleEntry(k, v);
			if (!shuffleToList.contains(sampleEntry)) {
				fromList.add(sampleEntry);
			}
		}); 
		conf.setFromList(fromList);
	}

	private void submitExperiments() throws QueueModelException, EventException {
		List<Long> sampleIdsList = new ArrayList<>();
		for (Object sample : conf.getToList()) {
			sampleIdsList.add(((SampleEntry) sample).getSampleId());
		}
		queueSpoolerService.submitExperiments(proposalCode, proposalNumber, sampleIdsList);
	}
	
}
