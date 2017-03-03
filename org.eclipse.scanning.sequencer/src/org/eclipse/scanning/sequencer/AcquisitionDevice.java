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
package org.eclipse.scanning.sequencer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.annotation.scan.AnnotationManager;
import org.eclipse.scanning.api.annotation.scan.FileDeclared;
import org.eclipse.scanning.api.annotation.scan.PointEnd;
import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.annotation.scan.ScanAbort;
import org.eclipse.scanning.api.annotation.scan.ScanEnd;
import org.eclipse.scanning.api.annotation.scan.ScanFault;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.annotation.scan.ScanPause;
import org.eclipse.scanning.api.annotation.scan.ScanResume;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.annotation.scan.WriteComplete;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IPausableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.ScanMode;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IDeviceDependentIterable;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.IScanService;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanEstimator;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.event.IPositioner;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.sequencer.nexus.INexusScanFileManager;
import org.eclipse.scanning.sequencer.nexus.NexusScanFileManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This device does a standard GDA scan at each point. If a given point is a 
 * MalcolmDevice, that device will be configured and run for its given point.
 * 
 * The levels of the scannables at the position will be taken into
 * account and the position reached using an IPositioner then the 
 * scanners run.
 * 
 * @author Matthew Gerring
 */
final class AcquisitionDevice extends AbstractRunnableDevice<ScanModel> implements IPositionListener {
		
	// Scanning stuff
	private IPositioner                          positioner;
	private LevelRunner<IRunnableDevice<?>>      runners;
	private LevelRunner<IRunnableDevice<?>>      writers;
	private AnnotationManager                    manager;
	
	// the nexus file
	private INexusScanFileManager nexusScanFileManager = null;
	
	private static Logger logger = LoggerFactory.getLogger(AcquisitionDevice.class);
	
	/*
	 * Concurrency design recommended by Keith Ralphs after investigating
	 * how to pause and resume a collection cycle using Reentrant locks.
	 * Design requires these three fields.
	 */
	private ReentrantLock    lock;
	private Condition        paused;
	private volatile boolean awaitPaused;
	
	/**
	 * Used for clients that would like to wait until the run. Most useful
	 * if the run was started with a start() call then more work is done, then
	 * a latch() will join with the start and return once it is finished.
	 * 
	 * If the start hangs, so will calling latch, there is no timeout.
	 * 
	 */
	private CountDownLatch latch;
	
	/**
	 * Manages the positions we reach in the scan, including
	 * the outer scan location.
	 */
	private LocationManager location;

	/**
	 * The current position iterator we are using
	 * to move over the CPU scan.
	 */
	private Iterator<IPosition> positionIterator;
		
	/**
	 * Package private constructor, devices are created by the service.
	 */
	AcquisitionDevice() {
		super(ServiceHolder.getRunnableDeviceService());
		this.lock      = new ReentrantLock();
		this.paused    = lock.newCondition();
		setName("solstice_scan");
		setPrimaryScanDevice(true);
		setRole(DeviceRole.VIRTUAL);
		setSupportedScanModes(EnumSet.allOf(ScanMode.class));
	}
	
	/**
	 * Method to configure the device. It also will check if the
	 * declared devices in the scan are INexusDevice. If they are,
	 * it will hook them up to the file writing if the ScanModel 
	 * file is set. If there is no file set in the model, the scan
	 * will proceed but not write to a nexus file.
	 */
	@Override
	public void configure(ScanModel model) throws ScanningException {
		
		long before = System.currentTimeMillis();
		
		setDeviceState(DeviceState.CONFIGURING);
		setModel(model);
		setBean(model.getBean()!=null?model.getBean():new ScanBean());
		getBean().setPreviousStatus(getBean().getStatus());
		getBean().setStatus(Status.QUEUED);
		
		positioner = runnableDeviceService.createPositioner();
		if (model.getDetectors()!=null) {
			// Make sure all devices report the same scan id
			for (IRunnableDevice<?> device : model.getDetectors()) {
				if (device instanceof AbstractRunnableDevice<?>) {
					// TODO the same bean should not be shared between detectors
					AbstractRunnableDevice<?> adevice = (AbstractRunnableDevice<?>)device;
					DeviceState deviceState = adevice.getDeviceState();
					ScanBean bean = getBean();
					bean.setDeviceState(deviceState);
					adevice.setBean(bean);
					adevice.setPrimaryScanDevice(false);
				}
			}
		}
		
		// Create the manager and populate it
		if (manager!=null) manager.dispose(); // It is allowed to configure more than once.
		manager = createAnnotationManager(model);

		// create the location manager
		location = new LocationManager(getBean(), model, manager);
		
		// add the scan information to the context - it is created if not set on the scan model
		manager.addContext(getScanInformation(location.getTotalSize()));
		
		// create the nexus file, if appropriate
		nexusScanFileManager = NexusScanFileManagerFactory.createNexusScanFileManager(this);
		nexusScanFileManager.configure(model);
		nexusScanFileManager.createNexusFile(Boolean.getBoolean("org.eclipse.scanning.sequencer.nexus.async"));
		
		// create the runners and writers
		if (model.getDetectors()!=null) {
			runners = new DeviceRunner(model.getDetectors());
			if (nexusScanFileManager.isNexusWritingEnabled()) {
				writers = new DeviceWriter(model.getDetectors());
			} else {
				writers = LevelRunner.createEmptyRunner();
			}
		} else {
			runners = LevelRunner.createEmptyRunner();
			writers = LevelRunner.createEmptyRunner();
		}
		
		// notify that the device is now ready
		setDeviceState(DeviceState.READY); 
		
		// record the time taken to configure the device
		long after = System.currentTimeMillis();
		setConfigureTime(after-before);
	}

