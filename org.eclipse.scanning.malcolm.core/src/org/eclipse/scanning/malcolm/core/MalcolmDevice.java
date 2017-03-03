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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.MalcolmModel;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.attributes.MalcolmAttribute;
import org.eclipse.scanning.api.malcolm.attributes.NumberAttribute;
import org.eclipse.scanning.api.malcolm.connector.IMalcolmConnectorService;
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
import org.eclipse.scanning.points.mutators.FixedDurationMutator;
import org.eclipse.scanning.sequencer.SubscanModerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object that make the connection to the device and monitors its status.
 * 
 * Important things to do:
 * 1. The locking in AbstractMalcolmDevice should in theory not be needed, we will push this all into the connection.
 * 2. The Serializer to JSON, 'ObjectMapper mapper' must be abstacted out because real connection can be JSON or EPICSV4
 * 3. The Socket to ZeroMQ must be abstracted out because the real socket can be ZeroMQ or EPICSV4
 * 
 * @author Matthew Gerring
 *
 */
public class MalcolmDevice<M extends MalcolmModel> extends AbstractMalcolmDevice<M> {
	
	public static final class EpicsMalcolmModel {
		private final IPointGenerator<?> generator;
		private final List<String> axesToMove;
		private final String fileDir;

		public EpicsMalcolmModel(String fileDir, List<String> axesToMove,
				IPointGenerator<?> generator) {
			this.fileDir = fileDir;
			this.axesToMove = axesToMove;
			this.generator = generator;
		}

		public String getFileDir() {
			return fileDir;
		}

		public List<String> getAxesToMove() {
			return axesToMove;
		}

		public IPointGenerator<?> getGenerator() {
			return generator;
		}
		
	}

	private static Logger logger = LoggerFactory.getLogger(MalcolmDevice.class);

    private MalcolmMessage                      stateSubscriber;
    private MalcolmMessage                      scanSubscriber;

	private IPublisher<ScanBean>             publisher;
	
	private MalcolmEventBean meb;
	
	private Iterator<IPosition> scanPositionIterator;
	
	private long lastBroadcastTime = System.currentTimeMillis();
	
	private int lastUpdateCount = 0;
	
	private boolean succesfullyInitialised = false;
	
	private boolean subscribedToStateChange = false;
	
	private final long POSITION_COMPLETE_TIMEOUT = 250; // broadcast every 250 milliseconds

	private static String STATE_ENDPOINT = "state";
	
	private static String STATUS_ENDPOINT = "status";
	
	private static String BUSY_ENDPOINT = "busy";
	
	private static String CURRENT_STEP_ENDPOINT = "completedSteps";

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
	
	public void register() {
		try {
			super.register();
			initialize();
		} catch (MalcolmDeviceException e) {
			logger.error("Could not initialize malcolm device " + getName(), e);
		}
	}
	
	public void initialize() throws MalcolmDeviceException {
		try {
			setAlive(false);
	    	final DeviceState currentState = getDeviceState();
			logger.debug("Connecting to '"+getName()+"'. Current state: "+currentState);
			
			stateSubscriber = connectionDelegate.createSubscribeMessage(STATE_ENDPOINT);
			connector.subscribe(this, stateSubscriber, new IMalcolmListener<MalcolmMessage>() {
				
				@Override
				public void eventPerformed(MalcolmEvent<MalcolmMessage> e) {				
					try {
						sendScanStateChange(e);										
					} catch (Exception ne) {
						logger.error("Problem dispatching message!", ne);
					}
				}
			});		
			
			scanSubscriber  = connectionDelegate.createSubscribeMessage(CURRENT_STEP_ENDPOINT);
			connector.subscribe(this, scanSubscriber, new IMalcolmListener<MalcolmMessage>() {
				
				@Override
				public void eventPerformed(MalcolmEvent<MalcolmMessage> e) {				
					try {
						sendScanEvent(e);										
					} catch (Exception ne) {
						logger.error("Problem dispatching message!", ne);
					}
				}
			});		
			succesfullyInitialised = true;
			setAlive(true);
		
		} finally {
			if (!subscribedToStateChange) {
				subscribedToStateChange = true;
				MalcolmDevice<?> thisDevice = this;
				Thread subscriberThread = new Thread() {
					public void run() {
						try {
							connector.subscribeToConnectionStateChange(thisDevice, new IMalcolmListener<Boolean>() {
								@Override
								public void eventPerformed(MalcolmEvent<Boolean> e) {				
									handleConnectionStateChange(e.getBean());
								}
							});
							handleConnectionStateChange(true);
							setAlive(true);
						} catch (MalcolmDeviceException ex) {
							logger.error("Unable to subsribe to state change on '" + thisDevice.getName() + "'", ex);
						}
					}
				};
				subscriberThread.setDaemon(true);
				subscriberThread.start();	
			}
		}	
		
	}
	 
	/**
	 * Actions to take when the PointStart attribute is used
	 * @param moderator the SubscanModerator
	 */
    @PointStart
    public void scanPoint(SubscanModerator moderator) {
    	Iterable<IPosition> scanPositions = moderator.getInnerIterable();
        scanPositionIterator = scanPositions.iterator();
    }

