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
package org.eclipse.scanning.malcolm.core;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.MalcolmModel;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.attributes.IDeviceAttribute;
import org.eclipse.scanning.api.malcolm.attributes.MalcolmAttribute;
import org.eclipse.scanning.api.malcolm.attributes.NumberAttribute;
import org.eclipse.scanning.api.malcolm.connector.IMalcolmConnectorService;
import org.eclipse.scanning.api.malcolm.connector.MalcolmMethod;
import org.eclipse.scanning.api.malcolm.event.IMalcolmListener;
import org.eclipse.scanning.api.malcolm.event.MalcolmEvent;
import org.eclipse.scanning.api.malcolm.event.MalcolmEventBean;
import org.eclipse.scanning.api.malcolm.message.MalcolmMessage;
import org.eclipse.scanning.api.malcolm.message.MalcolmUtil;
import org.eclipse.scanning.api.malcolm.message.Type;
import org.eclipse.scanning.api.points.IMutator;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.sequencer.SubscanModerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object that make the connection to the device and monitors its status.
 *
 * @author Matthew Gerring
 */
public class MalcolmDevice<M extends MalcolmModel> extends AbstractMalcolmDevice<M> {

	private static final Logger logger = LoggerFactory.getLogger(MalcolmDevice.class);

	// Constants
	private static final String STATE_ENDPOINT = "state";
	private static final String HEALTH_ENDPOINT = "health";
	private static final String CURRENT_STEP_ENDPOINT = "completedSteps";
	private static final String FILE_EXTENSION_H5 = "h5";
	private static final String STANDARD_MALCOLM_ERROR_STR = "Error from Malcolm Device Connection: ";

	// Frequencies and Timeouts
	// broadcast every 250 milliseconds
	private static final long POSITION_COMPLETE_FREQ = Long.getLong("org.eclipse.scanning.malcolm.core.positionCompleteFrequency", 250);

	private static enum Timeout {
		STANDARD("org.eclipse.scanning.malcolm.core.timeout", Duration.ofSeconds(5)),
		CONFIG("org.eclipse.scanning.malcolm.core.configureTimeout", Duration.ofMinutes(10)),
		RUN("org.eclipse.scanning.malcolm.core.runTimeout", Duration.ofDays(2));

		private final long timeoutMillis;

		Timeout(String propertyName, Duration defaultTimeout) {
			timeoutMillis = Long.getLong(propertyName, defaultTimeout.toMillis());
		}

		public long toMillis() {
			return timeoutMillis;
		}
	}


	// Standard timeout for Malcolm Calls

	// Subscriber messages
    private MalcolmMessage stateSubscriber;
    private MalcolmMessage scanSubscriber;

    // Our connection to the outside.
	private IPublisher<ScanBean> publisher;

	// Data should be in model?
	private MalcolmEventBean meb;
	private Iterator<IPosition> scanPositionIterator;

	// Local data.
	private long lastBroadcastTime = System.currentTimeMillis();
	private int lastUpdateCount = 0;
	private boolean succesfullyInitialised = false;
	private boolean subscribedToStateChange = false;


	public MalcolmDevice() throws MalcolmDeviceException {
		super(Services.getConnectorService(), Services.getRunnableDeviceService());
	}

	public MalcolmDevice(String name,
			IMalcolmConnectorService<MalcolmMessage> service,
			IRunnableDeviceService runnableDeviceService,
			IPublisher<ScanBean> publisher) throws MalcolmDeviceException {
		super(service, runnableDeviceService);
		setName(name);
		this.publisher = publisher;
		setAlive(false);
	}

	@Override
	public void register() {
		try {
			super.register();
			initialize();
		} catch (MalcolmDeviceException e) {
			logger.error("Could not initialize malcolm device " + getName(), e);
		}
	}

