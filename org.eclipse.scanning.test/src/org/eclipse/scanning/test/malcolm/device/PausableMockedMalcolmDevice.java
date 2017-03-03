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
package org.eclipse.scanning.test.malcolm.device;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceOperationCancelledException;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for devices with Pause/Resume capability.
 * The {@link #taskPauseLock} blocks entry into {@link AbstractMalcolmDevice#callableTask} when paused until {@link #resume()} is called.
 * The {@link #runningStateChangeLock} guards the acquisition/release of {@link #taskPauseLock} during transition from RUNNING to PAUSED and vice versa.
 * 
 * @author fri44821
 *
 */
public abstract class PausableMockedMalcolmDevice extends MockedMalcolmDevice {
	
	private static final Logger logger = LoggerFactory.getLogger(PausableMockedMalcolmDevice.class);
	
	private final ReentrantLock taskPauseLock;
	private final Condition taskPauseCompleted;
	private volatile boolean taskPauseRequested = false;				/** indicates the next execution should be paused until {@link #resume()} is called **/
	
	private final ReentrantLock runningStateChangeLock;
	private final Condition runningStateChangeCompleted;
	private volatile boolean runningStateChangeActive = false;		// indicates the device state is transitioning from RUNNING to PAUSED or vice versa
			
	
	/**
	 * Constructor to use in tests to allow them to wait for device state changes via the supplied latch map
	 * 
	 * @param latchMap					// A map of thread id to CountdownLatch to allow external test threads
	 * 									// to wait on device state changes.
	 * 
	 * @throws MalcolmDeviceException	// if connection to the event system cannot be made
	 */
	public PausableMockedMalcolmDevice(String name, final LatchDelegate latcher) throws ScanningException {
		super(name, latcher);
		taskPauseLock = new ReentrantLock(true);
		taskPauseCompleted = taskPauseLock.newCondition();
		runningStateChangeLock = new ReentrantLock(true);
		runningStateChangeCompleted = runningStateChangeLock.newCondition();
	}
	
	/**
	 * Sets the device state as required taking account of any trailing {@link #runningStateChangeCompleted} awaits
	 */
	public void setState(final DeviceState state) throws ScanningException {
		
        if (!state.isRunning()) {				// Clear up any trailing awaits from previous Run/Pause sequences
        	clearRunningStateChangeActive();	// when entering a non-running state e.g when extra threads have
        }										// tried to pause completed tasks
        super.setDeviceState(state);
	}

	/**
	 * Wrapper method to allow subclasses to enact the task block via the beforeExecute hook. Pause/resume are
	 * handled by {@link #beforeExecute()} and the task only proceeds if the out come of this is the RUNINNG state
	 * 
	 * @return	The return value of the underlying Callable if required
	 * @throws MalcolmDeviceException
	 */
	protected Long executeTask() throws MalcolmDeviceException {
		Long retObject = null;
		try {
			beforeExecute();
			if (getState().equals(DeviceState.RUNNING)) {		//Only proceed with the task if we're in the right state
				if (acquireRunLock()) {
					try {
						retObject = callableTask.call();						
					}
					finally {
						releaseRunLock();
					}
				}
				else {
					throw new MalcolmDeviceException(this, "Could not acquire the task run lock");					
				}
			}
			afterExecute();
		} 
		catch (Exception e) {
			throw new MalcolmDeviceException(this, "Could not execute the task", e );
		}
		return retObject;
	}

	/**
	 * Enacts any requested pausing of the thread before it attempts to run the task block. After the PAUSE
	 * phase the state is set to RUNNING only if nothing has modified the state in the meantime.
	 *  
	 * @throws Exception
	 */
	protected void beforeExecute() throws Exception {
        logger.debug("Entering beforeExecute lock, state is " + getState());
        
    	if(!taskPauseLock.tryLock(1, TimeUnit.SECONDS)) {
    		throw new MalcolmDeviceException(this, "Could not obtain lock to read isPaused within 1 second");    		
    	}

    	try {
    		while (taskPauseRequested) {  			
        		setState(DeviceState.PAUSED, "Scan has been paused and will be resumed when the run method is called on the same thread.");
        		
        		clearRunningStateChangeActive();		// signal that state transition to PAUSED has completed
				taskPauseCompleted.await();				// wait for pausing thread to signal resume
				
				// only proceed with run if nothing has changed the state during the pause (e.g. abort)
				if (getState().equals(DeviceState.PAUSED)) {
					setState(DeviceState.RUNNING, "Scan has been unpaused and will resume.");
				}
        		clearRunningStateChangeActive();
			}
    	}
    	catch (InterruptedException ie) {
    		Thread.currentThread().interrupt();	    		
    	}
    	finally {
    		taskPauseLock.unlock();
    	}	
	}	

	protected Thread pauseThread;
	
	/**
	 * Requests that the task thread be paused at the start of its next run
	 */
	public void pause() throws ScanningException {
		if (getState() != DeviceState.RUNNING) {
			throw new MalcolmDeviceException(this, "Device is in state "+getState()+" which cannot be paused!");
		}

		setRunningStateChangeActive();							// start the state transition to PAUSED
		
		setState(DeviceState.SEEKING);
		try {
			taskPauseLock.lockInterruptibly();					// Attempt to start task-pause request allowing for abort							
		} catch (InterruptedException e) {
			throw new MalcolmDeviceException(this, "Paused lock has been interrupted trying to acquire lock!", e);
		}
		
		try {
			taskPauseRequested = true;							// Trigger a pause [await()] when next in beforeExecute
			pauseThread = Thread.currentThread();			
			setState(DeviceState.PAUSED);
		} finally {
			releaseStateChangeInterlock("pause", pauseThread.getId());
	    }
	}

	/**
	 * Clears the pause request 
	 */
	public void resume()throws MalcolmDeviceException {
		long id = pauseThread.getId();
		if (getState() != DeviceState.PAUSED) {
			throw new MalcolmDeviceException(this, "Device is not paused and cannot be resumed!");
		}
		
		setRunningStateChangeActive();							// start the state transition to RUNNING
		
		try {
			taskPauseLock.lockInterruptibly();					// Attempt to start task-resume request allowing for abort							
		} 
		catch (InterruptedException e) {
			throw new MalcolmDeviceException(this, "Paused lock has been interrupted trying to acquire lock!", e);
		}
		
		try {
			taskPauseRequested = false;
			pauseThread = null;
			taskPauseCompleted.signalAll();						// Cancel the await() triggered by pause()
		} 
		finally {
			releaseStateChangeInterlock("resume", id);			
		}
	}
	
	@Override
	public void abort() throws ScanningException {
		
		if (!getState().isAbortable()) {
			throw new MalcolmDeviceException(this, "Device is in state "+getState()+" which cannot be aborted!");
		}
		
		setState(DeviceState.ABORTING); // Tells any running loops that we are killing it
		try {
			if (taskPauseLock.tryLock() || taskPauseLock.tryLock(5, TimeUnit.SECONDS)) {
				try {
					setState(DeviceState.ABORTED);
				}
				finally {
					taskPauseLock.unlock();
				}
			} else {
				// We hope that the state is paused and the thread that paused
				// can be interrupted.
				if (pauseThread!=null) {
					pauseThread.interrupt();
					setState(DeviceState.ABORTED);
				} else {
				    throw new MalcolmDeviceException(this, "Another thread has paused the device and it cannot be aborted!");
				}
			}
			
		} catch (InterruptedException e) {
			throw new MalcolmDeviceException(this, "Lock waiting for abort interupted!", e);
		}
	}

	/**
	 * Releases the task pause lock having first initiated a wait for
	 * confirmation of the change of state this will produce.
	 * 
	 * @param operation		The state change being performed (pause or resume)
	 * @param id			The id of the current thread
	 * 
	 * @throws MalcolmDeviceOperationCancelledException		if the device has completed its running activities during the await 
	 */
	private void releaseStateChangeInterlock(final String operation, final long id) throws MalcolmDeviceOperationCancelledException {
		runningStateChangeLock.lock();						// Guard state change confirmation		
		taskPauseLock.unlock();								// Pause/Resume has been requested - beforeExecute can now respond
		try {
			while (runningStateChangeActive) {
				logger.debug("Await transition " + operation + " id=" + id);
				
				runningStateChangeCompleted.await();		// Wait for beforeExecute to signal PAUSED/RUNNING has been set
				if (getState().isBeforeRun()) {
					throw new MalcolmDeviceOperationCancelledException(operation + " operation has been cancelled");
				}
			}
		} 
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		finally {
			runningStateChangeLock.unlock();				// state change successfully confirmed				
		}		
	}
	
	/**
	 * Sets the guarded flag that indicates a transition
	 * from RUNNING to PAUSED or vice-versa is underway
	 */
	private void setRunningStateChangeActive() {
		runningStateChangeLock.lock();
		try {
			runningStateChangeActive = true;
		}
		finally {
			runningStateChangeLock.unlock();
		}
	}

	/**
	 * Clears the guarded flag that indicates a transition
	 * from RUNNING to PAUSED or vice-versa is underway
	 */
	private void clearRunningStateChangeActive() {
		if (runningStateChangeLock==null) return;
		runningStateChangeLock.lock();
		try {
			runningStateChangeActive = false;
			runningStateChangeCompleted.signalAll();
		}
		finally {
			runningStateChangeLock.unlock();
		}
	}
}
