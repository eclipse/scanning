/*-
 * Copyright © 2017 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package org.eclipse.scanning.api.ui;

import java.util.HashMap;
import java.util.Map;

public class ScannableUIPreferencesService implements IScannableUIPreferencesService {

	private Map<String, ScannableUIPreferences> prefsStore;

	protected ScannableUIPreferencesService() {
		prefsStore = new HashMap<>();
	}

	@Override
	public ScannableUIPreferences getPreferences(String scannableName) {
		return prefsStore.containsKey(scannableName) ? prefsStore.get(scannableName) : new ScannableUIPreferences();
	}

	@Override
	public void register(ScannableUIPreferences prefs) {
		prefsStore.put(prefs.getName(), prefs);
	}

}
