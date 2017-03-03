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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.dawnsci.nexus.IMultipleNexusDevice;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.IMalcolmModel;
import org.eclipse.scanning.api.device.models.ScanMode;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.connector.IMalcolmConnectorService;
import org.eclipse.scanning.api.malcolm.connector.MessageGenerator;
import org.eclipse.scanning.api.malcolm.event.IMalcolmListener;
import org.eclipse.scanning.api.malcolm.event.MalcolmEventBean;
import org.eclipse.scanning.api.malcolm.message.MalcolmMessage;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Base class for Malcolm devices
 * 
 * @param <M> the model class for this malcolm device
 */
public abstract class AbstractMalcolmDevice<M extends IMalcolmModel> extends AbstractRunnableDevice<M>
		implements IMalcolmDevice<M>, IMultipleNexusDevice {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractMalcolmDevice.class);
	
	public static final String ATTRIBUTE_NAME_DATASETS = "datasets";
	public static final String ATTRIBUTE_NAME_AXES_TO_MOVE = "axesToMove";

	public static final String DATASETS_TABLE_COLUMN_NAME = "name";
	public static final String DATASETS_TABLE_COLUMN_FILENAME = "filename";
	public static final String DATASETS_TABLE_COLUMN_PATH = "path";
	public static final String DATASETS_TABLE_COLUMN_TYPE = "type";
	public static final String DATASETS_TABLE_COLUMN_RANK = "rank";
	public static final String DATASETS_TABLE_COLUMN_UNIQUEID = "uniqueid";
	
	// Events
	protected MalcolmEventDelegate eventDelegate;
	
	// Connection to serialization to talk to the remote object
	protected MessageGenerator<MalcolmMessage> connectionDelegate;
	
	protected IMalcolmConnectorService<MalcolmMessage> connector;
	
	protected IPointGenerator<?> pointGenerator;
	
	public AbstractMalcolmDevice(IMalcolmConnectorService<MalcolmMessage> connector,
			IRunnableDeviceService runnableDeviceService) throws MalcolmDeviceException {
		super(runnableDeviceService);
		this.connector = connector;
   		this.connectionDelegate = connector.createDeviceConnection(this);
   		this.eventDelegate = new MalcolmEventDelegate(getName(), connector);
   		setRole(DeviceRole.MALCOLM);
   		setSupportedScanMode(ScanMode.HARDWARE);
	}
		
	@PreConfigure
	public void setPointGenerator(IPointGenerator<?> pointGenerator) {
		this.pointGenerator = pointGenerator;
	}
	public IPointGenerator<?> getPointGenerator() {
		return pointGenerator;
	}
	
	/**
	 * Enacts any pre-actions or conditions before the device attempts to run the task block.
	 *  
	 * @throws Exception
	 */
	protected void beforeExecute() throws Exception {
        logger.debug("Entering beforeExecute, state is " + getDeviceState());	
	}

	/**
	 * Enacts any post-actions or conditions after the device completes a run of the task block.
	 *  
	 * @throws Exception
	 */
	protected void afterExecute() throws Exception {
        logger.debug("Entering afterExecute, state is " + getDeviceState());	
	}
	
	protected void setTemplateBean(MalcolmEventBean bean) {
		eventDelegate.setTemplateBean(bean);
	}

	public void start(final IPosition pos) throws ScanningException, InterruptedException {
		
		final List<Throwable> exceptions = new ArrayList<>(1);
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					AbstractMalcolmDevice.this.run(pos);
				} catch (ScanningException|InterruptedException e) {
					e.printStackTrace();
					exceptions.add(e);
				}
			}
		}, "Device Runner Thread "+getName());
		thread.start();
		
		// We delay by 500ms just so that we can 
		// immediately throw any connection exceptions
		Thread.sleep(500);
		
		if (!exceptions.isEmpty()) throw new ScanningException(exceptions.get(0));
	}

	protected void close() throws Exception {
		eventDelegate.close();
	}
	
	@Override
	public void dispose() throws MalcolmDeviceException {
		try {
			try {
			    if (getDeviceState().isRunning()) abort();
			} finally {
			   close();
			}
		} catch (Exception e) {
			throw new MalcolmDeviceException(this, "Cannot dispose of '"+getName()+"'!", e);
		}
	}
	
	@Override
	public List<NexusObjectProvider<?>> getNexusProviders(NexusScanInfo info) throws NexusException {
		try {
			MalcolmNexusObjectBuilder<M> malcolmNexusBuilder = new MalcolmNexusObjectBuilder<>(this);
			return malcolmNexusBuilder.buildNexusObjects(info);
		} catch (Exception e) {
			throw new NexusException("Could not create nexus objects for malcolm device " + getName(), e);
		}
	}

	@Override
	public void addMalcolmListener(IMalcolmListener l) {
		eventDelegate.addMalcolmListener(l);
	}

	@Override
	public void removeMalcolmListener(IMalcolmListener l) {
		eventDelegate.removeMalcolmListener(l);
	}

	protected void sendEvent(MalcolmEventBean event) throws Exception {
		eventDelegate.sendEvent(event);
	}

	@Override
	public Set<String> getAxesToMove() throws ScanningException {
		String[] axesToMove = (String[]) getAttributeValue(ATTRIBUTE_NAME_AXES_TO_MOVE);
		return new HashSet<>(Arrays.asList(axesToMove));
	}

}
