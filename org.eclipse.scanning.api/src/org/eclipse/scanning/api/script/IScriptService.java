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
package org.eclipse.scanning.api.script;

/**
 * 
 * A service to run scripts, python, javascript, r etc.
 * 
 * This service may be backed by a custom engine which supports a subset
 * of the scripting languages. It might be implemented by an OSGi Jython
 * or by an eclipse EASE environment.
 * 
 * @author Matthew Gerring
 *
 */
public interface IScriptService {
	
	/**
	 * For DAQ server version 8 and 9 this will probably be {JYTHON, SPEC_PASTICHE}
	 * @return the available supported scripting languages that this service can execute.
	 */
	ScriptLanguage[] supported();

	/**
	 * Execute a script on the server. This can be used for instance inside scanning to run
	 * a script before and after a scan.
	 * 
	 * @param req the script request
	 * @return a {@link ScriptResponse} object describing the result of running the script
	 * @throws UnsupportedLanguageException if the script language specified by
	 *   {@link ScriptRequest#getLanguage()} is not supported by this script service.
	 * @throws ScriptExecutionException if an error occurred running the script
	 */
	ScriptResponse<?> execute(ScriptRequest req) throws UnsupportedLanguageException, ScriptExecutionException;
}
