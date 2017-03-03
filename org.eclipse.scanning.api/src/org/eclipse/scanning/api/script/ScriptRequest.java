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

import java.util.Map;

/**
 * 
 * This object provides the information such as file path,
 * environment, script language etc. to run a script on the 
 * server.
 * 
 * @author Matthew Gerring.
 *
 */
public class ScriptRequest {

	private String             file;
	private ScriptLanguage     language;
	private Map<String,String> environment;

	public ScriptRequest() {

	}
	
	/**
	 * Creates a new SPEC_PASTICHE request running the file at the given path.
	 * @param file
	 */
	public ScriptRequest(String file) {
		this(file, ScriptLanguage.SPEC_PASTICHE);
	}

	public ScriptRequest(String file, ScriptLanguage lang) {
		this.file = file;
		this.language = lang;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((environment == null) ? 0 : environment.hashCode());
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScriptRequest other = (ScriptRequest) obj;
		if (environment == null) {
			if (other.environment != null)
				return false;
		} else if (!environment.equals(other.environment))
			return false;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (language != other.language)
			return false;
		return true;
	}

	public ScriptLanguage getLanguage() {
		return language;
	}

	public void setLanguage(ScriptLanguage language) {
		this.language = language;
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}

	@Override
	public String toString() {
		return file+" ("+language+")";
	}
}
