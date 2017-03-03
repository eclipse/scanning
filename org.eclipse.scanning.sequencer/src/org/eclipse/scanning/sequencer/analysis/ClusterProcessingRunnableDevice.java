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

import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.GROUP_NAME_SOLSTICE_SCAN;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.dawnsci.analysis.api.processing.IOperationBean;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.device.models.ClusterProcessingModel;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.ScanMode;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.IFilePathService;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterProcessingRunnableDevice extends AbstractRunnableDevice<ClusterProcessingModel>
		implements IWritableDetector<ClusterProcessingModel> {

	private static final Logger logger = LoggerFactory.getLogger(ClusterProcessingRunnableDevice.class);
	
	public static final String DEFAULT_ENTRY_PATH = "/entry/";
	
	public static final String PROCESSING_QUEUE_NAME = "scisoft.operation.SUBMISSION_QUEUE";
	
	public static final String NEXUS_FILE_EXTENSION = ".nxs";
	
	private static ISubmitter<StatusBean> submitter = null;
	
	public ClusterProcessingRunnableDevice() {
		super(ServiceHolder.getRunnableDeviceService());
		setRole(DeviceRole.PROCESSING);
		setSupportedScanModes(ScanMode.SOFTWARE, ScanMode.HARDWARE);
	}
	
	@ScanStart
	public void submitProcessingOperation(ScanInformation scanInfo) {
		final IOperationBean operationBean = createOperationBean(scanInfo);
		try {
			getSubmitter().submit((StatusBean) operationBean);
		} catch (Exception e) {
			logger.error("Could not submit processing bean for processing step" + getName());
		}
	}
	
	@Override
	public void validate(ClusterProcessingModel model) throws ValidationException {
        super.validate(model);
        if (model.getDetectorName()==null || model.getDetectorName().length()<1) {
        	throw new ModelValidationException("The detector must be set", model, "detectorName");
        }
	}
	
	private ISubmitter<StatusBean> getSubmitter() throws Exception {
		if (submitter == null) {
			submitter = createSubmitter();
		}
		return submitter;
	}
	
	private ISubmitter<StatusBean> createSubmitter() throws Exception {
		try {
			URI uri = new URI(CommandConstants.getScanningBrokerUri());
			IEventService eventService = ServiceHolder.getEventService();
			return eventService.createSubmitter(uri, PROCESSING_QUEUE_NAME);
		} catch (URISyntaxException e) {
			logger.error("Could not create URI " , e);
			throw e;
		}
	}
	
	private IOperationBean createOperationBean(ScanInformation scanInfo) {
		final ClusterProcessingModel model = getModel();
		
		final IOperationBean operationBean = ServiceHolder.getOperationService().createBean();
		operationBean.setName(model.getName());
		
		final IFilePathService filePathService = ServiceHolder.getFilePathService();
		operationBean.setRunDirectory(filePathService.getTempDir()); // temp dir in visit
		String scanFilePath = scanInfo.getFilePath();
		operationBean.setFilePath(scanFilePath); // name of nexus file produced by scan
		operationBean.setProcessingPath(model.getProcessingFilePath()); // The name of the processing file
		operationBean.setDeleteProcessingFile(false);
		operationBean.setOutputFilePath(getOutputFilePath(scanFilePath));

		// The path for the NXdata for the primary data field of the detector
		operationBean.setDatasetPath(DEFAULT_ENTRY_PATH + model.getDetectorName());
		operationBean.setScanRank(scanInfo.getRank()); // set scan rank
		operationBean.setDataKey(DEFAULT_ENTRY_PATH + GROUP_NAME_SOLSTICE_SCAN);

		operationBean.setReadable(true); // true as this is a SWMR file so can be read while scan in running
		operationBean.setXmx("1024m"); // TODO what should this be set to?
		operationBean.setNumberOfCores(1); // for now
		
		return operationBean;
	}
	
	private String getOutputFilePath(String inputFilePath) {
		// append input file name with processing name
		final String inputFileNameNoExt = getFileNameWithoutExtension(inputFilePath);
		// if this is a file path, we just get the name. If it's a name, no change
		final String processingName = getFileNameWithoutExtension(getModel().getName());
		
		String outputFileNamePrefix = inputFileNameNoExt + "-" + processingName;
		String outputDirStr = ServiceHolder.getFilePathService().getProcessedFilesDir();
		File outputDir = new File(outputDirStr);
		
		//  
		File outputFile = new File(outputDir, outputFileNamePrefix + NEXUS_FILE_EXTENSION);
		int suffix = 0;
		while (outputFile.exists()) {
			suffix++;
			outputFile = new File(outputDir, outputFileNamePrefix + "-" + suffix + NEXUS_FILE_EXTENSION);
		}
		
		return outputFile.toString();
	}
	
	private String getFileNameWithoutExtension(String filePath) {
		String fileName = new File(filePath).getName();
		int i = fileName.lastIndexOf('.');
		if (i != -1) {
			fileName = fileName.substring(0, i);
		}
		
		return fileName;
	}
	
	@Override
	public void run(IPosition position) throws ScanningException, InterruptedException {
		// Do nothing here
	}

	@Override
	public boolean write(IPosition position) throws ScanningException {
		// do nothing here
		return true;
	}

}
