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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;


public class XcenActivator extends AbstractUIPlugin {
	
	public static final String PLUGIN_ID = "org.eclipse.scanning.example.xcen.ui";

	public XcenActivator() {
		// TODO Auto-generated constructor stub
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return getImageDescriptor(PLUGIN_ID, path);
	}

	private static ImageDescriptor getImageDescriptor(String plugin, String path) {
		return imageDescriptorFromPlugin(plugin, path);
	}
}
