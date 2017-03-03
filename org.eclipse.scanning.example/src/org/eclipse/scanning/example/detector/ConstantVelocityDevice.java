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
 * This device mimicks telling EPICS to do a constant velcity scan down a line.
 * It basically is like a 1D malcolm device running in a scan. This allows I18
 * to continue doing a CVscan with the new scanning for mapping by creating a 
 * device to do the cv scan.
 * 
 * It writes an HDF5 file for the line (actually random data)
 * 
 * @author Matthew Gerring
 *
 */
public class ConstantVelocityDevice extends AbstractRunnableDevice<ConstantVelocityModel> implements IWritableDetector<ConstantVelocityModel>, INexusDevice<NXdetector> {

	private ILazyWriteableDataset context;
	private IDataset              data;
	
	public ConstantVelocityDevice() throws ScanningException {
		super(Services.getRunnableDeviceService()); // So that spring will work.
		this.model = new ConstantVelocityModel();
		setDeviceState(DeviceState.IDLE);
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
		int rank = info.getRank()+3; // scan rank plus three dimensions for the CV scan.
		
		context = detector.initializeLazyDataset(NXdetector.NX_DATA, rank, Double.class);
		
		// Setting chunking is a very good idea if speed is required.
		int[] chunk = info.createChunk(model.getLineSize(), model.getChannelCount(), model.getSpectraSize());
		context.setChunking(chunk);
		
		Attributes.registerAttributes(detector, this);

		return detector;
	}

	@Override
	public void run(IPosition pos) throws ScanningException, InterruptedException {
		// TODO Real device would tell EPICS to run the line scan now.
		// To simulate this, we create a line using the definition in the model
		// EPICS might write an HDF5 file with this data rather than the data 
		// being in memory.
		data = Random.rand(new int[]{model.getLineSize(), model.getChannelCount(), model.getSpectraSize()});
	}

	@Override
	public boolean write(IPosition pos) throws ScanningException {
		try {
			// In a real CV Scan the write step could be to either link in the HDF5 or read in its data 
			// and write a new record. Avoiding reading in the HDF5 being preferable.
			final IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(pos, model.getLineSize(), model.getChannelCount(), model.getSpectraSize());
			SliceND sliceND = new SliceND(context.getShape(), context.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
			context.setSlice(null, data, sliceND);

		} catch (Exception e) {
			throw new ScanningException(e.getMessage(), e); 
		}

		return true;
	}

	@Override
	public void configure(ConstantVelocityModel model) throws ScanningException {	
		super.configure(model);
		setName(model.getName());
	}

}
