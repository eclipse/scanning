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
package org.eclipse.scanning.sequencer.nexus;

import java.util.Set;

import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.scanning.api.IConfigurable;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;

/**
 * An interface defining a nexus scan file to managed.
 */
public interface INexusScanFileManager extends IConfigurable<ScanModel> {

	/**
	 * Creates the nexus file.
	 * @throws ScanningException
	 * @return the path to the nexus file created
	 */
	public String createNexusFile(boolean async) throws ScanningException;

	/**
	 * Flushes any pending data into the nexus file.
	 * @throws ScanningException
	 */
	public void flushNexusFile() throws ScanningException;
	
	/**
	 * Informs the manager that the scan has finished. This will
	 * cause the scanFinished dataset to be updated and the nexus file to be closed.
	 * @throws ScanningException
	 */
	public void scanFinished() throws ScanningException;
	
	/**
	 * Get the nexus scan info for the scan.
	 * @return
	 */
	public NexusScanInfo getNexusScanInfo();
	
	/**
	 * Returns whether this nexus file manager actually writes nexus, i.e.
	 * returns <code>false</code> if this is a dummy.
	 * @return
	 */
	public boolean isNexusWritingEnabled();
	
	/**
	 * The names of all the external files which are 
	 * used to trigger archiving or for any other purpose where the .
	 * @return
	 */
	public Set<String> getExternalFilePaths();
}
