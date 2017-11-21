/*-
 * Copyright © 2017 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package org.eclipse.scanning.sequencer;

import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.scanning.api.annotation.scan.WriteComplete;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.LevelInformation;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the abstract base class of all classes which define the delegated functionality of an
 * AreaDetectorRunnableDeviceProxy object.
 * <P>
 * Since this defines all functions through inheritance rather than through annotations, and it doesn't depend on
 * interface default methods, it can be used as a base class for delegates defined in Jython.
 * <P>
 * Note that an object of this class must be initialised with a reference to the object it is going to provide
 * delegated functionality to and setDelegate() on that object should then be immediately called with this one.
 * <p>
 * Example of use:
 * </P>
 * <pre>{@code
areaDetectorRunnableDeviceProxyFinder = finder.find("areaDetectorRunnableDeviceProxyFinder")
areaDetectorRunnableDeviceProxy = areaDetectorRunnableDeviceProxyFinder.getRunnableDevice()

from jythonAreaDetectorRunnableDeviceDelegate import JythonAreaDetectorRunnableDeviceDelegate
jythonAreaDetectorRunnableDeviceDelegate = JythonAreaDetectorRunnableDeviceDelegate(areaDetectorRunnableDeviceProxy)
areaDetectorRunnableDeviceProxy.setDelegate(jythonAreaDetectorRunnableDeviceDelegate)
areaDetectorRunnableDeviceProxy.register()
 * }</pre>
 */
public abstract class AbstractRunnableDeviceDelegate {

	private static final Logger logger = LoggerFactory.getLogger(AbstractRunnableDeviceDelegate.class);

	RunnableDeviceProxy runnableDeviceProxy;

	public AbstractRunnableDeviceDelegate(RunnableDeviceProxy runnableDeviceProxy) {
		this.runnableDeviceProxy = runnableDeviceProxy;
	}

	public RunnableDeviceProxy getRunnableDeviceProxy() {
		return runnableDeviceProxy;
	}

	// Delegated AbstractRunnableDevice<AreaDetectorRunnableDeviceModel> methods

	@SuppressWarnings("unused")
	public void configure(Object model) throws ScanningException {
		logger.trace("configure({}) on {}", model, runnableDeviceProxy.getName());
	}

	// Delegated interface IRunnableDevice<AreaDetectorRunnableDeviceModel> methods

	@SuppressWarnings("unused")
	public void run(IPosition position) throws ScanningException, InterruptedException {
		logger.trace("run({}) on {}", position, runnableDeviceProxy.getName());
	}

	// Delegated interface IWritableDetector<AreaDetectorRunnableDeviceModel> methods

	@SuppressWarnings("unused")
	public boolean write(IPosition position) throws ScanningException {
		logger.trace("write({}) on {}", position, runnableDeviceProxy.getName());
		return false;
	}

	// Delegated interface INexusDevice<NXdetector> methods

	@SuppressWarnings("unused")
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info) throws NexusException {
		logger.trace("getNexusProvider({}) on {}", info, runnableDeviceProxy.getName());
		return null;
	}


	protected NexusObjectWrapper<NXdetector> getNexusObjectWrapper(String name, NXdetector nexusBaseClass) {
		return new NexusObjectWrapper<NXdetector>(name, nexusBaseClass);
	}

	// Delegated annotated methods

	@SuppressWarnings("unused")
	public void preConfigure(Object scanModel, ScanBean scanBean, IPublisher<?> publisher) throws ScanningException {
		logger.trace("preConfigure({}, {}, {}) on {}", scanModel, scanBean, publisher, runnableDeviceProxy.getName());
	}

	@SuppressWarnings("unused")
	public void postConfigure(Object scanModel, ScanBean scanBean, IPublisher<?> publisher) throws ScanningException {
		logger.trace("postConfigure({}, {}, {}) on {}", scanModel, scanBean, publisher, runnableDeviceProxy.getName());
	}

	@SuppressWarnings("unused")
	public void levelStart(LevelInformation info)  throws ScanningException { // Other arguments are allowed
		logger.trace("levelStart({}) on {}", info, runnableDeviceProxy.getName());
	}

	@SuppressWarnings("unused")
	public void levelEnd(LevelInformation info) throws ScanningException { // Other arguments are allowed
		logger.trace("levelEnd({}) on {}", info, runnableDeviceProxy.getName());
	}

	@SuppressWarnings("unused")
	public void pointStart(IPosition point) throws ScanningException {
		logger.trace("pointStart({}) on {} stepIndex={}", point, runnableDeviceProxy.getName(), point.getStepIndex());
	}

	@SuppressWarnings("unused")
	public void pointEnd(IPosition point) throws ScanningException {
		logger.trace("pointEnd({}) on {} stepIndex={}", point, runnableDeviceProxy.getName(), point.getStepIndex());
	}

	@SuppressWarnings("unused")
	public void scanStart(ScanInformation info) throws ScanningException {
		logger.trace("scanStart({}) on {} filePath={}", info, runnableDeviceProxy.getName(), info.getFilePath());
	}

	@SuppressWarnings("unused")
	public void scanEnd(ScanInformation info) throws ScanningException {
		logger.trace("scanEnd({}) on {} filePath={}", info, runnableDeviceProxy.getName(), info.getFilePath());
	}

	/** Delegated {@link WriteComplete}
	 *
	 * @param info
	 * @throws ScanningException
	 */
	@SuppressWarnings("unused")
	public void writeComplete(ScanInformation info) throws ScanningException {
		logger.trace("writeComplete({}) on {} filePath={}", info, runnableDeviceProxy.getName(), info.getFilePath());
	}

	@SuppressWarnings("unused")
	public void scanAbort(ScanInformation info) throws ScanningException {
		logger.trace("scanAbort({}) on {}", info, runnableDeviceProxy.getName());
	}

	@SuppressWarnings("unused")
	public void scanFault(ScanInformation info) throws ScanningException {
		logger.trace("scanFault({}) on {}", info, runnableDeviceProxy.getName());
	}

	@SuppressWarnings("unused")
	public void scanFinally(ScanInformation info) throws ScanningException {
		logger.trace("scanFinally({}) on {}", info, runnableDeviceProxy.getName());
	}

	@SuppressWarnings("unused")
	public void scanPause() throws ScanningException {
		logger.trace("scanPause() on {}", runnableDeviceProxy.getName());
	}

	@SuppressWarnings("unused")
	public void scanResume() throws ScanningException {
		logger.trace("scanResume() on {}", runnableDeviceProxy.getName());
	}
}
