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
package org.eclipse.scanning.example.malcolm;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.Random;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.annotation.scan.PostConfigure;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.models.ScanMode;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;
import org.eclipse.scanning.example.Services;
import org.eclipse.scanning.sequencer.SubscanModerator;

/**
 * A detector that can run alongside a malcolm device and writes data when the malcolm
 * device performs a point of the inner scan. This simulates a real device that received hardware
 * triggers when running alongside a malcolm device.
 * 
 * @author Matthew Dickie
 */
public class DummyMalcolmTriggeredDetector<T extends DummyMalcolmTriggeredModel> extends AbstractRunnableDevice<T> implements INexusDevice<NXdetector>, IPositionListener {
	
	private DummyMalcolmDevice dummyMalcolmDevice;

	private IPosition outerPoint = null;

	private Iterable<IPosition> innerPointsIterable;

	private Iterator<IPosition> innerPointsIterator;
	
	private IDataset[] currentLineData = null;
	private double[] currentLineIndex = null;
	
	private int scanRank = -1;
	private int lineSize = -1;
	
	private int pointsInLineCount = 0;
	
	private double currentVal = 0.0; // Used as the current value to write
	
	private ILazyWriteableDataset data = null;
	
	private ILazyWriteableDataset count = null;
	
	@SuppressWarnings("unchecked")
	public DummyMalcolmTriggeredDetector() throws ScanningException {
		super(Services.getRunnableDeviceService());
		this.model = (T) new DummyMalcolmTriggeredModel();
		setDeviceState(DeviceState.IDLE);
		setSupportedScanMode(ScanMode.HARDWARE);
	}
	
	@Override
	public void run(IPosition position) throws ScanningException, InterruptedException {
		setDeviceState(DeviceState.RUNNING);
	}
	
	@Override
	public void validate(T model) throws ValidationException {
		if (model instanceof INameable) {
			INameable dmodel = (INameable)model;
		    if (dmodel.getName()==null || dmodel.getName().length()<1) {
		    	throw new ModelValidationException("The name must be set!", model, "name");
		    }
		    // don't validate exposure time. The malcolm device determines the exposure time
		}
	}

	@ScanStart
	public void scanStart(ScanBean scanBean, SubscanModerator subscanModerator) throws ScanningException {
		this.innerPointsIterable = subscanModerator.getInnerIterable();
		
		// We need to get the malcolm device so that we can listen to it as a position listener
		// this simulates receving hardware triggers, so a real hardware triggered detector doesn't
		// need to do this.
		Map<String, Object> detectorMap = scanBean.getScanRequest().getDetectors();
		for (Entry<String, Object> detectorEntry : detectorMap.entrySet()) {
			if (detectorEntry.getValue() instanceof DummyMalcolmModel) {
				IRunnableDevice<?> runnableDevice = getRunnableDeviceService().getRunnableDevice(
						detectorEntry.getKey());
				dummyMalcolmDevice = (DummyMalcolmDevice) runnableDevice;
				dummyMalcolmDevice.addPositionListener(this);
			}
		}
	}
	
	@ScanFinally
	public void scanFinally() {
		if (dummyMalcolmDevice != null) {
			dummyMalcolmDevice.removePositionListener(this);
		}
		dummyMalcolmDevice = null;
		
		scanRank = -1;
		lineSize = -1;
		currentLineData = null;
		currentLineIndex = null;
		innerPointsIterable = null;
		innerPointsIterator = null;
		outerPoint = null;
		data = null;
	}
	
	@PostConfigure
	public void postConfigure(ScanInformation scanInformation) {
		int[] shape = scanInformation.getShape();
		this.scanRank = shape.length;
		this.lineSize = shape[scanRank - 1];
	}
	
	@PointStart
	public void pointStart(IPosition outerPoint) {
		this.outerPoint = outerPoint;
		pointsInLineCount = 0;
		innerPointsIterator = innerPointsIterable.iterator();
		currentLineData = new IDataset[lineSize];
		currentLineIndex = new double[lineSize]; 
	}

	@Override
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info)
			throws NexusException {
		NXdetector detector = NexusNodeFactory.createNXdetector(); 
		data = detector.initializeLazyDataset(NXdetector.NX_DATA, scanRank + 2, Double.class);
		count = detector.initializeLazyDataset("sum", scanRank, Double.class);
		
		NexusObjectWrapper<NXdetector> nexusProvider = new NexusObjectWrapper<>(getName(), detector);
		nexusProvider.setPrimaryDataFieldName(NXdetector.NX_DATA);
		nexusProvider.addAdditionalPrimaryDataFieldName("sum");
		
		return nexusProvider;
	}
	
	@Override
	public void positionPerformed(PositionEvent evt) throws ScanningException {
		hardwareTriggerReceived();
	}

	public void hardwareTriggerReceived() {
		currentLineData[pointsInLineCount] = Random.rand(64, 64); // a 64x64 image at each point in the scan
		currentLineIndex[pointsInLineCount] = (currentVal++); // use the index as the actual value
		pointsInLineCount++;
		
		if (pointsInLineCount == lineSize) {
			pointsInLineCount = 0;
			try {
				writeLine();
			} catch (DatasetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void writeLine() throws DatasetException {
		// This could be done in a separate thread, which could be given references to the
		// arrays currently pointed to by these member fields, while the actual member fields
		// are set to point to new empty arrays
		for (int i = 0; i < lineSize; i++) {
			IPosition innerPosition = innerPointsIterator.next();
			IPosition overallPosition = innerPosition.compound(outerPoint);
			
			writeData(data, overallPosition, DatasetFactory.createFromObject(currentLineData[i]));
			writeData(count, overallPosition, DatasetFactory.createFromObject(currentLineIndex[i]));
		}
	}
	
	private void writeData(ILazyWriteableDataset dataset, IPosition position, IDataset dataToWrite) throws DatasetException {
		IScanSlice slice = IScanRankService.getScanRankService().createScanSlice(position, dataToWrite.getShape());
		SliceND sliceND = new SliceND(dataset.getShape(), dataset.getMaxShape(),
				slice.getStart(), slice.getStop(), slice.getStep());
		dataset.setSlice(null, dataToWrite, sliceND);
	}

}
