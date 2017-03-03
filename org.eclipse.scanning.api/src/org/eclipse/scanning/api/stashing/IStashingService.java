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
package org.eclipse.scanning.api.stashing;

import java.io.File;

/**
 * Service for creating stashes which save objects to json
 * using the IMarshalling Service.
 * 
 * @author Matthew Gerring
 *
 */
public interface IStashingService {

	/**
	 * Create a stash using a path contructed from the user's home.
	 * The file = System.getProperty("user.home")+"/.solstice/"+fileName
	 * 
	 * @param path
	 * @return
	 */
	IStashing createStash(String fileName);
	
	/**
	 * Create a stash using a file
	 * @param path
	 * @return
	 */
	IStashing createStash(File file);
}
