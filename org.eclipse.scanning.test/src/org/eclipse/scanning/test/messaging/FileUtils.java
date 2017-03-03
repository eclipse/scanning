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
package org.eclipse.scanning.test.messaging;

import java.io.File;

public class FileUtils {

	static public final boolean recursiveDelete(File parent) {

		if (parent.exists()) {
			if (parent.isDirectory()) {

				File[] files = parent.listFiles();
				for (int ifile = 0; ifile < files.length; ++ifile) {
					if (files[ifile].isDirectory()) {
						recursiveDelete(files[ifile]);
					}
					if (files[ifile].exists()) {
						files[ifile].delete();
					}
				}
			}
			return parent.delete();
		}
		return false;
	}
	
}
