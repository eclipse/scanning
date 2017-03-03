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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.scanning.api.stashing.IStashing;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Stashing implements IStashing {

	private static final Logger logger = LoggerFactory.getLogger(Stashing.class);
	
	private final File file;

	private IMarshallerService marshallerService;
	
	/**
	 * 
	 * @param fileName, e.g. org.eclipse.scanning.device.ui.device.controls.json
	 */
	Stashing(String fileName, IMarshallerService marshallerService) {
		this(new File(System.getProperty("user.home")+"/.solstice/"+fileName),marshallerService);
	}
	
	Stashing(File file, IMarshallerService marshallerService) {
		this.file = file;
		this.marshallerService = marshallerService;
	}

	public boolean isStashed() {
		return file.exists();
	}
	
	public void stash(Object object) throws Exception {
		final String json = marshallerService.marshal(object);
		write(file, json);
	}
	
	public <T> T unstash(Class<T> clazz) throws Exception {
		
		if (!isStashed()) return null;
		
		final String json = readFile(file).toString();
		final T ret = marshallerService.unmarshal(json, clazz);
		return ret;
		
	}
	
	private static void write(final File file, final String text) throws Exception {
		
		file.getParentFile().mkdirs();
		BufferedWriter b = null;
		try {
			final OutputStream out = new FileOutputStream(file);
			final OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
			b = new BufferedWriter(writer);
			b.write(text.toCharArray());
		} finally {
			if (b != null) {
				b.close();
			}
		}
	}

	private static final StringBuffer readFile(final File file) throws Exception {

		final String charsetName = "UTF-8";
		final InputStream in = new FileInputStream(file);
		BufferedReader ir = null;
		try {
			ir = new BufferedReader(new InputStreamReader(in, charsetName));

			// deliberately do not remove BOM here
			int c;
			StringBuffer currentStrBuffer = new StringBuffer();
			final char[] buf = new char[4096];
			while ((c = ir.read(buf, 0, 4096)) > 0) {
				currentStrBuffer.append(buf, 0, c);
			}
			return currentStrBuffer;

		} finally {
			if (ir != null) {
				ir.close();
			}
		}
	}
	
	/**
	 * Stash using appropriate messages to the user.
	 * @param models
	 * @param shell
	 */
	public void save(Object object) {
		try {
			
			if (file.exists()) {
				boolean ok = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Confirm Overwrite", "Are you sure that you would like to overwrite '"+file.getName()+"'?");
				if (ok) return;
			}
			
			stash(object);
			
		} catch (Exception e) {
			ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Error Saving Information", "An exception occurred while writing to file.",
					              new Status(IStatus.ERROR, "org.eclipse.scanning.device.ui", e.getMessage()));
		    logger.error("Error Saving Information", e);
		}
	}
	
	public <T> T load(Class<T> clazz) {
		try {
            return unstash(clazz);	
		} catch (Exception e) {
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Exception while reading scans from file", "An exception occurred while reading scans from a file.\n" + e.getMessage());
		    return null;
		}
     
	}

	@Override
	public String toString() {
		return file.getAbsolutePath();
	}

	public File getFile() {
		return file;
	}


}