	private AnnotationManager createAnnotationManager(ScanModel model) throws ScanningException {
		Collection<Object> globalParticipants = ((IScanService)runnableDeviceService).getScanParticipants();
		AnnotationManager manager = new AnnotationManager(SequencerActivator.getInstance());
		manager.addDevices(getScannables(model));
		if (model.getMonitors()!=null)               manager.addDevices(model.getMonitors());
		if (model.getAnnotationParticipants()!=null) manager.addDevices(model.getAnnotationParticipants());
		if (globalParticipants!=null)                manager.addDevices(globalParticipants);
		if (model.getDetectors()!=null)              manager.addDevices(model.getDetectors());
		
		return manager;
	}

	@Override
	public void run(IPosition parent) throws ScanningException, InterruptedException {
		
		
		if (getDeviceState()!=DeviceState.READY) throw new ScanningException("The device '"+getName()+"' is not ready. It is in state "+getDeviceState());
		
		ScanModel model = getModel();
		if (model.getPositionIterable()==null) throw new ScanningException("The model must contain some points to scan!");
						
		manager.addContext(getBean());
		manager.addContext(model);
	
		boolean errorFound = false;
		IPosition pos = null;
		try {
			this.positionIterator = location.createPositionIterator();

			RunnableDeviceServiceImpl.setCurrentScanningDevice(this); // Alows Jython to get and pause/seek.
			
			if (latch!=null) latch.countDown();
			this.latch = new CountDownLatch(1);
	        // TODO Should we validate the position iterator that all
	        // the positions are valid before running the scan?
	        // It was called limit checking in GDA.
	        // Sometimes logic is needed to implement collision avoidance
			
    		// Set the size and declare a count
    		fireStart(location.getTotalSize());
    		
    		// We allow monitors which can block a position until a setpoint is
    		// reached or add an extra record to the NeXus file.
    		if (model.getMonitors()!=null) {
    			List<IScannable<?>> perPoint = model.getMonitors().stream().filter(scannable -> scannable.getMonitorRole()==MonitorRole.PER_POINT).collect(Collectors.toList());
    			positioner.setMonitors(perPoint);
    		}
    		
    		// Add the malcolm listners so that progress on inner malcolm scans can be reported
    		addMalcolmListeners();

    		// The scan loop
        	pos = null; // We want the last point when we are done so don't use foreach
        	boolean firedFirst = false;
	        while (positionIterator.hasNext()) {
                
	        	pos = positionIterator.next();
	        	pos.setStepIndex(location.getStepNumber());
	        	
	        	if (!firedFirst) {
	        		fireFirst(pos);
	            	firedFirst = true;
	        	}
	        	
	        	// Check if we are paused, blocks until we are not
	        	boolean continueRunning = checkPaused();
	        	if (!continueRunning) return;  // finally block performed 

	        	// Run to the position
        		manager.invoke(PointStart.class, pos);
	        	positioner.setPosition(pos);   // moveTo in GDA8
	        	
	        	IPosition written = writers.await();          // Wait for the previous write out to return, if any
	       		if (written!=null) manager.invoke(WriteComplete.class, written);
	        	
 	        	runners.run(pos);              // GDA8: collectData() / GDA9: run() for Malcolm
	        	writers.run(pos, false);       // Do not block on the readout, move to the next position immediately.
	        	
	        	// Send an event about where we are in the scan
        		manager.invoke(PointEnd.class, pos);
	        	positionComplete(pos);
	        }
	        
	        // On the last iteration we must wait for the final readout.
        	IPosition written = writers.await();          // Wait for the previous write out to return, if any
       		manager.invoke(WriteComplete.class, written);

      	
		} catch (ScanningException | InterruptedException i) {
			errorFound=true;
			processException(i);
			throw i;
			
		} catch (Exception ne) {
			errorFound=true;
			processException(ne);
			throw new ScanningException(ne);
			
		} finally {
			close(errorFound, pos);
			RunnableDeviceServiceImpl.setCurrentScanningDevice(null);
		}
	}
	
