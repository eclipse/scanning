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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.scanning.api.IConfigurable;
import org.eclipse.scanning.api.ILevel;
import org.eclipse.scanning.api.IModelProvider;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.IValidator;
import org.eclipse.scanning.api.device.models.IDeviceRoleActor;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;


/**
 *
 * An IDevice is the runner for the whole scan but also for individual
 * detectors. Detectors, for instance an IMalcolmDevice can be run in
 * the system as if it were an IDetector.
 *
 * Anatomy of a CPU scan (non-malcolm)
 *
 *  <br>
 *&nbsp;_________<br>
 *_|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|________  collectData() Tell detector to collect<br>
 *&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;_________<br>
 *_________|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|_  readout() Tell detector to readout<br>
 *&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;_______<br>
 *_________|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|___  moveTo()  Scannables move motors to new position<br>
 * <br>
 *<br>
 * A MalcolmDevice is also an IDetector which may operate with an arbitrary model, usually driving hardware.<br>
 * <br>
 * <usage><code>
 * IParserService pservice = ...// OSGi<br>
 * <br>
 * // Parse the scan command, throws an exception<br>
 * IParserResult<StepModel> parser = pservice.createParser(...)<br>
 * // e.g. "scan x 0 5 0.1 analyser"<br>
 * <br>
 * // Now use the parser to create a generator<br>
 * IPointGeneratorService gservice = ...// OSGi<br>
 * StepModel model = parser.getModel("x");<br>
 * Iterable<IPosition> gen = gservice.createGenerator(model)<br>
 * <br>
 * // Now scan the point iterator<br>
 * IDeviceService sservice = ...// OSGi<br>
 * IRunnableDevice<ScanModel> scanner = sservice.createScanner(...);<br>
 * scanner.configure(model);<br>
 * scanner.run();<br>
 *
 * </code></usage>
 *
 * <img src="./doc/device_state.png" />
 *
 * @author Matthew Gerring
 *
 */
public interface IRunnableDevice<T> extends INameable, IDeviceRoleActor, ILevel, IConfigurable<T>, IResettableDevice, IValidator<T>, IModelProvider<T> {

	/**
	 *
	 * @return the current device State. This is not the same as the Status of the scan.
	 */
	public DeviceState getDeviceState() throws ScanningException;

	/**
	 *
	 * @return the current device Health.
	 */
	public String getDeviceHealth() throws ScanningException;

	/**
	 *
	 * @return the current value of the device 'busy' flag.
	 */
	public boolean isDeviceBusy() throws ScanningException;

	/**
	 * This method is the same as calling run(null). I.e. run without specifying start position.
	 *
	 * @throws ScanningException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws ExecutionException
	 */
	default void run() throws ScanningException, InterruptedException, TimeoutException, ExecutionException {
		run(null);
	}

	/**
	 * Blocking call to execute the scan. The position specified may be null.
	 *
	 * @throws ScanningException
	 */
	public void run(IPosition position) throws ScanningException, InterruptedException, TimeoutException, ExecutionException;

	/**
	 * The default implementation of start simply executes run in a thread named using the getName() value.
	 * @param pos
	 * @throws ScanningException
	 * @throws InterruptedException
	 */
	default void start(final IPosition pos) throws ScanningException, InterruptedException, TimeoutException, ExecutionException {
		final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>(1));
		final Thread thread = new Thread(() -> {
			try {
				IRunnableDevice.this.run(pos);
			} catch (ScanningException | InterruptedException | TimeoutException | ExecutionException e) {
				// If you add an exception type to this catch clause,
				// you must also add an "else if" clause for it inside
				// the "if (!exceptions.isEmpty())" conditional below.
				exceptions.add(e);
			}
		}, getName()+" execution thread");
		thread.start();

		// Re-throw any exception from the thread.
		createException(exceptions);
	}

	/**
	 * Creates an exception out of a list of exceptions and throws it if
	 * the list is not empty.
	 *
	 * @param exceptions
	 * @throws ScanningException
	 * @throws InterruptedException
	 */
	default void createException(List<Throwable> exceptions) throws ScanningException, InterruptedException, TimeoutException, ExecutionException {

		if (!exceptions.isEmpty()) {
			Throwable ex = exceptions.get(0);

			// We must manually match the possible exception types because Java
			// doesn't let us do List<Either<ScanningException, InterruptedException>>.
			if (ex.getClass() == ScanningException.class) {
				throw (ScanningException) ex;

			} else if (ex.getClass() == InterruptedException.class) {
				throw (InterruptedException) ex;

			} else if (ex.getClass() == TimeoutException.class) {
				throw (TimeoutException) ex;

			} else  if (ex.getClass() == ExecutionException.class) {
				throw (ExecutionException) ex;
			} else {
				throw new IllegalStateException();
			}
		}

	}

	/**
	 * Call to terminate the scan before it has finished.
	 *
	 * @throws ScanningException
	 */
	public void abort() throws ScanningException, InterruptedException;

	/**
	 * Call to disable the device, stopping all activity.
	 *
	 * @throws ScanningException
	 */
	public void disable() throws ScanningException;

	/**
	 * Latches until this run is complete if it was initiated from a start.
	 * If a device does not have a latch, then this method always throws an exception.
	 *
	 * @throws ScanningException
	 */
	default void latch() throws ScanningException, InterruptedException, TimeoutException, ExecutionException {
		throw new ScanningException("Latch is not implemnented for "+getClass().getSimpleName());
	}

	default boolean latch(long time, TimeUnit unit) throws ScanningException, InterruptedException, TimeoutException, ExecutionException	{
		throw new ScanningException("Latch is not implemnented for "+getClass().getSimpleName());
	}
	/**
	 * The model being used for the device.
	 * @return
	 */
	@Override
	public T getModel();

	/**
	 * Gets whether the device is 'alive' or not. 'Alive' is taken to mean that the device is on and responding.
	 * @return
	 */
	public boolean isAlive();

	/**
	 * Sets whether the device is alive or not
	 * @param alive
	 */
	public void setAlive(boolean alive);
}
