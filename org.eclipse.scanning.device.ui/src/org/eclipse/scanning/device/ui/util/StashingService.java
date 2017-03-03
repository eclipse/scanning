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
package org.eclipse.scanning.device.ui.util;

import java.io.File;

import org.eclipse.scanning.api.stashing.IStashing;
import org.eclipse.scanning.api.stashing.IStashingService;
import org.eclipse.scanning.device.ui.ServiceHolder;

public class StashingService implements IStashingService {

	@Override
	public IStashing createStash(String path) {
		return new Stashing(path, ServiceHolder.getMarshallerService());
	}

	@Override
	public IStashing createStash(File file) {
		return new Stashing(file, ServiceHolder.getMarshallerService());
	}

}
