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
package org.eclipse.scanning.api;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * A parser for reading a spring file and creating objects.
 * The parser might use actual spring or its own home grown version.
 * If a home grown version is used, the parse method will throw an
 * UnsupportedOperationException. If other errors occur with the 
 * parsing any other type of exception may be thrown.
 * 
 * @author Matthew Gerring
 *
 */
public interface ISpringParser {

	/**
	 * Parse an xml 
	 * @param path
	 * @return
	 */
	Map<String, Object> parse(String path) throws UnsupportedOperationException, Exception;
	
	
	/**
	 * Parse an xml 
	 * @param in
	 * @return
	 */
	Map<String, Object> parse(InputStream in) throws UnsupportedOperationException, Exception;

	/**
	 * Set the directory for import resource
	 * @param dir
	 */
	void setDirectory(File dir);
}
