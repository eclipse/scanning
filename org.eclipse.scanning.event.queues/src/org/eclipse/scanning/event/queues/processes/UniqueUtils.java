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
package org.eclipse.scanning.event.queues.processes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 
 * File contains some methods copied from org.dawb.common.util.io.FileUtils
 * because do not want to make a dependency on that!
 * 
 * Original version EPL licensed and at:
 * https://github.com/DawnScience/dawn-common/blob/master/org.dawb.common.util/src/org/dawb/common/util/io/FileUtils.java
 * 
 * @author Matthew Gerring
 *
 */
class UniqueUtils {

	/**
	 * 
	 * @param n
	 * @return
	 */
	public static String getSafeName(String n) {
		
		if (n==null) return null;
		
		if (n.matches("[a-zA-Z0-9_]+")) return n;
		
		final StringBuilder buf = new StringBuilder();
		for (char c : n.toCharArray()) {
			if (String.valueOf(c).matches("[a-zA-Z0-9_]")) {
				buf.append(c);
			} else {
				if (buf.length()<1 || "_".equals(buf.substring(buf.length()-1))) continue;
				buf.append("_");
			}
		}
		
		if (buf.length()<1) {
			buf.append("Invalid_name");
		} else if (buf.substring(0, 1).matches("[0-9]")) {
			buf.append("var", 0, 3);
		}
		
		return buf.toString();
	}

	/**
	 * Generates a unique file of the name template or template+an integer
	 * 
	 * @param dir
	 * @param template
	 * @param ext
	 *        if null will return a unique directory name
	 * @return a unique file.
	 */
	public static File getUnique(final File dir, final String template, final String ext) {
		String extension = ext != null ? (ext.startsWith(".")) ? ext : "." + ext : null;
		extension = extension != null ? extension : "";
		final File file = new File(dir, template + extension);
		if (!file.exists()) {
			return file;
		}

		return getUnique(dir, template, ext, 1);
	}

	/**
	 * @param dir
	 * @param template
	 * @param ext
	 * @param i
	 * @return file
	 */
	public static File getUnique(final File dir, final String template, final String ext, int i) {
		final String extension = ext != null ? (ext.startsWith(".")) ? ext : "." + ext : null;
		final File file = ext != null ? new File(dir, template + i + extension) : new File(dir, template + i);
		if (!file.exists()) {
			return file;
		}

		return getUnique(dir, template, ext, ++i);
	}
	
	/**
	 * 
	 * @param dir
	 * @param template
	 * @param ext
	 * @return
	 */
	public static Path getUnique(final Path dir, final String template, final String ext) {
		final String extension = ext != null ? (ext.startsWith(".")) ? ext : "." + ext : null;
		final Path file = Paths.get(dir.toAbsolutePath()+"/"+ template + extension);
		if (!Files.exists(file)) {
			return file;
		}

		return getUnique(dir, template, ext, 1);
	}

	private static Path getUnique(final Path dir, final String template, final String ext, int i) {
		final String extension = ext != null ? (ext.startsWith(".")) ? ext : "." + ext : null;
		final Path file = ext != null ? Paths.get(dir.toAbsolutePath()+"/"+ template + i + extension) : Paths.get(dir.toAbsolutePath()+"/"+ template + i);
		if (!Files.exists(file)) {
			return file;
		}

		return getUnique(dir, template, ext, ++i);
	}


}