	protected void sendScanEvent(MalcolmEvent<MalcolmMessage> e) throws Exception {
		
		MalcolmMessage msg      = e.getBean();
		DeviceState newState = MalcolmUtil.getState(msg, false);

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
			
			if (scanPosition != null && currentTime - lastBroadcastTime >= POSITION_COMPLETE_TIMEOUT) {
				scanPosition.setStepIndex(point);
            	firePositionComplete(scanPosition);
            	
	            lastBroadcastTime = System.currentTimeMillis();
			}
		}
		
		if (publisher!=null) publisher.broadcast(bean);
	}

	protected void sendScanStateChange(MalcolmEvent<MalcolmMessage> e) throws Exception {
		
		MalcolmMessage msg = e.getBean();
		
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
			logger.error("Error message encountered: "+msg);
			Thread.dumpStack();
		}

		eventDelegate.sendEvent(meb);
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
			    java.awt.EventQueue.invokeLater(new Runnable() {
			        public void run() {
						try {
							if (!succesfullyInitialised) {
								initialize();
							} else {
								getDeviceState();
							}
						} catch (MalcolmDeviceException ex) {
							logger.warn("Unable to initialise/getDeviceState for device '" + getName() + "' on reconnection", ex);
						}
			        }
			    });
			} else {
				logger.warn("Malcolm Device '" + getName() + "' connection state changed to not connected");
			}
		} catch (Exception ne) {
			logger.error("Problem dispatching message!", ne);
		}
	}

	@Override
	public DeviceState getDeviceState() throws MalcolmDeviceException {
		try {
			final MalcolmMessage message = connectionDelegate.createGetMessage(STATE_ENDPOINT);
			final MalcolmMessage reply   = connector.send(this, message);
			if (reply.getType()==Type.ERROR) {
				throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
			}
			return MalcolmUtil.getState(reply);
			
		} catch (MalcolmDeviceException mne) {
			throw mne;
			
		} catch (Exception ne) {
			throw new MalcolmDeviceException(this, "Cannot connect to device '" + getName() + "'", ne);
		}
	}

	@Override
	public String getDeviceStatus() throws MalcolmDeviceException {
		try {
			final MalcolmMessage message = connectionDelegate.createGetMessage(STATUS_ENDPOINT);
			final MalcolmMessage reply   = connector.send(this, message);
			if (reply.getType()==Type.ERROR) {
				throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
			}

			return MalcolmUtil.getStatus(reply);
			
		} catch (MalcolmDeviceException mne) {
			throw mne;
			
		} catch (Exception ne) {
			throw new MalcolmDeviceException(this, "Cannot connect to device '" + getName() + "'", ne);
		}
	}

	@Override
	public boolean isDeviceBusy() throws MalcolmDeviceException {
		try {
			final MalcolmMessage message = connectionDelegate.createGetMessage(BUSY_ENDPOINT);
			final MalcolmMessage reply   = connector.send(this, message);
			if (reply.getType()==Type.ERROR) {
				throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
			}

			return MalcolmUtil.getBusy(reply);
			
		} catch (MalcolmDeviceException mne) {
			throw mne;
			
		} catch (Exception ne) {
			throw new MalcolmDeviceException(this, "Cannot connect to device '" + getName() + "'", ne);
		}
	}


	@Override
	public void validate(M params) throws ValidationException {
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
			final MalcolmMessage msg   = connectionDelegate.createCallMessage("validate", epicsModel);
			reply = connector.send(this, msg);
			if (reply.getType()==Type.ERROR) {
				throw new ValidationException("Error from Malcolm Device Connection: " + reply.getMessage());
			}
			
		} catch (MalcolmDeviceException mde) {
			throw new ValidationException(mde);
		}

		return reply.getRawValue();
	}
	
	@Override
	public void configure(M model) throws MalcolmDeviceException {
		
		// Reset the device before configure in case it's in a fault state
		try {
			reset();
		} catch (Exception ex) {
			// Swallow the error as it might throw one if in a non-resetable state
		}
		
		final EpicsMalcolmModel epicsModel = createEpicsMalcolmModel(model);
		final MalcolmMessage msg   = connectionDelegate.createCallMessage("configure", epicsModel);
		MalcolmMessage reply = connector.send(this, msg);
		if (reply.getType() == Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
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

		if (pointGenerator != null) { // TODO could the point generator be null here?
			List<IMutator> mutators = Arrays.asList(new FixedDurationMutator(exposureTime));
			((CompoundModel<?>) pointGenerator.getModel()).setMutators(mutators);
		}
		
		final EpicsMalcolmModel epicsModel = new EpicsMalcolmModel(model.getFileDir(),
				model.getAxesToMove(), pointGenerator);
		return epicsModel;
	}

	@Override
	public void run(IPosition pos) throws MalcolmDeviceException {
		MalcolmMessage reply = connectionDelegate.call(Thread.currentThread().getStackTrace(), DeviceState.RUNNING);
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
		}
	}
	
	@Override
	public void seek(int stepNumber) throws MalcolmDeviceException {
		LinkedHashMap<String, Integer> seekParameters = new LinkedHashMap<>();
		seekParameters.put(CURRENT_STEP_ENDPOINT, stepNumber);
		final MalcolmMessage msg   = connectionDelegate.createCallMessage("pause", seekParameters);
		final MalcolmMessage reply = connector.send(this, msg);
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
		}
	}

	@Override
	public void abort() throws MalcolmDeviceException {
		MalcolmMessage reply = connectionDelegate.call(Thread.currentThread().getStackTrace());
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
		}
	}

	@Override
	public void disable() throws MalcolmDeviceException {
		MalcolmMessage reply = connectionDelegate.call(Thread.currentThread().getStackTrace());
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
		}
	}

	@Override
	public void reset() throws MalcolmDeviceException {
		MalcolmMessage reply = connectionDelegate.call(Thread.currentThread().getStackTrace());
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
		}
	}

	@Override
	public void pause() throws MalcolmDeviceException {
		MalcolmMessage reply = connectionDelegate.call(Thread.currentThread().getStackTrace());
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
		}
	}
	
	@Override
	public void resume() throws MalcolmDeviceException {
		MalcolmMessage reply = connectionDelegate.call(Thread.currentThread().getStackTrace());
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
		}
	}

	@Override
	public void dispose() throws MalcolmDeviceException {
		unsubscribe(stateSubscriber);
		unsubscribe(scanSubscriber);

		setAlive(false);
	}

	private final void unsubscribe(MalcolmMessage subscriber) throws MalcolmDeviceException {
		if (subscriber!=null) {
			final MalcolmMessage unsubscribeStatus = connectionDelegate.createUnsubscribeMessage();
			unsubscribeStatus.setId(subscriber.getId());
			connector.unsubscribe(this, subscriber);
			logger.debug("Unsubscription "+getName()+" made "+unsubscribeStatus);
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
			IMalcolmListener<MalcolmMessage> stateChanger = new IMalcolmListener<MalcolmMessage>() {
				@Override
				public void eventPerformed(MalcolmEvent<MalcolmMessage> e) {
					MalcolmMessage msg = e.getBean();
					try {
						DeviceState state = MalcolmUtil.getState(msg);
						if (state != null) {
							if (ignoredStates!=null && Arrays.asList(ignoredStates).contains(state)) {
								return; // Found state that we don't want!
							}
						}
						stateContainer.add(state);
						latch.countDown();
						
					} catch (Exception ne) {
						exceptionContainer.add(ne);
						latch.countDown();
					}
				}
			};
				
			connector.subscribe(this, stateSubscriber, stateChanger);
			
			boolean countedDown = false;
			if (time>0) {
				countedDown = latch.await(time, unit);
			} else {
				latch.await();
			}
	
			connector.unsubscribe(this, stateSubscriber, stateChanger);
			
			if (exceptionContainer.size()>0) throw exceptionContainer.get(0);
			
			if (stateContainer.size() > 0) return stateContainer.get(0);
			
			if (countedDown) {
			    throw new Exception("The countdown of "+time+" "+unit+" timed out waiting for state change for device "+getName());
			} else {
				throw new Exception("A problem occured trying to latch state change for device "+getName());
			}
			
		} catch (MalcolmDeviceException ne) {
			throw ne;
			
		} catch (Exception neOther) {
			throw new MalcolmDeviceException(this, neOther);
		}

	}
	
	public Object getAttribute(String attribute) throws MalcolmDeviceException {
		final MalcolmMessage message = connectionDelegate.createGetMessage(attribute);
		final MalcolmMessage reply   = connector.send(this, message);
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
		}
		return reply.getValue();
	}
	
	public List<MalcolmAttribute> getAllAttributes() throws MalcolmDeviceException {
		List<MalcolmAttribute> attributeList = new LinkedList<MalcolmAttribute>();
		String endpoint = "";
		final MalcolmMessage message = connectionDelegate.createGetMessage(endpoint);
		final MalcolmMessage reply   = connector.send(this, message);
		if (reply.getType()==Type.ERROR) {
			throw new MalcolmDeviceException("Error from Malcolm Device Connection: " + reply.getMessage());
		}
		
		Object wholeBlock = reply.getValue();
		
		if (wholeBlock instanceof Map) {
			Map wholeBlockMap = (Map)wholeBlock;
			
			for (Object entry : wholeBlockMap.values()) {
				if (entry instanceof MalcolmAttribute) {
					attributeList.add((MalcolmAttribute)entry);
				}
			}
		}
		
		return attributeList;
	}
	
	/**
	 * Gets the value of an attribute on the device
	 */
	public Object getAttributeValue(String attributeName) throws MalcolmDeviceException {
		Object attribute = getAttribute(attributeName);
		if (attribute instanceof MalcolmAttribute) {
			MalcolmAttribute malcolmAttribute = (MalcolmAttribute)attribute;
			return malcolmAttribute.getValue();
		}
		return attribute;
	}
}
