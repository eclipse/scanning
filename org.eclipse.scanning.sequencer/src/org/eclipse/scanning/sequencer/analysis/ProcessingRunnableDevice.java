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
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.device.models.ProcessingModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.rank.IScanSlice;
import org.eclipse.scanning.sequencer.ServiceHolder;

/**
 * A runnable device that can be executed inline with the scan.
 * Runs any operation model.
 *
 * TODO This device only deals with images currently. Make nD at some point...
 * TODO This device is not finished and functional, it is not recommended to
 * expose to users unless more testing and improvements have occurred.
 *
 * @author Matthew Gerring
 *
 */
public class ProcessingRunnableDevice extends SlicingRunnableDevice<ProcessingModel> implements INexusDevice<NXdetector> {

	private ILazyWriteableDataset processed;
	private NexusScanInfo         info;

	private IOperationService  oservice;
	private IOperationContext  context;

	private IScanSlice   rslice;
	private ILazyDataset data;


	public ProcessingRunnableDevice() {
		super();
		this.model = new ProcessingModel(); // We start with an empty one in case they want to fill it in the UI.
	}

	@Override
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info) throws NexusException {
		NXdetector detector = createNexusObject(info);
		NexusObjectWrapper<NXdetector> nexusProvider = new NexusObjectWrapper<NXdetector>(getName(), detector);

		// Add all fields for any NXdata groups that this device creates
		nexusProvider.setAxisDataFieldNames(NXdetector.NX_DATA);

		// "data" is the name of the primary data field (i.e. the 'signal' field of the default NXdata)
		nexusProvider.setPrimaryDataFieldName(NXdetector.NX_DATA);

		return nexusProvider;
	}
	public NXdetector createNexusObject(NexusScanInfo info)  throws NexusException {

		final NXdetector detector = NexusNodeFactory.createNXdetector();

		// TODO Hard coded to images
		this.processed = detector.initializeLazyDataset(NXdetector.NX_DATA, info.getRank()+2, Double.class);
		this.info      = info;

		Attributes.registerAttributes(detector, this);

		return detector;
	}

	@Override
	public void validate(ProcessingModel model) throws ValidationException {
		super.validate(model);
		if (model.getOperationsFile()==null) throw new ModelValidationException("The operation file must be set!", model, "operationsFile");
	}

	@Override
	public boolean process(SliceDeviceContext sdcontext) throws ScanningException {

		try {

			this.rslice = sdcontext.getScanSlice();
			this.data   = sdcontext.getData();

			if (context==null) {
				createOperationService();
				processed.setChunking(info.createChunk(getDataShape(data)));
			}

			context.setData(sdcontext.getSlice()); // Just this frame.
	        oservice.execute(context);

		} catch (Exception ne) {
			throw new ScanningException("Cannot run processing from "+model.getOperationsFile(), ne);
		}
		return true;
	}

	@ScanFinally
	public void clear() {
		this.data = null;
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