	private void positionComplete(IPosition pos) throws EventException, ScanningException {
    	positionComplete(pos, location.getStepNumber(), location.getOuterSize());
	}

	private void fireFirst(IPosition firstPosition) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, ScanningException, EventException {
		
		// Notify that we will do a run and provide the first position.
		manager.invoke(ScanStart.class, firstPosition);
		if (model.getFilePath()!=null) manager.invoke(FileDeclared.class, model.getFilePath(), firstPosition);
		
		final Set<String> otherFiles = nexusScanFileManager.getExternalFilePaths();
		if (otherFiles!=null && otherFiles.size()>0) {
			for (String path : otherFiles) if (path!=null) manager.invoke(FileDeclared.class, path, firstPosition);
		}
		
    	fireRunWillPerform(firstPosition);
	}

	/**
	 * Remove this from the list of position listeners for any Malcolm Device
	 */
	private void removeMalcolmListeners() {
		try {
			if (model.getDetectors() != null) {
				// Make sure all devices report the same scan id
				for (IRunnableDevice<?> device : model.getDetectors()) {
					if (device.getRole() == DeviceRole.MALCOLM) {
						AbstractRunnableDevice<?> ard = (AbstractRunnableDevice<?>) device;
						ard.removePositionListener(this);
					}
				}
			}
		} catch (Exception ex) {
			logger.error("Error removing listener", ex);
		}
	}

	/**
	 * Add this to the list of position listeners for any Malcolm Device
	 * 
	 */
	private void addMalcolmListeners() {
		if (model.getDetectors() != null) {
			for (IRunnableDevice<?> device : model.getDetectors()) {
				if (device.getRole() == DeviceRole.MALCOLM) {
					AbstractRunnableDevice<?> ard = (AbstractRunnableDevice<?>) device;
					ard.addPositionListener(this);
				}
			}
		}
	}

	private void close(boolean errorFound, IPosition last) throws ScanningException {
		try {
			try {
				try {
					removeMalcolmListeners();
				} catch (Exception ex) {
					logger.warn("Error during removing Malcolm listeners", ex);
				}
				positioner.close();
				runners.close();
				writers.close();
				
				nexusScanFileManager.scanFinished(); // writes scanFinished and closes nexus file
	        	
				// We should not fire the run performed until the nexus file is closed.
	        	// Tests wait for this step and reread the file.
	       	    fireRunPerformed(last);              // Say that we did the overall run using the position we stopped at.
			
			} finally {    	    
	    		// only fire end if finished normally
	    		if (!errorFound) fireEnd(last);	
			}
			
		} finally {
       	    try {
				manager.invoke(ScanFinally.class, last);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException | EventException e) {
				throw new ScanningException(e);
			}
			if (latch!=null) latch.countDown();
		}
	}

	@Override
	public void latch() throws InterruptedException {
		if (latch==null) return;
		latch.await();
	}
	
	@Override
	public boolean latch(long time, TimeUnit unit) throws InterruptedException {
		if (latch==null) return true;
		return latch.await(time, unit);
	}

	private void processException(Exception ne) throws ScanningException {
		
		if (!getBean().getStatus().isFinal()) getBean().setStatus(Status.FAILED);
		getBean().setMessage(ne.getMessage());
		try {
			manager.invoke(ScanFault.class, ne);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException | EventException e) {
			throw new ScanningException(ne);
		}
		setDeviceState(DeviceState.FAULT);
		
		if (!getBean().getStatus().isFinal()) getBean().setStatus(Status.FAILED);
		getBean().setMessage(ne.getMessage());

	}
	
