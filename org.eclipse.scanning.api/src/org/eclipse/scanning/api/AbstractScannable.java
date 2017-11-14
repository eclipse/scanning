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
package org.eclipse.scanning.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.event.IPositionListenable;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.event.Location;
import org.eclipse.scanning.api.scan.event.PositionDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Convenience class using inheritance to contain some of the general
 * things a scannable does that are the same for all scannables.
 *
 * NOTE: Inheritance is designed to have three levels only
 * IScannable->AbstractScannable->A device
 *
 * The preferred alternative if more complex behaviour is required would
 * be to create delegates for these interfaces which are then aggregated
 * in the device.
 *
 * @author Matthew Gerring
 *
 * @param <T>
 */
public abstract class AbstractScannable<T> implements IScannable<T>, IScanAttributeContainer, IPositionListenable {

	private static Logger logger = LoggerFactory.getLogger(AbstractScannable.class);

	private T                   max;
	private T                   min;
	private T                   tolerance;
	private Map<String, Object> attributes;
	private int                 level;
	private String              name;
	private boolean             activated;
	private MonitorRole         monitorRole=MonitorRole.PER_POINT;
	private long                timeout=-1;

	/**
	 * Model is used for some scannables for instance those writing NeXus
	 * in a complex way to configure the scannable such that it can write
	 * the complex information. It is not compulsory to provide a model,
	 * only those scannables requiring extra-ordinary information require
	 * one.
	 */
	private Object              model;

	/**
	 * The service used to register this device.
	 */
	private IScannableDeviceService scannableDeviceService;

	/**
	 * Implementors should use the delegate to notify of position.
	 */
	protected PositionDelegate  delegate;

	protected AbstractScannable() {
		this(null, null);
	}

	/**
	 *
	 * @param publisher used to notify of positions externally.
	 */
	protected AbstractScannable(IPublisher<Location> publisher) {
		this(publisher, null);
	}

	/**
	 *
	 * @param sservice
	 */
	protected AbstractScannable(IScannableDeviceService sservice) {
		this(null, sservice);
	}

	/**
	 *
	 * @param publisher
	 * @param sservice
	 */
	protected AbstractScannable(IPublisher<Location> publisher, IScannableDeviceService sservice) {
		this.attributes = new HashMap<>(7);
		this.delegate   = new PositionDelegate(publisher, this);
		setScannableDeviceService(sservice);
	}

	/**
	 * Used by spring to register the detector with the Runnable device service
	 * *WARNING* Before calling register the detector must be given a service to
	 * register this. This can be done from the constructor super(IRunnableDeviceService)
	 * of the detector to make it easy to instantiate a no-argument detector and
	 * register it from spring.
	 */
	public void register() {
		scannableDeviceService.register(this);
	}

	@Override
	public void addPositionListener(IPositionListener listener) {
		delegate.addPositionListener(listener);
	}

	@Override
	public void removePositionListener(IPositionListener listener) {
		delegate.removePositionListener(listener);
	}

	public void setPublisher(IPublisher<Location> publisher) {
		delegate.setPublisher(publisher);
	}

	/**
	 *
	 * @return null if no attributes, otherwise collection of the names of the attributes set
	 */
	@Override
	public Set<String> getScanAttributeNames() {
		return attributes.keySet();
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
		attributes.put(attributeName, (A)value);
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
		return (A)attributes.get(attributeName);
	}

	public DeviceInformation<T> getDeviceInformation() {
		DeviceInformation<T> deviceInfo = new DeviceInformation<>();
		deviceInfo.setName(getName());
		deviceInfo.setLevel(getLevel());
		deviceInfo.setUnit(getUnit());
		deviceInfo.setUpper(getMaximum());
		deviceInfo.setLower(getMinimum());
		deviceInfo.setPermittedValues(deviceInfo.getPermittedValues());
		deviceInfo.setActivated(isActivated());
		deviceInfo.setMonitorRole(getMonitorRole());

		return deviceInfo;
	}

	@Override
	public int getLevel() {
		return level;
	}

	@Override
	public void setLevel(int level) {
		this.level = level;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public T getMaximum() {
		return max;
	}

	public T setMaximum(T upper) {
		T ret = this.max;
		this.max = upper;
		return ret;
	}

	@Override
	public T getMinimum() {
		return min;
	}

	public T setMinimum(T lower) {
		T ret = this.min;
		this.min = lower;
		return ret;
	}

	@Override
	public boolean isActivated() {
		return activated;
	}

	@Override
	public boolean setActivated(boolean activated) {
		logger.trace("setActivated({}) was {} ({})", activated, this.activated, this);
		boolean was = this.activated;
		this.activated = activated;
		return was;
	}

	public static final <T> IScannable<T> empty() {
		return new AbstractScannable<T>() {
			@Override
			public T getPosition() throws Exception {
				return null;
			}

			@Override
			public T setPosition(T value, IPosition position) throws Exception {
				throw new Exception("Cannot set position, scannable is empty!");
			}
		};
	}

	@SuppressWarnings("unchecked")
	public <M> M getModel() {
		return (M)model;
	}

	public <M> void setModel(M model) {
		this.model = model;
	}

	public IScannableDeviceService getScannableDeviceService() {
		return scannableDeviceService;
	}

	public void setScannableDeviceService(IScannableDeviceService scannableDeviceService) {
		this.scannableDeviceService = scannableDeviceService;
	}

	@Override
	public MonitorRole getMonitorRole() {
		return monitorRole;
	}

	@Override
	public MonitorRole setMonitorRole(MonitorRole monitorRole) {
		logger.trace("setMonitorRole({}) was {} ({})", monitorRole, this.monitorRole, this);
		MonitorRole orig = this.monitorRole;
		this.monitorRole = monitorRole;
		return orig;
	}

	@Override
	public long getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public T getTolerance() {
		return tolerance;
	}

	@Override
	public T setTolerance(T tolerance) {
		T orig = this.tolerance;
		this.tolerance = tolerance;
		return orig;
	}
}
