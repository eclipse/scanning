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
package org.eclipse.scanning.sequencer.analysis;

import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentFile;
import org.eclipse.dawnsci.analysis.api.processing.IExecutionVisitor;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;
import org.eclipse.dawnsci.analysis.api.processing.OperationData;
import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.january.IMonitor;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.ProcessingModel;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;
import org.eclipse.scanning.sequencer.ServiceHolder;

/**
 * A runnable device that can be executed inline with the scan.
 * Runs any operation model.
 * 
 * TODO This device only deals with images currently. Make nD at some point...
 * 
 * @author Matthew Gerring
 *
 */
public class ProcessingRunnableDevice extends AbstractRunnableDevice<ProcessingModel> implements IWritableDetector<ProcessingModel>, INexusDevice<NXdetector> {
	
	private ILazyWriteableDataset processed;
	private NexusScanInfo info;


	public ProcessingRunnableDevice() {
		super(null); 
		setLevel(100); // Runs at the end of the cycle by default.
		setRole(DeviceRole.PROCESSING);
		this.model = new ProcessingModel(); // We start with an empty one in case they want to fill it in the UI.
	}
	
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info) throws NexusException {
		NXdetector detector = createNexusObject(info);
		NexusObjectWrapper<NXdetector> nexusProvider = new NexusObjectWrapper<NXdetector>(
				getName(), detector);

		// Add all fields for any NXdata groups that this device creates
		nexusProvider.setAxisDataFieldNames(NXdetector.NX_DATA);
		
		// "data" is the name of the primary data field (i.e. the 'signal' field of the default NXdata)
		nexusProvider.setPrimaryDataFieldName(NXdetector.NX_DATA);

		return nexusProvider;
	}
	
	@Override
	public void validate(ProcessingModel model) throws ValidationException {
		if (model.getDataFile()==null) throw new ModelValidationException("The data file must be set!", model, "dataFile");
		if (model.getOperationsFile()==null) throw new ModelValidationException("The operation file must be set!", model, "operationsFile");
		if (model.getDetectorName()==null) throw new ModelValidationException("The detector name must be set!", model, "detectorName");
		if (model.getTimeout()<=0) throw new ModelValidationException("The timeout must be greater than 0", model, "timeout");
	}

	public NXdetector createNexusObject(NexusScanInfo info)  throws NexusException {
		
		final NXdetector detector = NexusNodeFactory.createNXdetector();
		
		// TODO Hard coded to images
		this.processed = detector.initializeLazyDataset(NXdetector.NX_DATA, info.getRank()+2, Double.class);
		this.info      = info;		
		
		Attributes.registerAttributes(detector, this);

		return detector;
	}

	private ILazyDataset       data;
	private IOperationService  oservice;
	private IOperationContext  context;

	@Override
	public void run(IPosition loc) throws ScanningException, InterruptedException {

		// We cannot run the processing until the earlier frames were definitely
		// written. Therefore we do the processing in the write(...) 
		// TODO This could be made more efficient, the processing could
		// check that the data has written for a given slice and
		// run in this method...
	}
	
	private IScanSlice rslice;
	
	@Override
	public boolean write(IPosition loc) throws ScanningException {

		try {
			// Get the frame
			ILoaderService lservice = ServiceHolder.getLoaderService();
			IDataHolder    holder   = lservice.getData(model.getDataFile(), new IMonitor.Stub());

			// TODO Might not be correct place to find the detector. Ideally
			// An object in INexusDevice interface should provide where the node is.
			this.data = holder.getLazyDataset("/entry/instrument/"+model.getDetectorName()+"/data");

			if (context==null) {
				createOperationService();
				processed.setChunking(info.createChunk(getImageShape()));
			}
			
			this.rslice = IScanRankService.getScanRankService().createScanSlice(loc, getImageShape());
			SliceND slice = new SliceND(processed.getShape(), processed.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
			IDataset set = data.getSlice(slice);
			context.setData(set); // Just this frame.
	        oservice.execute(context);
	        
	        // TODO Write it!
	        
		} catch (Exception ne) {
			throw new ScanningException("Cannot run processing from "+model.getOperationsFile(), ne);
		}
		return true;
	}

	private int[] getImageShape() {
		// TODO Hard coded to images
		return new int[]{data.getShape()[data.getShape().length-2], data.getShape()[data.getShape().length-1]};
	}

	private void createOperationService() throws ScanningException {
		
		try {
			IOperation<?,?>[]         operations;
			
			// TODO Currently we assume that the templates for 
			// the pipelines to run come from that saved by the UI which
			// is the version in IPersistentFile.
			// It might be that now there is a correct NeXus way of recording the 
			// processing that this could be supported as well/instead.
			// For now we have:
			// 1. Get operations as required in UI on some existing data.
			// 2. Save the persistent file in UI
			// 3. Set the file location in the ProcessingModel
			// 4. Run the processing device in the scan.
			if (getModel().getOperationsFile()!=null) {
				final IPersistenceService pservice   = ServiceHolder.getPersistenceService();
				IPersistentFile           file       = pservice.createPersistentFile(getModel().getOperationsFile());
				operations = file.getOperations();
				
			} else if (getModel().getOperation()!=null) {
				operations = new IOperation<?,?>[]{(IOperation<?,?>)getModel().getOperation()};
			} else {
				throw new ScanningException("No persisted operations file supplied!");
			}
			
	        this.oservice = ServiceHolder.getOperationService();
	        if (oservice == null) throw new ScanningException("Unable to use device '"+getName()+"' because no operations service is available.");
	    
	        this.context = oservice.createContext();
	        context.setSeries(operations);
			context.setVisitor(new IExecutionVisitor.Stub() {
				@Override
				public void executed(OperationData result, IMonitor monitor) throws Exception {
					IDataset lastResult = result.getData();
					SliceND slice = new SliceND(processed.getShape(), processed.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
					processed.setSlice(null, lastResult, slice);
				}			
			});

	        
	        // The data dimensions are the scan dimensions.
			// TODO Hard coded to images
	        context.setDataDimensions(new int[]{data.getRank()-2, data.getRank()-1});
        		
		} catch (ScanningException known) {
			throw known;
		} catch (Exception ne) {
			throw new ScanningException("Cannot run processing from "+model.getOperationsFile(), ne);
		}
	}

}