	private ScanInformation getScanInformation(int size) throws ScanningException {
		ScanInformation scanInfo = getModel().getScanInformation();
		if (scanInfo == null) {
			ScanEstimator estimator;
			try {
				estimator = new ScanEstimator(model.getPositionIterable(), model.getDetectors());
			} catch (GeneratorException e) {
				logger.error("Could not create scan estimator", e);
				throw new ScanningException("Could not create scan estimator", e);
			}
			
			scanInfo = new ScanInformation(estimator);
			scanInfo.setSize(size);
			scanInfo.setRank(getScanRank(getModel().getPositionIterable()));
			scanInfo.setScannableNames(getScannableNames(getModel().getPositionIterable()));
			scanInfo.setFilePath(getModel().getFilePath());
			
			getModel().setScanInformation(scanInfo);
		}
		
		return scanInfo;
	}

	private void fireStart(int size) throws Exception {
		// Setup the bean to sent
		getBean().setSize(size);	        
		getBean().setPreviousStatus(getBean().getStatus());
		getBean().setStatus(Status.RUNNING);
		
		// Will send the state of the scan off.
		setDeviceState(DeviceState.RUNNING); // Fires!
		
		// Leave previous state as running now that we have notified of the start.
		getBean().setPreviousStatus(Status.RUNNING);
	}

	private void fireEnd(IPosition lastPosition) throws ScanningException {
		
		// Setup the bean to sent
		getBean().setPreviousStatus(getBean().getStatus());
		getBean().setStatus(Status.COMPLETE);
		getBean().setPercentComplete(100);
		getBean().setMessage("Scan Complete");
		
		// Will send the state of the scan off.
		try {
			manager.invoke(ScanEnd.class, lastPosition);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException  | EventException e) {
			throw new ScanningException(e);
		}
   	    setDeviceState(DeviceState.READY); // Fires!
				
	}

	public void reset() throws ScanningException {
		
		if (positioner instanceof LevelRunner) {
			((LevelRunner)positioner).reset();
		}
		runners.reset();
		writers.reset();

		super.reset();
	}

	/**
	 * Blocks until not paused
	 * @return true if state has not been set to a rest one, i.e. we are still scanning.
	 * @throws Exception
	 */
	private boolean checkPaused() throws Exception {
		
		if (!getDeviceState().isRunning() && getDeviceState()!=DeviceState.READY) {
			if (getDeviceState().isRestState()) return false;
			throw new Exception("The scan state is "+getDeviceState());
		}

		// Check the locking using a condition
    	if(!lock.tryLock(1, TimeUnit.SECONDS)) {
    		throw new ScanningException(this, "Internal Error - Could not obtain lock to run device!");    		
    	}
    	try {
    		if (!getDeviceState().isRunning() && getDeviceState()!=DeviceState.READY) {
    			throw new Exception("The scan state is "+getDeviceState());
    		}
       	    if (awaitPaused) {
        		if (getDeviceState() != DeviceState.PAUSED) setDeviceState(DeviceState.PAUSED);
        		manager.invoke(ScanPause.class);
        		paused.await();
        		getBean().setStatus(Status.RESUMED);
        		setDeviceState(DeviceState.RUNNING);
        		manager.invoke(ScanResume.class);
        	}
    	} finally {
    		lock.unlock();
    	}
    	return true;
	}

	// TODO Abort can be interpreted different ways. As 'skip' for short exposure experiments
	// it finishes the current frame, writes file and stops motors. For long exposure it might
	// need to stop the detector exposing further.
	
	// TODO Abort can stop everything, including detectors motors and file writing immediately.
	// Should the model define the behaviour of abort for a given detector? This would allow
	// abort to be configurable for different detectors.
	
