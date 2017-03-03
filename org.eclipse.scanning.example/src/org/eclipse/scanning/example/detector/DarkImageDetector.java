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

import java.io.IOException;

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
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.example.Services;


/**
 * A dummy detector which must be set up with references to two Scannables representing X and Y positions. When used in a step scan, this detector generates a
 * value of 0 if the point (x, y) is in the Mandelbrot set, and greater than zero otherwise.
 * <p>
 * Note: values will always be high if used at (x, y) positions more than 2 units away from the origin.
 */
public class DarkImageDetector extends AbstractRunnableDevice<DarkImageModel> implements IWritableDetector<DarkImageModel>, INexusDevice<NXdetector> {


	private IDataset              image;
	private ILazyWriteableDataset data;
	private int darkCount = 0;
	
	@ScanFinally
	public void clean() {
		image = null;
		data  = null;
	}

	
	public DarkImageDetector() throws IOException {
		super(Services.getRunnableDeviceService()); // So that spring will work.
		this.model = new DarkImageModel();
	}
	
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info) throws NexusException {
		NXdetector detector = createNexusObject(info);
		return new NexusObjectWrapper<NXdetector>(getName(), detector, NXdetector.NX_DATA);
	}

	public NXdetector createNexusObject(NexusScanInfo info) throws NexusException {
		
		final NXdetector detector = NexusNodeFactory.createNXdetector();
		
		data = detector.initializeLazyDataset(NXdetector.NX_DATA, 3, Double.class);
		
		// Setting chunking is a very good idea if speed is required.
		data.setChunking(new int[]{1, model.getRows(), model.getColumns()});
		
		Attributes.registerAttributes(detector, this);
		
		return detector;
	}

	@Override
	public void configure(DarkImageModel model) throws ScanningException {	
		super.configure(model);
		setName(model.getName());
		darkCount = 0;
	}

	@Override
	public void run(IPosition pos) throws ScanningException, InterruptedException {
		
		// Other logic may be done here as when to get the dark image.
		if (pos.getStepIndex()%model.getFrequency() == 0) {
			image = Random.rand(new int[]{model.getRows(), model.getColumns()});
		} else {
			image = null;
		}
  	}
	
	@Override
    public boolean write(IPosition pos) throws ScanningException {
		
		if (image==null) return false;
		
		try {
			// Append the dark to the stack, without writing
			// NaNs.
			// This means an nD scan always gives a 3D dark stack.
			final int[] start = {darkCount,   0, 0};
			final int[] stop  = {darkCount+1, model.getRows(), model.getColumns()};			
			SliceND slice = new SliceND(data.getShape(), data.getMaxShape(), start, stop, null);			
			data.setSlice(null, image, slice);
			darkCount+=1;
			
		} catch (Exception e) {
			throw new ScanningException(e.getMessage(), e); 
		}

		return true;
	}


	@Override
	public void abort() throws ScanningException {
		throw new ScanningException("Operation not supported!");
	}

	@Override
	public void pause() throws ScanningException {
		throw new ScanningException("Operation not supported!");
	}

	@Override
	public void resume() throws ScanningException {
		throw new ScanningException("Operation not supported!");
	}
	
}
