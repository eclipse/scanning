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

package org.eclipse.scanning.test.ui;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.scanning.api.ui.ScannableUIPreferences;
import org.junit.Test;

public class ScannableUIPreferencesTest {

	@Test
	public void testAllFieldsAreDefaulted() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// If you add a field to ScannableUIPreferences,
		// you must also give a default value which is set in ScannableUIPreferences::loadDefaults
		ScannableUIPreferences prefs = new ScannableUIPreferences();
		Class<ScannableUIPreferences> prefsClass = ScannableUIPreferences.class;
		Method[] allMethods = prefsClass.getMethods();
		for (Method method : allMethods) {
			if (method.getName().startsWith("get")) {
				assertNotNull(method.invoke(prefs));
			}
		}
	}

}