	public void initialize() throws MalcolmDeviceException {
		logger.debug("initialize() called");
		try {
			setAlive(false);
		final DeviceState currentState = getDeviceState();
			logger.debug("Connecting to ''{}''. Current state: {}", getName(), currentState);

			stateSubscriber = createSubscribeMessage(STATE_ENDPOINT);
			subscribe(stateSubscriber, this::sendScanStateChange);

			scanSubscriber  = createSubscribeMessage(CURRENT_STEP_ENDPOINT);
			subscribe(scanSubscriber, this::sendScanEvent);

			succesfullyInitialised = true;
			setAlive(true);

		} finally {
			if (!subscribedToStateChange) {
				subscribedToStateChange = true;

				final Runnable subscribeTask = () -> {
					try {
						subscribeToConnectionStateChange(
								e -> handleConnectionStateChange(e.getBean()));
						handleConnectionStateChange(true);
						setAlive(true);
					} catch (MalcolmDeviceException ex) {
						logger.error("Unable to subsribe to state change on '" + getName() + "'", ex);
					}
				};

				final Thread subscriberThread = new Thread(subscribeTask);
				subscriberThread.setDaemon(true);
				subscriberThread.start();
			}
		}

	}

	/**
	 * Called at the start of each point of the outer scan due to the {@link PointStart} attribute.
	 * Gets the iterator of the points of the inner scan from the {@link SubscanModerator}.
	 * @param moderator the SubscanModerator
	 */
    @PointStart
    public void scanPoint(SubscanModerator moderator) {
    	final Iterable<IPosition> scanPositions = moderator.getInnerIterable();
        scanPositionIterator = scanPositions.iterator();
    }

	protected void sendScanEvent(MalcolmEvent<MalcolmMessage> event) {
		final MalcolmMessage msg = event.getBean();
		DeviceState newState;
		try {
			newState = MalcolmUtil.getState(msg, false);
		} catch (Exception e1) {
			logger.error("Could not get state", e1);
			return;
		}

		ScanBean bean = getBean();
		bean.setDeviceName(getName());
		bean.setPreviousDeviceState(bean.getDeviceState());
		if (newState!=null) {
			bean.setDeviceState(newState);
		}

		Integer point = bean.getPoint();
		boolean newPoint = false;
		Object value = msg.getValue();
		if (value instanceof Map) {
			point = (Integer)((Map<?,?>)value).get("value");
			bean.setPoint(point);
			newPoint = true;
		} else if (value instanceof NumberAttribute) {
			point = (Integer)((NumberAttribute)value).getValue();
			bean.setPoint(point);
			newPoint = true;
		}

		// Fire a position complete only if it's past the timeout value
		if (newPoint && scanPositionIterator != null) {
			long currentTime = System.currentTimeMillis();

			int positionDiff = point - lastUpdateCount;

			IPosition scanPosition = null;
			for (int i = 0; i < positionDiff; i++) {
				if (scanPositionIterator.hasNext()) {
					scanPosition = scanPositionIterator.next();
				}
			}

			lastUpdateCount = point;

			// fire position complete event to listeners
			if (scanPosition != null && currentTime - lastBroadcastTime >= POSITION_COMPLETE_FREQ) {
				scanPosition.setStepIndex(point);
				try {
					firePositionComplete(scanPosition);
				} catch (ScanningException e) {
					logger.error("Exception firing position complete", e);
				}

				lastBroadcastTime = System.currentTimeMillis();
			}
		}

		// publish the scan bean
		if (publisher != null) {
			try {
				publisher.broadcast(bean);
			} catch (Exception e) {
				logger.error("Could not publish bean");
			}
		}
	}

	protected void sendScanStateChange(MalcolmEvent<MalcolmMessage> event) {

		try {
			MalcolmMessage msg = event.getBean();
			DeviceState newState = MalcolmUtil.getState(msg, false);

			// Send scan state changed
			ScanBean bean = getBean();
			bean.setDeviceName(getName());
			bean.setPreviousDeviceState(bean.getDeviceState());
			bean.setDeviceState(newState);
			if (publisher!=null) publisher.broadcast(bean);

			// We also send a malcolm event
			if (meb==null) meb = new MalcolmEventBean();
			meb.setDeviceName(getName());
			meb.setMessage(msg.getMessage());

			meb.setPreviousState(meb.getDeviceState());
			meb.setDeviceState(newState);

			if (msg.getType().isError()) { // Currently used for debugging the device.
				logger.error("Error message encountered: {}", msg);
				Thread.dumpStack();
			}

			eventDelegate.sendEvent(meb);
		} catch (Exception e) {
			logger.error("Could not send scan state change message");
		}
	}

