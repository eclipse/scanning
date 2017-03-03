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
package org.eclipse.scanning.api.annotation.scan;

import java.io.PrintStream;

import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.LevelInformation;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * 
 * This class is an interface which defaults all the annotated methods
 * which a device may use when 
 * 
 * Sadly this does not allow Jython classes to be defined due to http://bugs.jython.org/issue2403
 *  
 * @author Matthew Gerring
 *
 */
@Deprecated
public interface AnnotatedDevice {
	
	default boolean isDebug() {
		return false;
	}
	
	default PrintStream getStream() {
		return System.out;
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param model
	 */
	@PreConfigure
	default void preConfigure(IDetectorModel model, ScanInformation info) throws ScanningException {
		if (isDebug()) getStream().println("Pre-configure of "+model);
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param model
	 */
	@PostConfigure
	default void postConfigure(IDetectorModel model, ScanInformation info) throws ScanningException {
		if (isDebug()) getStream().println("Post-configure of "+model);
	}


	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param info
	 */
	@LevelStart
    default void levelStart(LevelInformation info)  throws ScanningException { // Other arguments are allowed
		if (isDebug()) getStream().println("Level start and level is "+info.getLevel());
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param info
	 */
	@LevelEnd
    default void levelEnd(LevelInformation info) throws ScanningException { // Other arguments are allowed
		if (isDebug()) getStream().println("Level end and level is "+info.getLevel());
	}

	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param point
	 */
	@PointStart
	default void pointStart(IPosition point) throws ScanningException {
		if (isDebug()) getStream().println("Point start "+point.getStepIndex());
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param point
	 */
	@PointEnd
	default void pointEnd(IPosition point) throws ScanningException {
		if (isDebug()) getStream().println("Point end "+point.getStepIndex());
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param info
	 */
	@ScanStart
	default void scanStart(ScanInformation info) throws ScanningException {
		if (isDebug()) getStream().println("Scan start "+info.getFilePath());
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param info
	 */
	@ScanEnd
	default void scanEnd(ScanInformation info) throws ScanningException {
		if (isDebug()) getStream().println("Scan end "+info.getFilePath());
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param info
	 */
	@ScanAbort
	default void scanAbort(ScanInformation info) throws ScanningException {
		if (isDebug()) getStream().println("Scan aborted");
	}

	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param info
	 */
	@ScanFault
	default void scanFault(ScanInformation info) throws ScanningException {
		if (isDebug()) getStream().println("Scan fault");
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param info
	 */
	@ScanFinally
	default void scanFinally(ScanInformation info) throws ScanningException {
		if (isDebug()) getStream().println("Scan finally");
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param info
	 */
	@ScanPause
	default void scanPaused() throws ScanningException {
		if (isDebug()) getStream().println("Scan paused");
	}
	
	/**
	 * Implement default method for use with Jython.
	 * NOTE: Arguments are filled with annotations using introspection. OSGi services may be requested (null if not found)
	 * NOTE: More than one method may be annotated.
	 * @param info
	 */
	@ScanResume
	default void scanResumed() throws ScanningException {
		if (isDebug()) getStream().println("Scan resumed");
	}

}
