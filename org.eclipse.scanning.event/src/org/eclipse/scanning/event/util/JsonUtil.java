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
package org.eclipse.scanning.event.util;

import java.util.List;

public class JsonUtil {

	
	/**
	 * Attempts to remove properties of the parent if the subscriber has
	 * been told that they are not of interest. For instance if the 'detectors'
	 * property of a ScanRequest in an ScanBean is not required, it can be removed
	 * from the json and avoid any objects it needs to be serialized being an issue.
	 * For instance, there is no guarantee that the required detector models would 
	 * be in the classpath. If they are not of interest to the subscriber then
	 * they may be removed.
	 * 
	 * @param json
	 * @param all properties in all objects with these names will be removed.
	 * @return
	 */
	public static String removeProperties(String json, List<String> properties) {
		
		if (properties==null) return json; // Nothing to filter!
		for (String property : properties) {
			json = remove(json, property);
		}
		return json;
	}
	
	private static String remove(String json, String property) {
		
		final String frag = "\""+property+"\":";
		int index = json.indexOf(frag);
		if (index<0) return json; // Nothing to ignore.
		
		StringBuilder ret = new StringBuilder();
		ret.append(json.substring(0, index));
		

		int i = index+frag.length();
		int bracket = 0;
		while(i<json.length()) {
			char c = json.charAt(i);
			if (c == '{') bracket++;
			if (c == '}') bracket--;
			if (c ==  ',') {
				if (bracket==0) break;
			}
			i++;
		}
		
		String end = json.substring(i+1, json.length());
		ret.append(end);
		String njson = ret.toString();
		return remove(njson, property);
	}


}
