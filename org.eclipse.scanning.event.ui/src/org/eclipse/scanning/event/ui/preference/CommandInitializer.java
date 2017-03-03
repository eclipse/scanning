/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.scanning.event.ui.preference;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.event.ui.Activator;

public class CommandInitializer extends AbstractPreferenceInitializer {


	@Override
	public void initializeDefaultPreferences() {
		
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		// TODO This is not the final URI
		store.setDefault(CommandConstants.JMS_URI,          CommandConstants.DEFAULT_JMS_URI);
		store.setDefault(CommandConstants.DIR_CHECKING_URI, CommandConstants.DEFAULT_CHECKING_URI);

	}

}