	/**
	 * Handle a change in the connection state of this device.
	 * Event is sent by the communications layer.
	 * @param connected true if the device has changed to being connected
	 */
	private void handleConnectionStateChange(boolean connected) {
		try {
			setAlive(connected);
			if (connected) {
				logger.info("Malcolm Device '" + getName() + "' connection state changed to connected");
				java.awt.EventQueue.invokeLater(() -> {
					try {
						if (!succesfullyInitialised) {
							initialize();
						} else {
							getDeviceState();
						}
					} catch (MalcolmDeviceException ex) {
						logger.warn("Unable to initialise/getDeviceState for device '" + getName() + "' on reconnection", ex);
					}
			    });
			} else {
				logger.warn("Malcolm Device '{}' connection state changed to not connected", getName());
			}
		} catch (Exception ne) {
			logger.error("Problem dispatching message!", ne);
		}
	}

	@Override
	public DeviceState getDeviceState() throws MalcolmDeviceException {
		try {
			final MalcolmMessage message = createGetMessage(STATE_ENDPOINT);
			final MalcolmMessage reply   = send(message, Timeout.STANDARD.toMillis());
			if (reply.getType()==Type.ERROR) {
				throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
			}
			return MalcolmUtil.getState(reply);

		} catch (MalcolmDeviceException mne) {
			throw mne;

		} catch (Exception ne) {
			throw new MalcolmDeviceException(this, "Cannot connect to device '" + getName() + "'", ne);
		}
	}

	@Override
	public String getDeviceHealth() throws MalcolmDeviceException {
		try {
			final MalcolmMessage message = createGetMessage(HEALTH_ENDPOINT);
			final MalcolmMessage reply   = send(message, Timeout.STANDARD.toMillis());
			if (reply.getType()==Type.ERROR) {
				throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
			}

			return MalcolmUtil.getHealth(reply);

		} catch (MalcolmDeviceException mne) {
			throw mne;

		} catch (Exception ne) {
			throw new MalcolmDeviceException(this, "Cannot connect to device '" + getName() + "'", ne);
		}
	}

	@Override
	public boolean isDeviceBusy() throws MalcolmDeviceException {
		return !getDeviceState().isRestState();
	}

	@Override
	public void validate(M params) throws ValidationException {
		logger.debug("validate() called");
		validateWithReturn(params);
	}

	@Override
	public Object validateWithReturn(M params) throws ValidationException {
		if (Boolean.getBoolean("org.eclipse.scanning.malcolm.skipvalidation")) {
			logger.warn("Skipping Malcolm Validate");
			return null;
		}

		final EpicsMalcolmModel epicsModel = createEpicsMalcolmModel(params);
		MalcolmMessage reply = null;
		try {
			final MalcolmMessage msg   = createCallMessage(MalcolmMethod.VALIDATE, epicsModel);
			reply = send(msg, Timeout.STANDARD.toMillis());
			if (reply.getType()==Type.ERROR) {
				throw new ValidationException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
			}

		} catch (Exception mde) {
			throw new ValidationException(mde);
		}

		return reply.getRawValue();
	}

	@Override
	public void configure(M model) throws MalcolmDeviceException {
		logger.debug("configure() called");

		// Reset the device before configure in case it's in a fault state
		try {
			reset();
		} catch (Exception ex) {
			// Swallow the error as it might throw one if in a non-resetable state
		}

		final EpicsMalcolmModel epicsModel = createEpicsMalcolmModel(model);
		final MalcolmMessage msg   = createCallMessage(MalcolmMethod.CONFIGURE, epicsModel);
		MalcolmMessage reply = wrap(()->send(msg, Timeout.CONFIG.toMillis()));
		if (reply.getType() == Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}
		setModel(model);
		resetProgressCounting();
	}

