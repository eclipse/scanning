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

/**
 * This class allows us to register instances of {@link ScannableUIPreferences}
 * configure in Spring to adjust default UI settings for particular scannables.
 */
public interface IScannableUIPreferencesService {

	/**
	 * The default service
	 */
	public static final IScannableUIPreferencesService DEFAULT = new ScannableUIPreferencesService();

	/**
	 * Return the Spring-configured UI preferences for a particular scannable, or default preferences if not configured
	 * @param scannableName
	 * @return
	 */
	ScannableUIPreferences getPreferences(String scannableName);

	void register(ScannableUIPreferences prefs);
}
