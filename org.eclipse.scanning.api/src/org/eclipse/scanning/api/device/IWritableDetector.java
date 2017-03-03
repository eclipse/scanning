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
package org.eclipse.scanning.api.device;

import org.eclipse.scanning.api.ILevel;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;


/**
 * Anatomy of a CPU scan (non-malcolm)
 * 
 * Making GDA8 Detectors work with GDA9 (org.eclipse.scanning) requires usage of the
 * GDA8 IHardwareConnectionService. This service returns IReadableDetector for the class
 * gda.device.Detector and IScanner for gda.px.detector.Detector. In the later case
 * the model provides the file path and omegaStart required.
 * <pre>
 *  ___________
 *  |         |
 * _|         |___________  collectData() Tell detector to collect
 *            ___________
 *            |         |
 * ___________|         |_  readout() Tell detector to readout
 * 
 *            ________
 *            |      |
 * ___________|      |____  moveTo()  Scannables move motors to new position
 * </pre>
 * 
 * The readable detector is designed to be read out manually by the scanning
 * system. Malcolm detectors are not read out manually by the scanning but
 * because they implement IScanner, they can be configured and run during the
 * scanning.
 * 
 * @author Matthew Gerring
 *
 * @param <T> Class of model required by detector to configure it.
 */
public interface IWritableDetector<T> extends IRunnableEventDevice<T>, INameable, ILevel {
	
	/**
	 * For GDA9 detectors, returns a boolean for the readout completed, true for complete, false for error.
	 * Generally detectors running in GDA9 will be IDetector.
	 * 
	 * For GDA8 detectors, the readout will be overridden.
	 * 
	 * @return true for a successful readout. If false the events will not be fired and the scan will continue.
	 * @throws DeviceException
	 */
	boolean write(IPosition position) throws ScanningException;

}