	@Override
	public void abort() throws ScanningException {
		
		try {
			lock.lockInterruptibly();
		} catch (Exception ne) {
			throw new ScanningException(ne);
		}
		
		setDeviceState(DeviceState.ABORTING);
		try {
			awaitPaused = true;
			
			positioner.abort();
			writers.abort();
			runners.abort();
			
			if (getModel().getDetectors()!=null) for (IRunnableDevice<?> device : getModel().getDetectors()) {
				device.abort();
			}

			setDeviceState(DeviceState.ABORTED);
    		manager.invoke(ScanAbort.class);
			RunnableDeviceServiceImpl.setCurrentScanningDevice(null);
			
		} catch (ScanningException s) {
			throw s;
		} catch (Exception ne) {
			throw new ScanningException(ne);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void pause() throws ScanningException {
		
		if (getDeviceState() != DeviceState.RUNNING) {
			throw new ScanningException(this, getName()+" is not running and cannot be paused! The state is "+getDeviceState());
		}
		try {
			lock.lockInterruptibly();
		} catch (Exception ne) {
			throw new ScanningException(ne);
		}
		
		getBean().setPreviousStatus(getBean().getStatus());
		getBean().setStatus(Status.PAUSED);
		setDeviceState(DeviceState.SEEKING);
		try {
			awaitPaused = true;
			if (getModel().getDetectors()!=null) for (IRunnableDevice<?> device : getModel().getDetectors()) {
				if (device instanceof IPausableDevice) ((IPausableDevice)device).pause();
			}
			setDeviceState(DeviceState.PAUSED);
			
		} catch (ScanningException s) {
			throw s;
		} catch (Exception ne) {
			throw new ScanningException(ne);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void seek(int stepNumber) throws ScanningException, InterruptedException {
		// This is the values of all motors at this global (including malcolm) scan
		// position. Therefore we do not need a subscan moderator but can run the iterator
		// to the point
		if (stepNumber<0) throw new ScanningException("Seek position is invalid "+stepNumber);
		if (stepNumber>location.getTotalSize())  throw new ScanningException("Seek position is invalid "+stepNumber);

		// We just changed the location from where we run the scan from and require a new location.
		this.positionIterator = location.createPositionIterator();
		IPosition pos = location.seek(stepNumber, positionIterator);
		positioner.setPosition(pos);
		if (getModel().getDetectors()!=null) for (IRunnableDevice<?> device : getModel().getDetectors()) {
			if (device instanceof IPausableDevice) ((IPausableDevice)device).seek(stepNumber);
		}
	}

	@Override
	public void resume() throws ScanningException {
		
		if (getDeviceState() != DeviceState.PAUSED) {
			throw new ScanningException(this, getName()+" is not paused and cannot be resumed!");
		}
		try {
			lock.lockInterruptibly();
		} catch (Exception ne) {
			throw new ScanningException(ne);
		}
		
		try {
			awaitPaused = false;
			if (getModel().getDetectors()!=null) for (IRunnableDevice<?> device : getModel().getDetectors()) {
				if (device instanceof IPausableDevice) ((IPausableDevice)device).resume();
			}
			paused.signalAll();
			// Notify of running is in checkPaused()
			
		} catch (ScanningException s) {
			throw s;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Actions to perform on a position performed event
	 */
	@Override
	public void positionPerformed(PositionEvent evt) throws ScanningException {
		if (location.isInnerScan()) {
			innerPositionPercentComplete();
		}
	}

	/**
	 * Calculate and set the position complete value on the scan bean based on an inner position
	 * @param innerCount The count representing the progress of of the inner scan
	 * @throws Exception
	 */
	private void innerPositionPercentComplete() {
		
		final ScanBean bean = getBean();
		bean.setMessage("Point " + location.getOverallCount() + " of " + location.getTotalSize());
		bean.setPercentComplete(location.getOuterPercent());
		
		if (getPublisher() != null) {
			try {
				getPublisher().broadcast(bean);
			} catch (EventException e) {
				logger.warn("An error occurred publishing percent complete event ", e);
			}
		}
	}	

	private Collection<IScannable<?>> getScannables(ScanModel model) throws ScanningException {
		final Collection<String>   names = getScannableNames(model.getPositionIterable());
		final Collection<IScannable<?>> ret = new ArrayList<>();
		for (String name : names) ret.add(runnableDeviceService.getDeviceConnectorService().getScannable(name));
		return ret;
	}
	
	private Collection<String> getScannableNames(Iterable<IPosition> gen) {
		
		Collection<String> names = null;
		if (gen instanceof IDeviceDependentIterable) {
			names = ((IDeviceDependentIterable)gen).getScannableNames();
			
		}
		if (names==null) {
			names = model.getPositionIterable().iterator().next().getNames();
		}
		return names;   		
	}
	
	private int getScanRank(Iterable<IPosition> gen) {
		int scanRank = -1;
		if (gen instanceof IDeviceDependentIterable) {
			scanRank = ((IDeviceDependentIterable)gen).getScanRank();
			
		}
		if (scanRank < 0) {
			scanRank = gen.iterator().next().getScanRank();
		}
		if (scanRank < 0) {
			scanRank = 1;
		}
		
		return scanRank;   		
	}

	@Override
	public IPositioner getPositioner() {
		return positioner;
	}

}
