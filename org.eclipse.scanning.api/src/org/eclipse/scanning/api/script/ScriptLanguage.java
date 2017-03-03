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
 * Language of the script supported.
 * 
 * *NOTE* Not all scripting languages are supported by all implementations of the service.
 * 
 * @author Matthew Gerring
 *
 */
public enum ScriptLanguage {

	/**
	 * Full cpython, usually with numpy and scipy available.
	 */
	PYTHON,
	
    /**
     * Jython with DAQ objects available or an exception thrown if they cannot be imported.
     */
	JYTHON, 
	
	/**
	 * Jython with the DSL extensions to make some commands look like spec
	 * 
	 * This is slower to run than pure jython scripts because it must be parsed
	 * and run line by line. It is also not possible to debug using pydev.
	 * 
	 */
	SPEC_PASTICHE, 
	
	/**
	 * The ever popular scripting language
	 */
	JAVASCRIPT, 
	
	/**
	 * Uses GroovyScriptEngine to parse the input script and
	 * execute it on the server.
	 */
	GROOVY, 
	
	/**
	 * To run ruby using jruby
	 */
	RUBY, 
	
	/**
	 * Runs the famous statistical package R. In order for the server to do this,
	 * it will have to have R installed separately and linked to the server.
	 */
	R,
	
	/**
	 * If you must
	 */
	BASH;
}
