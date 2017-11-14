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
package org.eclipse.scanning.example.detector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.Random;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;
import org.eclipse.scanning.example.Services;

/**
 * This device mimicks telling EPICS to do a scan down a line.
 *
 * It writes an HDF5 file for the line (actually random data)
 *
 * @author Matthew Gerring
 *
 */
public class RandomLineDevice extends AbstractRunnableDevice<RandomLineModel> implements IWritableDetector<RandomLineModel>, INexusDevice<NXdetector> {

	private Map<String, Integer>      counts;
	private Map<String, List<Object>> values;

	private ILazyWriteableDataset context;
	private IDataset              data;
	private boolean throwWriteExceptions=false;

	public RandomLineDevice() throws ScanningException {
		super(Services.getRunnableDeviceService()); // So that spring will work.
		this.model = new RandomLineModel();
		setDeviceState(DeviceState.READY);
		counts = new HashMap<>();
		values = new HashMap<>();
	}
	@ScanFinally
	public void clean() {
		context = null;
		data  = null;
	}

	@Override
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info) throws NexusException {
		NXdetector detector = createNexusObject(info);
		return new NexusObjectWrapper<NXdetector>(getName(), detector, NXdetector.NX_DATA);
	}

	public NXdetector createNexusObject(NexusScanInfo info) throws NexusException {
		final NXdetector detector = NexusNodeFactory.createNXdetector();
		// We add 2 to the scan rank to include the image
		int rank = info.getRank()+1; // scan rank plus three dimensions for the line scan.

		context = detector.initializeLazyDataset(NXdetector.NX_DATA, rank, Double.class);

		// Setting chunking is a very good idea if speed is required.
		int[] chunk = info.createChunk(model.getLineSize());
		context.setChunking(chunk);

		Attributes.registerAttributes(detector, this);

		return detector;
	}

	@Override
	public void configure(RandomLineModel model) throws ScanningException {
		count(Thread.currentThread().getStackTrace(), model);
		super.configure(model);
		setName(model.getName());
	}

	@Override
	public void run(IPosition pos) throws ScanningException, InterruptedException {
		count(Thread.currentThread().getStackTrace());
		// TODO Real device would tell EPICS to run the line scan now.
		// To simulate this, we create a line using the definition in the model
		// EPICS might write an HDF5 file with this data rather than the data
		// being in memory.
		data = Random.rand(new int[]{model.getLineSize()});
	}

	@Override
	public boolean write(IPosition pos) throws ScanningException, InterruptedException {

		count(Thread.currentThread().getStackTrace());
		if (model.getExposureTime()>0) {
			Thread.sleep(Math.round(model.getExposureTime()*1000));
		}
		if (isThrowWriteExceptions()) throw new ScanningException("The detector has been instructed to fail on a write!");
		try {
			// In a real CV Scan the write step could be to either link in the HDF5 or read in its data
			// and write a new record. Avoiding reading in the HDF5 being preferable.
			final IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(pos, model.getLineSize());
			SliceND sliceND = new SliceND(context.getShape(), context.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
			context.setSlice(null, data, sliceND);

		} catch (Exception e) {
			throw new ScanningException(e.getMessage(), e);
		}

		return true;
	}

	protected void count(StackTraceElement[] ste) throws ScanningException {
		count(ste, null);
	}
	protected void count(StackTraceElement[] ste, Object value) throws ScanningException {
		String methodName = getMethodName(ste);
		Integer count = counts.get(methodName);
		if (count==null) count = 0;
		count = count+1;
		counts.put(methodName, count);
		if (!values.containsKey(methodName)) values.put(methodName, new ArrayList<>());
		try {
			values.get(methodName).add(value!=null?BeanUtils.cloneBean(value):null);
		} catch (Exception e) {
			throw new ScanningException("Cannot clone information during test", e);
		}
	}

	public int getCount(String method) {
		if (!counts.containsKey(method)) return 0;
		return counts.get(method);
	}

	public Object getValue(String method, int index) {
		return values.get(method).get(index);
	}

	protected static final String getMethodName ( StackTraceElement ste[] ) {

	    String methodName = "";
	    boolean flag = false;

	    for ( StackTraceElement s : ste ) {

	        if ( flag ) {

	            methodName = s.getMethodName();
	            break;
	        }
	        flag = s.getMethodName().equals( "getStackTrace" );
	    }
	    return methodName;
	}

	@Override
	public void reset() throws ScanningException {
		resetCount();
		super.reset();
	}

	public void resetCount() {
		counts.clear();
		values.clear();
	}
	public boolean isThrowWriteExceptions() {
		return throwWriteExceptions;
	}
	public void setThrowWriteExceptions(boolean throwWriteExceptions) {
		this.throwWriteExceptions = throwWriteExceptions;
	}

}
