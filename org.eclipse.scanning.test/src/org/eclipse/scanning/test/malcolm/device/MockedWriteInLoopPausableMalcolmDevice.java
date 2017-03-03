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
package org.eclipse.scanning.test.malcolm.device;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.dawnsci.analysis.api.tree.GroupNode;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.january.IMonitor;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.LazyWriteableDataset;
import org.eclipse.january.dataset.Random;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.event.MalcolmEventBean;
import org.eclipse.scanning.api.malcolm.models.MapMalcolmModel;
import org.eclipse.scanning.api.scan.ScanningException;


/**
 * Device which pretends to write a 64x64 file a number of times
 * @author fri44821
 *
 */
public class MockedWriteInLoopPausableMalcolmDevice extends LoopingMockedMalcolmDevice {

	private NexusFileFactoryHDF5 factory;
	private NexusFile file;
	private ILazyWriteableDataset writer;
	
	public MockedWriteInLoopPausableMalcolmDevice() throws Exception {	
		this("zebra", new LatchDelegate());
	}

	public MockedWriteInLoopPausableMalcolmDevice(final String name, final LatchDelegate latcher) throws Exception {		
		
		super(name, latcher);
		
		factory = new NexusFileFactoryHDF5();
		final File ret = File.createTempFile("temp_transient_file", ".h5");
		ret.deleteOnExit();
		
		this.file = factory.newNexusFile(ret.getAbsolutePath(), false);
		file.openToWrite(true);
		GroupNode par = file.getGroup("/entry/data", true); // DO NOT COPY!

		final int[] shape = new int[] { 1, 64, 64 };
		final int[] max = new int[] { -1, 64, 64 };
		writer = new LazyWriteableDataset("image", Dataset.FLOAT, shape, max, shape, null); // DO NOT COPY!
		file.createData(par, writer);
		
		/**
		 * The task to be executed repeatably
		 */
		callableTask = new Callable<Long>() {
			
			@Override
			public Long call() throws Exception {

				int[] shape = (int[])model.getParameterMap().get("shape");
				if (shape==null) shape = new int[]{64,64};
				IDataset       rimage   = Random.rand(shape);
				rimage.setName("image");
				
				int[] start = { count, 0, 0 };
				int[] stop = { count + 1, 64, 64 };
				count++;
				
				writer.setSlice(new IMonitor.Stub(), rimage, start, stop, null);
				file.flush(); // remove explicit flush

				final MalcolmEventBean bean = new MalcolmEventBean(getState());
				bean.setPercentComplete((count/amount)*100d);	
				bean.setFilePath((String)model.getParameterMap().get("file"));
				bean.setDatasetPath("/entry/data");
				
				// Hardcoded shape change of dataset, in reality it will not be so simple.
				bean.setOldShape(new int[]{count-1, shape[0], shape[1]});
				bean.setNewShape(new int[]{count, shape[0], shape[1]});
				bean.setPreviousState(getDeviceState());
				bean.setDeviceState(getDeviceState());
				sendEvent(bean);

				System.err.println("> HDF5 wrote image to " + ret);
				System.err.println("> New shape " + Arrays.toString(writer.getShape()));
				
				return null;
			}
		};
	}

	@Override
	public void validate(MapMalcolmModel model) throws ValidationException {
		Map<String, Object> params = model.getParameterMap();
		if (!params.containsKey("shape")) throw new ValidationException("shape must be set!");
		if (!params.containsKey("nframes")) throw new ValidationException("nframes must be set!");
		if (!params.containsKey("file")) throw new ValidationException("file must be set!");
		if (!params.containsKey("exposure")) throw new ValidationException("exposure must be set!");
		return;
	}

	@Override
	public void configure(MapMalcolmModel params) throws ScanningException {
		
		validate(params);
		setState(DeviceState.CONFIGURING);
		this.model = params;
		if (params.getParameterMap().containsKey("configureSleep")) {
			try {
				long sleepTime = Math.round(((double)params.getParameterMap().get("configureSleep"))*1000d);
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				throw new MalcolmDeviceException(this, "Cannot sleep during configure!", e);
			}
		}
		setState(DeviceState.READY);
		
		// We configure a bean with all the scan specific things
		final MalcolmEventBean bean = new MalcolmEventBean();
		bean.setFilePath(params.getParameterMap().get("file").toString());
		bean.setDatasetPath("/entry/data");
		bean.setDeviceName(getName());
		bean.setBeamline("Testing");
		bean.setPercentComplete(0d);
        setTemplateBean(bean);
	}
}
