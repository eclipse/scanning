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
package org.eclipse.scanning.test.annot;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.annotation.scan.AnnotatedDevice;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;

// A device that uses inheritance instead of annotations for use with
// Jython.
// This class is just for testing, it is an example of making any device 
// have fixed annotated methods. This was done in the old Jython inheritance system for GDA8.
// @see AnnotatedDevice
public class AnAnnotatedDetector extends MockWritableDetector implements AnnotatedDevice {
	
	private List<String> lines; // Just for testing
	private LoggedStream stream;// Just for testing
	
	public AnAnnotatedDetector() {
		super();
		this.lines  = new ArrayList<>();
		this.stream = new LoggedStream();
	}
	
	public boolean contains(String line) {
		return lines.contains(line);
	}
	
	/**
	 * Just for testing!
	 */
	@Override
	public boolean isDebug() {
		return true;
	}
	
	/**
	 * Just for testing!
	 */
	@Override
	public PrintStream getStream() {
		return stream;
	}

	/**
	 * Just for testing!
	 */
	private final class LoggedStream extends PrintStream {
		private PrintStream delegate;
		public LoggedStream() {
			super(System.out);
			delegate = System.out;
		}
		public void println(String x) {
			delegate.println(x);
			lines.add(x);
		}
	}
}
