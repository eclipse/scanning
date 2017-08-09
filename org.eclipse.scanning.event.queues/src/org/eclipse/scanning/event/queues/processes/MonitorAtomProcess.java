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
 * 5. Unit test similar to PositionerAtomProcessTest
 * 
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

	public boolean incomplete = false;

	public MonitorAtomProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher, blocking);
		//Get the scanDevService from the OSGi configured holder.
		scanDevService = ServicesHolder.getScannableDeviceService();
		fPathService = ServicesHolder.getFilePathService();
		nexusFileFactory = ServicesHolder.getNexusFileFactory();
		
		//We make direct calls to scanning infrastructure which block -> need to happen in separate thread
		runInThread = true;
	}

	@Override
	protected void run() throws EventException {
		final File nxsFileObject; 
		NexusFile nxsFile = null; //This was final before. Don't know whether it needs to be, but this is a problem for cleanup(...)
		final String datasetName;
		final int[] shape;
		try {
			//Get the path where the NeXus file will be written to
			logger.debug("Getting path for uniquely named NeXus file to write data to...");
			broadcast(Status.RUNNING, 1.0, "Getting path for uniquely named NeXus file to write data to...");
			nxsFileObject = createFilePath();
			
			//Create a LazyDataset ready to receive the monitor value
			shape = queueBean.getDataShape(); // This is int[]: not sure about this, what about vector-data from the scannable?
			logger.debug("Creating lazy dataset with shape "+shape);
			broadcast(Status.RUNNING, 10.0, "Creating dataset with shape "+shape);
			datasetName = UniqueUtils.getSafeName(queueBean.getName());
			ILazyWriteableDataset datasetWriter = new LazyWriteableDataset(datasetName, Dataset.FLOAT, shape, shape, shape, null); // DO NOT COPY!
			SliceND slice = SliceND.createSlice(datasetWriter, new int[]{0}, new int[]{1});
			if (isTerminated()) throw new InterruptedException("Termination requested");

			logger.debug("Preparing NeXus file for writing...");
			broadcast(Status.RUNNING, 20.0, "Opening NeXus file for writing...");
			nxsFile = prepareNexus(nxsFileObject, datasetWriter);
			
			//Read data, write slice and close file
			logger.debug("Getting scannable'"+queueBean.getMonitor()+"' and reading current value");
			broadcast(Status.RUNNING, 40.0, "Reading value of monitor '"+queueBean.getMonitor()+"'");
			writeDataset(datasetWriter, slice);
			if (isTerminated()) throw new InterruptedException("Termination requested");
			
			//Record the full dataset path in the MonitorAtom
			logger.debug("Data successfully written to file");
			broadcast(Status.RUNNING, 70.0, "Monitor value recorded to file");
			queueBean.setDataset("/entry1/instrument/"+datasetName);
			broadcast(99.6);
			if (isTerminated()) throw new InterruptedException("Termination requested");

		} catch (InterruptedException iEx) {
			//We were terminated. Do tidy up and stop
			logger.debug("Processing was interrupted. Starting cleanup...");
			incomplete = true;
		} catch (EventException ex) {
			//Rethrowing EventExceptions (rather than not doing anything) seems to improve stability significantly (in the case of terminate)
			throw ex;
		}
		finally {
			cleanUp(nxsFile);
		}
	}
	
	private File createFilePath() throws InterruptedException {
		File visitTmpDir = new File(fPathService.getTempDir());
		final String fileName = UniqueUtils.getSafeName(queueBean.getName());
		final File visitFile = UniqueUtils.getUnique(visitTmpDir, fileName, "nxs");
		queueBean.setRunDirectory(visitTmpDir.getAbsolutePath());
		
		// Tell downstream which file to read
		String filePath = visitFile.getAbsolutePath();
		queueBean.setFilePath(filePath);
		if (isTerminated()) throw new InterruptedException("Termination requested");
		
		return visitFile;
	}
	
	private NexusFile prepareNexus(File visitFile, ILazyWriteableDataset datasetWriter) throws EventException, InterruptedException {
		//Open the Nexus file & create the group/tree structure
		final NexusFile nxsFile = nexusFileFactory.newNexusFile(visitFile.getAbsolutePath());
		try {
			nxsFile.openToWrite(true);
			GroupNode nxsParent = nxsFile.getGroup("/entry1/instrument", true);
			nxsFile.createData(nxsParent, datasetWriter);
			if (isTerminated()) throw new InterruptedException("Termination requested");
			
		} catch (NexusException nEx) {
			logger.error("Failed whilst accessing NeXus file '"+visitFile.getAbsolutePath()+"': "+nEx.getMessage(), nEx);
			broadcast(Status.FAILED, "Problems encountered accessing NeXus file: "+nEx.getMessage());
			throw new EventException("Problems encountered accessing NeXus file", nEx);
		}
		
		return nxsFile;
	}
	
	private void writeDataset(ILazyWriteableDataset datasetWriter, SliceND slice) throws EventException, InterruptedException {
		try {
			IScannable<?> scannable = scanDevService.getScannable(queueBean.getMonitor());
		
			IDataset toWrite = DatasetFactory.createFromObject(scannable.getPosition());
			datasetWriter.setSlice(new IMonitor.Stub(), toWrite, slice);
			if (isTerminated()) throw new InterruptedException("Termination requested");
			
		} catch (DatasetException dsEx) {
			logger.error("Could not pass data from monitor into LazyDataset for writing");
			broadcast(Status.FAILED, "Failed writing to LazyDataset: "+dsEx.getMessage());
			throw new EventException("Failed writing to LazyDataset", dsEx);
		} catch (ScanningException scEx) {
			logger.error("Failed to get monitor with the name '"+queueBean.getMonitor()+"': "+scEx.getMessage());
			broadcast(Status.FAILED, "Failed to get monitor with the name '"+queueBean.getMonitor()+"'");
			throw new EventException("Failed to get monitor with the name '"+queueBean.getMonitor()+"'", scEx);
		} catch (InterruptedException iEx) {
			//We don't actually want to catch this here - it's handled in the calling method
			throw iEx;
		} catch (Exception ex) {
			logger.error("Failed to read monitor with the name '"+queueBean.getMonitor()+"'");
			broadcast(Status.FAILED, "Failed to read monitor with the name '"+queueBean.getMonitor()+"'");
			throw new EventException("Failed to read monitor with the name '"+queueBean.getMonitor()+"'", ex);
		}
	}
	
	private void cleanUp(NexusFile nxsFile) {
		if (nxsFile == null) {
			return;
		}
		final String path = nxsFile.getFilePath();
		final File nxsFSObj = new File(path);
		
		if (nxsFSObj.exists()) {
			try {
				nxsFile.close();
			} catch (NexusException nxEx) {
				logger.debug("Failed to close Nexus file {} during cleanUp", path);
			}
			if (incomplete || terminated) {
				boolean deletedNexusFile = nxsFSObj.delete();
				if (!deletedNexusFile) {
					logger.warn("Failed to delete NeXus file{} during cleanup", nxsFSObj.getAbsolutePath());
				}
			}
		}
	}
	
	protected void terminateCleanupAction() {
		if (queueBean.getFilePath() == null) return;
		File nxsFSObj = new File(queueBean.getFilePath()).getAbsoluteFile();
		if (nxsFSObj.exists()) {
			//By deleting the object when terminate is called, we (should!) ensure that it really gets deleted
			boolean deletedNexusFile = nxsFSObj.delete();
			if (!deletedNexusFile) {
				logger.warn("Failed to delete NeXus file {} during cleanup", nxsFSObj.getAbsolutePath());
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
