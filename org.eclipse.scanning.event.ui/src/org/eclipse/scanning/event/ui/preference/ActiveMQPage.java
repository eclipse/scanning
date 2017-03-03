/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.scanning.event.ui.preference;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.event.ui.Activator;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Advanced configuration for connecting to ActiveMQ
 * @author Matthew Gerring
 *
 */
public class ActiveMQPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{
	
	// Do not change, referenced externally.
	public static final String ID = "org.dawnsci.commandserver.ui.activemqPage";

	public ActiveMQPage() {
		super();
		
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		setPreferenceStore(store);
		setDescription("Preferences for connecting auto-processing reruns to the command server.");
	}

	@Override
	protected void createFieldEditors() {
		
	    final StringFieldEditor uri = new StringFieldEditor(CommandConstants.JMS_URI, "Comamnd Server", getFieldEditorParent());
	    addField(uri);

	    final StringFieldEditor checking = new StringFieldEditor(CommandConstants.DIR_CHECKING_URI, "Directory Permisssion Checker", getFieldEditorParent());
	    addField(checking);
	}

	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub
		
	}

}
