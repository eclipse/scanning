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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.event.IPositioner;
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PositionerAtomProcess reads the values included in a {@link PositionerAtom} 
 * and instructs the positioners detailed in the atom to set these positions.
 * 
 * It uses the server's {@link IRunnableDeviceService} to create an 
 * {@link IPositioner} which it passes the target positions to.
 * 
 * TODO Implement pausing/resuming when implemented in the IPositioner system.
 * 
 * @author Michael Wharmby
 * 
 * @param <T> The {@link Queueable} specified by the {@link IConsumer} 
 *            instance using this PositionerAtomProcess. This will be 
 *            {@link QueueAtom}.
 */
public class PositionerAtomProcess<T extends Queueable> extends QueueProcess<PositionerAtom, T> {
	
	/**
	 * Used by {@link QueueProcessFactory} to identify the bean type this 
	 * {@link QueueProcess} handles.
	 */
	public static final String BEAN_CLASS_NAME = PositionerAtom.class.getName();
	
	private static Logger logger = LoggerFactory.getLogger(PositionerAtomProcess.class);
	
	//Scanning infrastructure
	private final IRunnableDeviceService deviceService;
	private IPositioner positioner;
	
	/**
	 * Create a PositionerAtomProcess to position motors on the beamline. 
	 * deviceService ({@link IRunnableDeviceService}) is configured using OSGi 
	 * through {@link ServicesHolder}.
	 */
	public PositionerAtomProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher, blocking);
		//Get the deviceService from the OSGi configured holder.
		deviceService = ServicesHolder.getRunnableDeviceService();
		
		//We make direct calls to scanning infrastructure which block -> need to happen in separate thread
		runInThread = true;
	}

	@Override
	protected void run() throws EventException, InterruptedException {
		logger.debug("Creating target position object from configured values");
		broadcast(Status.RUNNING,"Creating target position from configured values");
		
		final IPosition target = new MapPosition(queueBean.getPositionerConfig());
		broadcast(10d);
		if (isTerminated()) throw new InterruptedException("Termination requested");
		
		//Get the positioner
		logger.debug("Getting device positioner");
		broadcast(Status.RUNNING, "Getting device positioner");
		try {
			positioner = deviceService.createPositioner();
			final double configCompletePercent = 20d, positionCompletePercent = 99.5 - configCompletePercent; //Bean must be 99.5% complete for QueueProcess to mark Status.COMPLETE
			final Map<String, Object> initialPositions = getDeviceInitialPositions(target);
			final Map<String, Object> initialTargetDifference = findInitialTargetDifference(initialPositions, target);
			if (isTerminated()) throw new InterruptedException("Termination requested");
			
			positioner.addPositionListener(new IPositionListener() {
				

				@Override
				public void positionChanged(PositionEvent evt) throws ScanningException {
					beanProgressUpdate(evt);
				}

				@Override
				public void levelPerformed(PositionEvent evt) throws ScanningException {
					beanProgressUpdate(evt);
				}

				@Override
				public void positionPerformed(PositionEvent evt) throws ScanningException {
					//We completed the move, time to hand control back
					try {
						broadcast(99.5);
					} catch (EventException evEx) {
						logger.error("Broadcasting bean state failed with: "+evEx.getMessage());
						throw new ScanningException("Broadcasting bean state failed with: "+evEx.getMessage(), evEx);
					}
				}

				/**
				 * Determine the fractional completeness of the position 
				 * setting and update the {@link PositionerAtom} percent 
				 * complete. A 'configCompletePercent' amount of percent 
				 * complete is set prior to the move starting, so it is only 
				 * the remainder which changes as the position setting 
				 * progresses. 
				 * 
				 * @param evt PositionEvent to analyse
				 * @throws ScanningException if broadcasting of the bean fails
				 */
				private void beanProgressUpdate(PositionEvent evt) throws ScanningException {
					IPosition currentPosition = evt.getPosition();
					double targetCompleteness = calculateCompleteness(currentPosition, initialPositions, initialTargetDifference);
					
					try {
						broadcast(configCompletePercent + (positionCompletePercent * targetCompleteness));
					} catch (EventException evEx) {
						logger.error("Broadcasting bean state failed with: "+evEx.getMessage());
						throw new ScanningException("Broadcasting bean state failed with: "+evEx.getMessage(), evEx);
					}
				}
				
				/**
				 * Calculate the amount of the position setting that has been completed, 
				 * returned as a fraction.
				 * 
				 * @param current IPosition
				 * @param initial Map<String, Object> positions of all scannables
				 * @param initialDelta difference between initial and target positions
				 * @return double fraction of the move completed
				 */
				private double calculateCompleteness(IPosition current, Map<String, Object> initial, Map<String, Object> initialDelta) {
					List<Double> arr = new ArrayList<>();

					for (String name : initialDelta.keySet()) {
						try {
							arr.add((current.getValue(name) - ((Number)initial.get(name)).doubleValue()) / ((Number)initialDelta.get(name)).doubleValue());
						} catch (ClassCastException ccEx) {
							if (current.get(name).equals(initialDelta.get(name))) {
								arr.add(1d);
							} else {
								arr.add(0d);
							}
						}
					}
					return arr.stream().collect(Collectors.averagingDouble(val -> val));
				}
			});
			
			if (isTerminated()) throw new InterruptedException("Termination requested");
			broadcast(configCompletePercent);
			
			//Set the positioner
			logger.debug("Setting device(s) to requested position");
			broadcast(Status.RUNNING, "Moving device(s) to requested position");
			if (isTerminated()) throw new InterruptedException("Termination requested");
			positioner.setPosition(target);
		} catch (ScanningException se) {
			//Aborting setPosition causes it to throw a scanning exception. 
			//We expect this when we terminate, so ignoring the exception is fine.
			if (!isTerminated()) {
				broadcast(Status.FAILED, "Failed to set device positioner: '"+se.getMessage()+"'");
				logger.error("Failed to set device positioner in '"+queueBean.getName()+"': "+se.getMessage());
			}
		}
	}
	
	@Override
	public void postMatchCompleted() {
		updateBean(Status.COMPLETE, 100d, "Set position completed successfully");
	}

	@Override
	public void postMatchTerminated() {
		queueBean.setMessage("Position change aborted before completion (requested)");
	}

	@Override
	public void postMatchFailed() {
		positioner.abort();
	}
	
	@Override
	protected void terminateCleanupAction() throws EventException {
		positioner.abort(); //<--since setPosition is blocking we need to abort it.
	}
	
	@Override
	protected void doPause() throws EventException {
		if (finished) return; //Stops spurious messages/behaviour when processing already finished
		//TODO
		logger.error("Pause/resume not implemented on PositionerAtom");
	}

	@Override
	protected void doResume() throws EventException {
		if (finished) return; //Stops spurious messages/behaviour when processing already finished
		//TODO
		logger.error("Pause/resume not implemented on PositionerAtom");
	}
	
	@Override
	public Class<PositionerAtom> getBeanClass() {
		return PositionerAtom.class;
	}
	
	/**
	 * Finds the initial positions of all of the scannables listed in the 
	 * target IPosition object and returns them as a Map of their scannable 
	 * names against the position values.
	 * 
	 * @param target IPosition supplied by {@link PositionerAtom}
	 * @return Map<String, Object> scannable names and positions
	 * @throws EventException when the scannable name could not be found or 
	 * the position could not be returned
	 */
	private Map<String, Object> getDeviceInitialPositions(IPosition target) throws EventException {
		IScannableDeviceService connectorService = ServicesHolder.getScannableDeviceService();
		final Map<String, Object> initialPositions = new HashMap<>();
		for (String name : target.getNames())
			try {
				initialPositions.put(name, connectorService.getScannable(name).getPosition());
			} catch (ScanningException ex) {
				logger.error("Scannable device service could not find scannable '"+name+"': "+ex.getMessage());
				throw new EventException("Scannable device service could not find scannable '"+name+"'", ex);
			} catch (Exception ex) {
				logger.error("Failed to get initial position of scannable '"+name+"': "+ex.getMessage());
				throw new EventException("Failed to get initial position of scannable '"+name+"'", ex);
			}
		return initialPositions;

	}
	
	/**
	 * Calculate the difference between the initial positions and the target 
	 * positions of all the scannables named in the target {@link IPosition} 
	 * from the {@link PositionerAtom}.
	 * 
	 * @param initial Map<String, Object> the initial positions of named 
	 *        scannables
	 * @param targetPos IPosition target from {@link PositionerAtom}
	 * @return Map<String, Object> position difference to change from initial 
	 *         position to target
	 */
	private Map<String, Object> findInitialTargetDifference(Map<String, Object> initial, IPosition targetPos) throws EventException {
		HashMap<String, Object> diff = new HashMap<>();

		for (String name : targetPos.getNames()) {
			Object posDiff;
			try {
				posDiff = targetPos.getValue(name) - ((Number)initial.get(name)).doubleValue();
			} catch (ClassCastException ccEx) {
				posDiff = targetPos.get(name);
			}
			diff.put(name, posDiff);
		}
		return diff;
	}

}
