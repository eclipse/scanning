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
package org.eclipse.scanning.event.queues.processes;

import java.io.File;

import org.eclipse.dawnsci.analysis.api.tree.GroupNode;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.january.DatasetException;
import org.eclipse.january.IMonitor;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.LazyWriteableDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.beans.MonitorAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.scan.IFilePathService;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MonitorAtomProcess reads back a single value from a monitor. It will use 
 * the view detector methods discussed that should be available as part of the
 * Mapping project.
 * 
 * MonitorAtom has the scannable name to find information.
 * 1. Read value
 * 2. Write to a file visit/tmp 
 * 3. Set file path written to MonitorAtom
 * 4. Set the Status to RUNNING, set %-complete at 99.6% 
 * 5. Unit test similar to MoveAtomProcessTest
 * 
 * Michael will take this class forwards once Matt has completed a basic version.
 * 
 * @author Michael Wharmby
 * @author Matthew Gerring
 *
 * @param <T> The {@link Queueable} specified by the {@link IConsumer} 
 *            instance using this MonitorAtomProcess. This will be 
 *            {@link QueueAtom}.
 */
public class MonitorAtomProcess<T extends Queueable> extends QueueProcess<MonitorAtom, T> {
	
	/**
	 * Used by {@link QueueProcessFactory} to identify the bean type this 
	 * {@link QueueProcess} handles.
	 */
	public static final String BEAN_CLASS_NAME = MonitorAtom.class.getName();
	
	private static Logger logger = LoggerFactory.getLogger(MonitorAtomProcess.class);
	
	//Scanning infrastructure
	private final IScannableDeviceService scanDevService;
	private final IFilePathService fPathService;
	private final INexusFileFactory nexusFileFactory;

	public MonitorAtomProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher, blocking);
		//Get the scanDevService from the OSGi configured holder.
		scanDevService = ServicesHolder.getScannableDeviceService();
		fPathService = ServicesHolder.getFilePathService();
		nexusFileFactory = ServicesHolder.getNexusFileFactory();
	}

	@Override
	protected void run() throws EventException, InterruptedException {
		//Get the path where the NeXus file will be written to
		logger.debug("Getting path for uniquely named NeXus file to write data to...");
		broadcast(Status.RUNNING, 1.0, "Getting path for uniquely named NeXus file to write data to...");

		File visitTmpDir = new File(fPathService.getTempDir());
		final String fileName = UniqueUtils.getSafeName(queueBean.getName());
		final File visitFile = UniqueUtils.getUnique(visitTmpDir, fileName, "nxs");
		queueBean.setRunDirectory(visitTmpDir.getAbsolutePath());
		// Tell downstream which file to read
		String filePath = visitFile.getAbsolutePath();
		queueBean.setFilePath(filePath);
		
		final NexusFile nxsFile = nexusFileFactory.newNexusFile(visitFile.getAbsolutePath());
		try {
			logger.debug("Preparing NeXus file for writing...");
			broadcast(Status.RUNNING, 20.0, "Opening NeXus file for writing...");
			
			//Open the Nexus file & create the group/tree structure
			nxsFile.openToWrite(true);
			logger.debug("File '"+visitFile.getAbsolutePath()+"' created and ready for access");
			broadcast(Status.RUNNING, 25.0);
			final String datasetName = UniqueUtils.getSafeName(queueBean.getName());
			GroupNode nxsParent = nxsFile.getGroup("/entry1/instrument", true);
			if (isTerminated()) throw new InterruptedException();
			
			logger.debug("Created NeXus '/entry1/instrument/<uniquename>' tree");
			broadcast(Status.RUNNING, 30.0);
			
			//Create a LazyDataset ready to receive the monitor value
			final int[] shape = new int[]{1}; // Not sure about this, what about vector-data from the scannable?
			ILazyWriteableDataset datasetWriter = new LazyWriteableDataset(datasetName, Dataset.FLOAT, shape, shape, shape, null); // DO NOT COPY!
			nxsFile.createData(nxsParent, datasetWriter);
			SliceND slice = SliceND.createSlice(datasetWriter, new int[]{0}, new int[]{1});
			if (isTerminated()) throw new InterruptedException();
			
			//Read data, write slice and close file
			logger.debug("Getting scannable'"+queueBean.getMonitor()+"' and reading current value");
			broadcast(Status.RUNNING, 40.0, "Reading value of monitor '"+queueBean.getMonitor()+"'");
			
			IScannable<?> scannable = scanDevService.getScannable(queueBean.getMonitor());
			try {
				IDataset toWrite = DatasetFactory.createFromObject(scannable.getPosition());
				datasetWriter.setSlice(new IMonitor.Stub(), toWrite, slice);
			} catch (DatasetException dsEx) {
				// Because getPosition throws Exception, we need to rethrow other exception types of interest
				throw dsEx;
			} catch (Exception ex) {
				throw new ScanningException("Could not read scannable position", ex);
			}
			
			nxsFile.close();
			if (isTerminated()) throw new InterruptedException();
			
			logger.debug("Data successfully written to file");
			broadcast(Status.RUNNING, 70.0, "Monitor value recorded to file");
			
			//Record the full dataset path in the MonitorAtom
			queueBean.setDataset("/entry1/instrument/"+datasetName);
			broadcast(99.6);
		} catch (NexusException nxEx) {
			logger.error("Failed whilst accessing NeXus file '"+filePath+"': "+nxEx.getMessage());
			broadcast(Status.FAILED, "Problems encountered accessing NeXus file: "+nxEx.getMessage());
		} catch (DatasetException dsEx) {
			logger.error("Could not pass data from monitor into LazyDataset for writing");
			broadcast(Status.FAILED, "Failed writing to LazyDataset: "+dsEx.getMessage());
		} catch (ScanningException scEx) {
			logger.error("Failed to get monitor with the name '"+queueBean.getMonitor()+"': "+scEx.getMessage());
			broadcast(Status.FAILED, "Failed to get monitor with the name '"+queueBean.getMonitor()+"'");
		} catch (InterruptedException irEx) {
			//We were terminated. Do tidy up and stop
			logger.debug("Processing was interrupted. Starting cleanup...");
			cleanUp(nxsFile);
		} finally {
			processLatch.countDown();
		}
	}
	
	private void cleanUp(NexusFile nxsFile) {
		final String path = nxsFile.getFilePath();
		final File nxsFSObj = new File(path);
		
		if (nxsFSObj.exists()) {
			try {
				nxsFile.close();
			} catch (NexusException nxEx) {
				logger.debug("Failed to close Nexus file {} during cleanUp", path);
			}
			
			boolean deletedNexusFile = nxsFSObj.delete();
			if (!deletedNexusFile) {
				logger.warn("Failed to delete NeXus file{} during cleanup", path);
			}
		}
	}

	@Override
	public void postMatchCompleted() {
		updateBean(Status.COMPLETE, 100d, "Successfully stored current value of '"+queueBean.getMonitor()+"'");
	}

	@Override
	public void postMatchTerminated() {
		queueBean.setMessage("Get value of '"+queueBean.getMonitor()+"' aborted (requested)");
//TODO		logger.debug("'"+bean.getName()+"' was requested to abort");
	}

//	@Override
//	public void postMatchFailed() {
//		queueBean.setStatus(Status.FAILED);//<-- Don't set message here; it's broadcast above!
//		logger.error("'"+bean.getName()+"' failed. Last message was: '"+bean.getMessage()+"'");
//	}
	
	@Override
	public Class<MonitorAtom> getBeanClass() {
		return MonitorAtom.class;
	}

}
