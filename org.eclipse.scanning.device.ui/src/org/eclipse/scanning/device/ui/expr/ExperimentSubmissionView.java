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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.richbeans.widgets.shuffle.ShuffleConfiguration;
import org.eclipse.richbeans.widgets.shuffle.ShuffleViewer;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class ExperimentSubmissionView extends ViewPart {

	public static final String ID = "org.eclipse.scanning.device.ui.expr.experimentSubmissionView"; //$NON-NLS-1$

	private ShuffleConfiguration conf;
	private ShuffleViewer        viewer;
	
	public ExperimentSubmissionView() {
		
		conf = new ShuffleConfiguration();
		conf.setFromLabel("Available Experiments");
		conf.setToLabel("Submission List");
		conf.setFromReorder(true);
		conf.setToReorder(true);
		
		conf.setFromList(Arrays.asList("Experiment1", "Experiment2", "Experiment3", "Experiment4"));
	}

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		this.viewer = new ShuffleViewer(conf);
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
		
		Button submit = new Button(buttons, SWT.PUSH|SWT.FLAT);
		submit.setText("Submit");
		submit.setBackground(white);
		submit.setImage(Activator.getImageDescriptor("icons/shoe--arrow.png").createImage());

		createActions();
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		
		List<IContributionManager> mans = new ArrayList<>(Arrays.asList(getViewSite().getActionBars().getToolBarManager(), getViewSite().getActionBars().getMenuManager()));
		MenuManager     rightClick     = new MenuManager();
		mans.add(rightClick);

		IAction refresh = new Action("Refresh", Activator.getImageDescriptor("icons/recycle.png")) {
			public void run() {
				refresh();
			}
		};
		
		IAction submit = new Action("Submit", Activator.getImageDescriptor("icons/shoe--arrow.png")) {
			public void run() {
				submit();
			}
		};

		ViewUtil.addGroups("main", mans, refresh, submit);
		viewer.setMenu(rightClick);

	}

	private void submit() {
		System.out.println("TODO Implement send right data to queue!");
	}

	private void refresh() {
		System.out.println("TODO Implement stored procedure call...");
	}

	@Override
	public void setFocus() {
		viewer.setFocus();
	}
	
	@Override
	public void dispose() {
		viewer.dispose();
	}

}
