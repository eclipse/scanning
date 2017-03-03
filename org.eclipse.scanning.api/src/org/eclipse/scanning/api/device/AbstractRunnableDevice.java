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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.scanning.api.IModelProvider;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.IScanAttributeContainer;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.device.models.ScanMode;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListenable;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;

/**
 * A device should create its own model when its constructor is called. This
 * can be done by reading the current hardware state for the device. In this
 * case the runnable device service does not set its model. If a given device
 * does not set its own model, when the service makes the device, it will attempt
 * to create a new empty model and set this empty model as the current model. 
 * This means that the device does not have a null model and the user can get
 * the model and configure it.
 * 
 * @see IRunnableDevice
 * @author Matthew Gerring
 *
 * @param <T>
 */
public abstract class AbstractRunnableDevice<T> implements IRunnableEventDevice<T>, 
                                                           IModelProvider<T>, 
                                                           IScanAttributeContainer, 
                                                           IPositionListenable,
                                                           IActivatable{

	// Data
	protected T                          model;
	private   String                     name;
	private   int                        level = 1;
	private   String                     scanId;
	private   ScanBean                   bean;
	private   DeviceInformation<T>       deviceInformation;
	private   DeviceRole                 role = DeviceRole.HARDWARE;
	private   Set<ScanMode>              supportedScanModes = EnumSet.of(ScanMode.SOFTWARE);

	// Devices can either be the top of the scan or somewhere in the
	// scan tree. By default they are the scan but if used in a nested
	// scan, their primaryScanDevice will be set to false. This then 
	// stops state being written to the main scan bean
	private   boolean                    primaryScanDevice = true;

	// OSGi services and intraprocess events
	protected IRunnableDeviceService     runnableDeviceService;
	protected IScannableDeviceService    connectorService;
	private   IPublisher<ScanBean>       publisher;
	
	// Listeners
	private   Collection<IRunListener>   rlisteners;
	private   Collection<IPositionListener> posListeners;
	
	// Attributes
	private Map<String, Object>          scanAttributes;
	
	private volatile boolean busy = false;
	private boolean requireMetrics;
	
	/**
	 * Alive here is taken to represent the device being on and responding.
	 */
	private boolean alive = true;
	
	/**
	 * Since making the tree takes a while we measure its
	 * time and make that available to clients.
	 * It is optional if a given AbstractRunnableDevice
	 * saves the configure time.
	 */
	private long configureTime;

    /**
     * Do not make this constructor public. In order for the device
     * to be used with spring, the device service must be provided when
     * the object is constructed currently. If making this constructor
     * public please also fix the spring configuration which requires
     * the 'register' method to be called and the service to be
     * non-null.
     */
	private AbstractRunnableDevice() {
		this.scanId     = UUID.randomUUID().toString();
		this.scanAttributes = new HashMap<>();
		setRequireMetrics(Boolean.getBoolean(getClass().getName()+".Metrics"));
	}

	/**
	 * Devices may be created during the cycle of a runnable device service being
	 * made. Therefore the parameter dservice may be null. This is acceptable 
	 * because when used in spring the service is going and then the register(...)
	 * method may be used.
	 * 
	 * @param dservice
	 */
	protected AbstractRunnableDevice(IRunnableDeviceService dservice) {
		this();
		setRunnableDeviceService(dservice);
	}
	
	/**
	 * Used by spring to register the detector with the Runnable device service
	 * *WARNING* Before calling register the detector must be given a service to 
	 * register this. This can be done from the constructor super(IRunnableDeviceService)
	 * of the detector to make it easy to instantiate a no-argument detector and
	 * register it from spring.
	 */
	public void register() {
		if (runnableDeviceService==null) throw new RuntimeException("Unable to register "+getClass().getSimpleName()+" because the runnable device service was not injected correctly.");
		runnableDeviceService.register(this);
	}

	public ScanBean getBean() {
		if (bean==null) bean = new ScanBean();
		return bean;
	}
	
	public void setBean(ScanBean bean) throws ScanningException {
		this.bean = bean;
		try {
			bean.setHostName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			throw new ScanningException("Unable to read name of host!");
		}
	}

	public IRunnableDeviceService getRunnableDeviceService() {
		return runnableDeviceService;
	}

	public void setRunnableDeviceService(IRunnableDeviceService runnableDeviceService) {
		this.runnableDeviceService = runnableDeviceService;
	}

	public IScannableDeviceService getConnectorService() {
		return connectorService;
	}

	public void setConnectorService(IScannableDeviceService connectorService) {
		this.connectorService = connectorService;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void reset() throws ScanningException {
		setDeviceState(DeviceState.IDLE);
	}


	/**
	 * 
	 * @param nstate
	 * @param position
	 * @throws ScanningException 
	 */
	protected void setDeviceState(DeviceState nstate) throws ScanningException {
		
		if (!isPrimaryScanDevice()) return; // Overrall scan state is not managed by us.
		try {
			// The bean must be set in order to change state.
			if (bean==null) {
				bean = new ScanBean();
			}
			bean.setDeviceName(getName());
			bean.setPreviousDeviceState(bean.getDeviceState());
			bean.setDeviceState(nstate);
			
			fireStateChanged(bean.getPreviousDeviceState(), nstate);

			if (publisher!=null) {
				publisher.broadcast(bean);
			}

		} catch (Exception ne) {
			ne.printStackTrace();
			if (ne instanceof ScanningException) throw (ScanningException)ne;
			throw new ScanningException(this, ne);
		}
		
	}
	
	public DeviceState getDeviceState() throws ScanningException {
		if (bean==null) return null;
		return bean.getDeviceState();
	}

	
	private long lastPositionTime = -1;
	private long total=0;
	/**
	 * 
	 * @param pos
	 * @param count 0-based position count (1 is added to calculate % complete)
	 * @param size
	 * @throws EventException
	 * @throws ScanningException
	 */
	protected void positionComplete(IPosition pos, int count, int size) throws EventException, ScanningException {
		
		if (requireMetrics) {
			long currentTime = System.currentTimeMillis();
			if (lastPositionTime>-1) {
				long time = currentTime-lastPositionTime;
				System.out.println("Point "+count+" timed at "+time+" ms");
				total+=time;
			}
			lastPositionTime = currentTime;
		}
		firePositionComplete(pos);
		
		final ScanBean bean = getBean();
		bean.setPoint(count);
		bean.setPosition(pos);
		bean.setPreviousDeviceState(bean.getDeviceState());
		if (size>-1) bean.setPercentComplete(((double)(count+1)/size)*100);
		if (bean.getDeviceState()==DeviceState.RUNNING) { // Only set this message if we are still running.
			bean.setMessage("Point " + (pos.getStepIndex() + 1) +" of " + size);
		}
		if (publisher != null) {
			publisher.broadcast(bean);
		}
	}

	public String getScanId() {
		return scanId;
	}

	public void setScanId(String scanId) {
		this.scanId = scanId;
	}
	public IPublisher<ScanBean> getPublisher() {
		return publisher;
	}
	public void setPublisher(IPublisher<ScanBean> publisher) {
		this.publisher = publisher;
	}

	@Override
	public void addRunListener(IRunListener l) {
		if (rlisteners==null) rlisteners = Collections.synchronizedCollection(new LinkedHashSet<>());
		rlisteners.add(l);
	}
	
	@Override
	public void removeRunListener(IRunListener l) {
		if (rlisteners==null) return;
		rlisteners.remove(l);
	}
	
	@Override
	public void addPositionListener(IPositionListener l) {
		if (posListeners == null) {
			posListeners = Collections.synchronizedCollection(new LinkedHashSet<>());
		}
		posListeners.add(l);
	}
	
	@Override
	public void removePositionListener(IPositionListener l) {
		if (posListeners == null) return;
		posListeners.remove(l);
	}

	public void firePositionComplete(IPosition position) throws ScanningException {
		if (posListeners == null) return;
		
		final PositionEvent evt = new PositionEvent(position, this);
		
		// Make array, avoid multi-threading issues
		final IPositionListener[] la = posListeners.toArray(new IPositionListener[posListeners.size()]);
		for (IPositionListener l : la) l.positionPerformed(evt);
	}
	
	public void fireStateChanged(DeviceState oldState, DeviceState newState) throws ScanningException {
		
		if (rlisteners==null) return;
		
		final RunEvent evt = new RunEvent(this, null, newState);
		evt.setOldState(oldState);
		
		// Make array, avoid multi-threading issues.
		final IRunListener[] la = rlisteners.toArray(new IRunListener[rlisteners.size()]);
		for (IRunListener l : la) l.stateChanged(evt);
	}


	private long startTime;
	
	public void fireRunWillPerform(IPosition position) throws ScanningException {
		
		if (isRequireMetrics()) {
			startTime = System.currentTimeMillis();
			total     = 0;
		}

		if (rlisteners==null) return;
		
		final RunEvent evt = new RunEvent(this, position, getDeviceState());
		
		// Make array, avoid multi-threading issues.
		final IRunListener[] la = rlisteners.toArray(new IRunListener[rlisteners.size()]);
		for (IRunListener l : la) l.runWillPerform(evt);
	}
	
	public void fireRunPerformed(IPosition position) throws ScanningException {
		
		if (isRequireMetrics()) {
			long time = System.currentTimeMillis()-startTime;
			System.out.println("Ran "+(position.getStepIndex()+1)+" points in *total* time of "+time+" ms.");
			if (position.getStepIndex()>0) {
				System.out.println("Average point time of "+(total/position.getStepIndex())+" ms/pnt");
			}
		}
		
		if (rlisteners==null) return;
		
		final RunEvent evt = new RunEvent(this, position, getDeviceState());
		
		// Make array, avoid multi-threading issues.
		final IRunListener[] la = rlisteners.toArray(new IRunListener[rlisteners.size()]);
		for (IRunListener l : la) l.runPerformed(evt);
	}
	
	public void fireWriteWillPerform(IPosition position) throws ScanningException {
		
		if (rlisteners==null) return;
		
		final RunEvent evt = new RunEvent(this, position, getDeviceState());
		
		// Make array, avoid multi-threading issues.
		final IRunListener[] la = rlisteners.toArray(new IRunListener[rlisteners.size()]);
		for (IRunListener l : la) l.writeWillPerform(evt);
	}
	
	public void fireWritePerformed(IPosition position) throws ScanningException {
		
		if (rlisteners==null) return;
		
		final RunEvent evt = new RunEvent(this, position, getDeviceState());
		
		// Make array, avoid multi-threading issues.
		final IRunListener[] la = rlisteners.toArray(new IRunListener[rlisteners.size()]);
		for (IRunListener l : la) l.writePerformed(evt);
	}


	public T getModel() {
		return model;
	}

	public void setModel(T model) {
		this.model = model;
	}
	
	@Override
	public void configure(T model) throws ScanningException {
		this.model = model;
		setDeviceState(DeviceState.READY);
	}


	@Override
	public void abort() throws ScanningException {

	}

	@Override
	public void disable() throws ScanningException {

	}

	@Override
	public void pause() throws ScanningException {

	}
	
	@Override
	public void seek(int stepNumber) throws ScanningException, InterruptedException {
       // Do nothing
	}


	@Override
	public void resume() throws ScanningException {

	}

	/**
	 * 
	 * @return null if no attributes, otherwise collection of the names of the attributes set
	 */
	@Override
	public Set<String> getScanAttributeNames() {
		return scanAttributes.keySet();
	}

	/**
	 * Set any attribute the implementing classes may provide
	 * 
	 * @param attributeName
	 *            is the name of the attribute
	 * @param value
	 *            is the value of the attribute
	 * @throws DeviceException
	 *             if an attribute cannot be set
	 */
	@Override
	public <A> void setScanAttribute(String attributeName, A value) throws Exception {
		scanAttributes.put(attributeName, (A)value);
	}

	/**
	 * Get the value of the specified attribute
	 * 
	 * @param attributeName
	 *            is the name of the attribute
	 * @return the value of the attribute
	 * @throws DeviceException
	 *             if an attribute cannot be retrieved
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <A> A getScanAttribute(String attributeName) throws Exception {
		return (A)scanAttributes.get(attributeName);
	}

	/**
	 * Do not override without calling super.getDeviceInformation()
	 * Method is final for now to help avoid that problem.
	 * @return
	 * @throws ScanningException
	 */
	public final DeviceInformation<T> getDeviceInformation() throws ScanningException {
 		return getDeviceInformationIncludeNonAlive(false);
	}

	/**
	 * Do not override without calling super.getDeviceInformation()
	 * Method is final for now to help avoid that problem.
	 * Gets the device information, with the ability to specify whether to get information that is potentially held
	 * on a device or not in the case that the device is not marked as being alive.
	 * 
	 * @param includeNonAlive If set to false, if a device is not alive, information potentially held on the device will not be retrieved
	 * @return
	 * @throws ScanningException
	 */
	public final DeviceInformation<T> getDeviceInformationIncludeNonAlive(boolean includeNonAlive) throws ScanningException {
		if (deviceInformation==null) {
			deviceInformation = new DeviceInformation<T>();
		}
		deviceInformation.setModel(getModel());
		deviceInformation.setDeviceRole(getRole());
		deviceInformation.setSupportedScanModes(getSupportedScanModes());
		if (getName()!=null) deviceInformation.setName(getName());
		deviceInformation.setLevel(getLevel());
		deviceInformation.setActivated(isActivated());
		deviceInformation.setAlive(isAlive());
		
		// Information below may come from an actual device. Check if device is alive before attempting to get this 
		if (includeNonAlive || deviceInformation.isAlive()) {
			try {
				deviceInformation.setState(getDeviceState());
				deviceInformation.setStatus(getDeviceStatus());
				deviceInformation.setBusy(isDeviceBusy());
				deviceInformation.setAlive(isAlive());
			} catch (Exception ex) {
				ex.printStackTrace();
				deviceInformation.setAlive(false);
			}
		}

		// TODO TEMPFIX for DAQ-419 before GUI updated. Just need some way of showing user if a device is offline.
		if (deviceInformation.getLabel() != null) {
			deviceInformation.setLabel(deviceInformation.getLabel().replace(" [*]","")); // Get rid of any existing non-alive flag
		}
		if (!deviceInformation.isAlive()) {
			if (deviceInformation.getLabel() != null) {
				deviceInformation.setLabel(deviceInformation.getLabel() + " [*]"); 
			}
			deviceInformation.setState(DeviceState.OFFLINE);
		}
		
 		return deviceInformation;
	}

	public void setDeviceInformation(DeviceInformation<T> deviceInformation) {
		this.deviceInformation = deviceInformation;
	}

	public boolean isPrimaryScanDevice() {
		return primaryScanDevice;
	}

	public void setPrimaryScanDevice(boolean primaryScanDevice) {
		this.primaryScanDevice = primaryScanDevice;
	}
	
	/**
	 * If overriding don't forget the old super.validate(...)
	 */
	@Override
	public void validate(T model) throws ValidationException {
		if (model instanceof INameable) {
			INameable dmodel = (INameable)model;
		    if (dmodel.getName()==null || dmodel.getName().length()<1) {
		    	throw new ModelValidationException("The name must be set!", model, "name");
		    }
		}
		if (model instanceof IDetectorModel) {
			IDetectorModel dmodel = (IDetectorModel)model;
			if (dmodel.getExposureTime()<=0) throw new ModelValidationException("The exposure time for '"+getName()+"' must be non-zero!", model, "exposureTime");
		}
	}
	
	private boolean activated = false;

	@Override
	public boolean isActivated() {
		return activated;
	}
	
	@Override
	public boolean setActivated(boolean activated) {
		boolean wasactivated = this.activated;
		this.activated = activated;
		return wasactivated;
	}
	
	/**
	 * Please override to provide a device status (which a malcolm device will have)
	 * The default returns null.
	 * @return the current device Status.
	 */
	public String getDeviceStatus() throws ScanningException {
		return null;
	}
	
	/**
	 * Gets whether the device is busy or not
	 * @return the current value of the device 'busy' flag.
	 */
	public boolean isDeviceBusy() throws ScanningException {
		return busy;
	}

	/**
	 * Call to set the busy state while the device is running.
	 * This should not be part of IRunnableDevice, it is derived
	 * by the device when it is running or set by the scanning when
	 * it is scanning on CPU devices. This means that the creator of
	 * a Detector does not have to worry about setting it busy during
	 * scans.
	 * 
	 * @param busy
	 */
	public void setBusy(boolean busy) {
		this.busy = busy;
	}
	
	public DeviceRole getRole() {
		return role;
	}

	public void setRole(DeviceRole role) {
		this.role = role;
	}
	
	@Override
	public Set<ScanMode> getSupportedScanModes() {
		return supportedScanModes;
	}
	
	public void setSupportedScanModes(Set<ScanMode> supportedScanModes) {
		this.supportedScanModes = supportedScanModes;
	}
	

	protected void setSupportedScanModes(ScanMode... supportedScanModes) {
		if (supportedScanModes==null) {
			supportedScanModes = null;
			return;
		}
		this.supportedScanModes = EnumSet.of(supportedScanModes[0], supportedScanModes);
	}
	
	public void setSupportedScanMode(ScanMode supportedScanMode) {
		this.supportedScanModes = EnumSet.of(supportedScanMode);
	}

	public boolean isRequireMetrics() {
		return requireMetrics;
	}

	public void setRequireMetrics(boolean requireMetrics) {
		this.requireMetrics = requireMetrics;
	}
	public long getConfigureTime() {
		return configureTime;
	}

	public void setConfigureTime(long configureTime) {
		this.configureTime = configureTime;
	}

	@Override
	public String toString() {
		return getClass().getName() + '@' + Integer.toHexString(hashCode()) +" [name=" + name + "]";
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	public void setAlive(boolean alive) {
		this.alive = alive;
	}
}
