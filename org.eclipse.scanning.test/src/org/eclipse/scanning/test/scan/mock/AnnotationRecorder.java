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
package org.eclipse.scanning.test.scan.mock;

import java.lang.annotation.Annotation;

import org.eclipse.scanning.api.annotation.scan.FileDeclared;
import org.eclipse.scanning.api.annotation.scan.LevelEnd;
import org.eclipse.scanning.api.annotation.scan.LevelStart;
import org.eclipse.scanning.api.annotation.scan.PointEnd;
import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.annotation.scan.PostConfigure;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.annotation.scan.ScanAbort;
import org.eclipse.scanning.api.annotation.scan.ScanEnd;
import org.eclipse.scanning.api.annotation.scan.ScanFault;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.annotation.scan.ScanPause;
import org.eclipse.scanning.api.annotation.scan.ScanResume;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.annotation.scan.WriteComplete;
import org.eclipse.scanning.api.scan.ScanInformation;

/**
 *
 * Yes should use Mockito but that has the issues with
 * hamcrest which are hard to resolve in the build.
 * 
 * This class is a handy way to do multiple inheritance in Java by defaulting
 * methods, filthy I know.
 * 
 * @author Matthew Gerring
 *
 */
public interface AnnotationRecorder {

	@PreConfigure
	default void configure(ScanInformation info) {
		record(PreConfigure.class);
	}
	
	@PostConfigure
	default void configured(ScanInformation info) {
		record(PostConfigure.class);
	}

	@ScanStart
	default void scanStart() {
		record(ScanStart.class);
	}
	
	@FileDeclared
	default void fileDeclared() {
		record(FileDeclared.class);
	}

	@ScanEnd
	default void scanEnd() {
		record(ScanEnd.class);
	}

	@PointStart
	default void pointStart() {
		record(PointStart.class);
	}
	
	@PointEnd
	default void pointEnd() {
		record(PointEnd.class);
	}
	
	@WriteComplete
	default void writeComplete() {
		record(WriteComplete.class);
	}

	@LevelStart
	default void levelStart() {
		record(LevelStart.class);
	}
	
	@LevelEnd
	default void levelEnd() {
		record(LevelEnd.class);
	}

	@ScanPause
	default void scanPause() {
		record(ScanPause.class);
	}
	
	@ScanResume
	default void scanResume() {
		record(ScanResume.class);
	}

	@ScanAbort
	default void scanAbort() {
		record(ScanAbort.class);
	}
	
	@ScanFault
	default void scanFault() {
		record(ScanFault.class);
	}
	
	@ScanFinally
	default void scanFinally() {
		record(ScanFinally.class);
	}

	void record(Class<? extends Annotation> method);
	
	int getCount(Class<? extends Annotation> methodName);
}