	/**
	 * Reset any variables used in counting progress
	 */
	private void resetProgressCounting() {
		scanPositionIterator = null;
		lastUpdateCount = 0;
	}

	private EpicsMalcolmModel createEpicsMalcolmModel(M model) {
		double exposureTime = model.getExposureTime();

		if (pointGenerator != null) {
			List<IMutator> mutators = new ArrayList<>();
			((CompoundModel<?>) pointGenerator.getModel()).setMutators(mutators);
			((CompoundModel<?>) pointGenerator.getModel()).setDuration(exposureTime);
		}

		final String fileDir = model.getFileDir();
		String fileTemplate = null;
		if (fileDir != null) {
			fileTemplate = new File(fileDir).getName() + "-%s." + FILE_EXTENSION_H5;
		}

		return new EpicsMalcolmModel(fileDir, fileTemplate, model.getAxesToMove(), pointGenerator);
	}

	@Override
	public void run(IPosition pos) throws MalcolmDeviceException, InterruptedException, ExecutionException, TimeoutException {
		logger.debug("run() called with position {}", pos);
		MalcolmMessage reply = call(MalcolmMethod.RUN, Timeout.RUN.toMillis(), DeviceState.RUNNING);
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}
	}

	@Override
	public void seek(int stepNumber) throws MalcolmDeviceException {
		logger.debug("seek() called with step number {}", stepNumber);
		LinkedHashMap<String, Integer> seekParameters = new LinkedHashMap<>();
		seekParameters.put(CURRENT_STEP_ENDPOINT, stepNumber);
		final MalcolmMessage msg   = createCallMessage(MalcolmMethod.PAUSE, seekParameters);
		final MalcolmMessage reply = wrap(()->send(msg, Timeout.CONFIG.toMillis()));
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}
	}

	@Override
	public void abort() throws MalcolmDeviceException {
		logger.debug("abort() called");
		MalcolmMessage reply = wrap(()->call(MalcolmMethod.ABORT, Timeout.STANDARD.toMillis()));
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}
	}

	@Override
	public void disable() throws MalcolmDeviceException {
		logger.debug("disable() called");
		MalcolmMessage reply = wrap(()->call(MalcolmMethod.DISABLE, Timeout.STANDARD.toMillis()));
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}
	}

	@Override
	public void reset() throws MalcolmDeviceException {
		logger.debug("reset() called");
		MalcolmMessage reply = wrap(()->call(MalcolmMethod.RESET, Timeout.STANDARD.toMillis()));
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}
	}

	@Override
	public void pause() throws MalcolmDeviceException {
		logger.debug("pause() called");
		MalcolmMessage reply = wrap(()->call(MalcolmMethod.PAUSE, Timeout.CONFIG.toMillis()));
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}
	}

	@Override
	public void resume() throws MalcolmDeviceException {
		logger.debug("resume() called");
		MalcolmMessage reply = wrap(()->call(MalcolmMethod.RESUME, Timeout.STANDARD.toMillis()));
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}
	}

	@Override
	public void dispose() throws MalcolmDeviceException {
		logger.debug("dispose() called");
		unsubscribe(stateSubscriber);
		unsubscribe(scanSubscriber);

		setAlive(false);
	}

	private final void unsubscribe(MalcolmMessage subscriber) throws MalcolmDeviceException {
		if (subscriber!=null) {
			final MalcolmMessage unsubscribeStatus = createUnsubscribeMessage();
			unsubscribeStatus.setId(subscriber.getId());
			unsubscribe(subscriber);
			logger.debug("Unsubscription {} made {}", getName(), unsubscribeStatus);
		}
	}

	@Override
	public boolean isLocked() throws MalcolmDeviceException {
		final DeviceState state = getDeviceState();
		return state.isTransient(); // Device is not locked but it is doing something.
	}

	@Override
	public DeviceState latch(long time, TimeUnit unit, final DeviceState... ignoredStates) throws MalcolmDeviceException {
		try {
			final CountDownLatch latch = new CountDownLatch(1);
			final List<DeviceState>     stateContainer     = new ArrayList<>(1);
			final List<Exception> exceptionContainer = new ArrayList<>(1);

			// Make a listener to check for state and then add it and latch
			IMalcolmListener<MalcolmMessage> stateChanger = e -> {
				MalcolmMessage msg = e.getBean();
				try {
					DeviceState state = MalcolmUtil.getState(msg);
					if (state != null && ignoredStates != null && Arrays.asList(ignoredStates).contains(state)) {
						return; // Found state that we don't want!
					}
					stateContainer.add(state);
					latch.countDown();

				} catch (Exception ne) {
					exceptionContainer.add(ne);
					latch.countDown();
				}
			};

			subscribe(stateSubscriber, stateChanger);

			boolean countedDown = false;
			if (time>0) {
				countedDown = latch.await(time, unit);
			} else {
				latch.await();
			}

			unsubscribe(stateSubscriber, stateChanger);

			if (!exceptionContainer.isEmpty()) throw exceptionContainer.get(0);

			if (!stateContainer.isEmpty()) return stateContainer.get(0);

			if (countedDown) {
			    throw new MalcolmDeviceException("The countdown of "+time+" "+unit+" timed out waiting for state change for device "+getName());
			} else {
				throw new MalcolmDeviceException("A problem occured trying to latch state change for device "+getName());
			}

		} catch (MalcolmDeviceException ne) {
			throw ne;

		} catch (Exception neOther) {
			throw new MalcolmDeviceException(this, neOther);
		}

	}

	@Override
	public <T> IDeviceAttribute<T> getAttribute(String attributeName) throws MalcolmDeviceException {
		logger.debug("getAttribute() called with attribute name {}", attributeName);
		final MalcolmMessage message = createGetMessage(attributeName);
		final MalcolmMessage reply   = wrap(()->send(message, Timeout.STANDARD.toMillis()));
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}

		Object result = reply.getValue();
		if (!(result instanceof MalcolmAttribute)) {
			throw new MalcolmDeviceException("No such attribute: " + attributeName);
		}

		@SuppressWarnings("unchecked")
		IDeviceAttribute<T> attribute = (IDeviceAttribute<T>) result;
		return attribute;
	}

	@Override
	public List<IDeviceAttribute<?>> getAllAttributes() throws MalcolmDeviceException {
		logger.debug("getAllAttributes() called");
		final MalcolmMessage message = createGetMessage("");
		final MalcolmMessage reply   = wrap(()->send(message, Timeout.STANDARD.toMillis()));
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException(STANDARD_MALCOLM_ERROR_STR + reply.getMessage());
		}


		@SuppressWarnings("unchecked")
		Map<String, Object> wholeBlockMap = (Map<String, Object>) reply.getValue();
		return wholeBlockMap.values().stream().
				filter(MalcolmAttribute.class::isInstance).map(IDeviceAttribute.class::cast).
				collect(Collectors.toList());
	}


	/**
	 * Gets the value of an attribute on the device
	 */
	@Override
	public <T> T getAttributeValue(String attributeName) throws MalcolmDeviceException {
		logger.debug("getAttributeValue() called with attribute name {}", attributeName);
		IDeviceAttribute<T> attribute = getAttribute(attributeName);
		return attribute.getValue();
	}

	public static final class EpicsMalcolmModel {
		private final IPointGenerator<?> generator;
		private final List<String> axesToMove;
		private final String fileDir;
		private final String fileTemplate;

		public EpicsMalcolmModel(String fileDir, String fileTemplate,
				List<String> axesToMove, IPointGenerator<?> generator) {
			this.fileDir = fileDir;
			this.fileTemplate = fileTemplate;
			this.axesToMove = axesToMove;
			this.generator = generator;
		}

		public String getFileDir() {
			return fileDir;
		}

		public String getFileTemplate() {
			return fileTemplate;
		}

		public List<String> getAxesToMove() {
			return axesToMove;
		}

		public IPointGenerator<?> getGenerator() {
			return generator;
		}

	}

}
