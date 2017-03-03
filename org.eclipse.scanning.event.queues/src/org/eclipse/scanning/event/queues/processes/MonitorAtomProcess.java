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
import org.eclipse.dawnsci.nexus.NexusFile;
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
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.event.queues.ServicesHolder;

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

	public MonitorAtomProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher, blocking);
	}

	@Override
	protected void run() throws EventException, InterruptedException {
		try {
			broadcast(Status.RUNNING, "Writing position of: "+getQueueBean().getMonitor());
			broadcast(1.0);
			
			// Write file
			IScannableDeviceService sservice  = ServicesHolder.getScannableDeviceService();
			IScannable<?>           scannable = sservice.getScannable(getQueueBean().getMonitor());
			
			final File vistFile = createNewTemporaryFile();
			getQueueBean().setRunDirectory(vistFile.getParent());
			// Tell downstream which file to read
		    getQueueBean().setFilePath(vistFile.getAbsolutePath());
			
			final INexusFileFactory factory = ServicesHolder.getNexusFileFactory();
			final NexusFile file = factory.newNexusFile(vistFile.getAbsolutePath());
			file.openToWrite(true);
			broadcast(Status.RUNNING, 10.0, "Retrieved motor value.");
			
			String dataset = writeScannable(file, scannable);
			// Tell downstream which dataset to read
			getQueueBean().setDataset(dataset);
			broadcast(99.6);
						
		} catch (Exception ne) {
			ne.printStackTrace();
			reportFail(ne, "Write of file with value from '"+getQueueBean().getMonitor()+"' failed with: \""+ne.getMessage()+"\".");
			executed = true;		
			broadcast(Status.FAILED);
		} finally {
			processLatch.countDown();
		}
	}

	private String writeScannable(NexusFile file, IScannable<?> scannable) throws Exception {
		
		// We make a lazy writeable dataset to write out the mandels.
		final int[] shape = new int[]{1}; // Not sure about this, what about vector-data from the scannable?
		
		// Make NeXus group and lazy dataset (which can be used to append.
		final String name = UniqueUtils.getSafeName(getQueueBean().getName());
		GroupNode par = file.getGroup("/entry1/instrument", true); 
		ILazyWriteableDataset writer = new LazyWriteableDataset(name, Dataset.FLOAT, shape, shape, shape, null); // DO NOT COPY!
		file.createData(par, writer);
		broadcast(Status.RUNNING, 20.0, "Lazydataset created.");
		
		// Write Value
		SliceND slice = SliceND.createSlice(writer, new int[]{0}, new int[]{1});
		IDataset toWrite = DatasetFactory.createFromObject(scannable.getPosition());
		writer.setSlice(new IMonitor.Stub(), toWrite, slice); 
		broadcast(Status.RUNNING, 50.0, "Slice written.");
			
		file.close();
		
		return "/entry1/instrument/"+name;
	}


	private File createNewTemporaryFile() {
		IFilePathService service = ServicesHolder.getFilePathService();
		File dir = new File(service.getTempDir());
		final String name = UniqueUtils.getSafeName(getQueueBean().getName());
		return UniqueUtils.getUnique(dir, name, "nxs");
	}

	@Override
	protected void postMatchAnalysis() throws EventException, InterruptedException {
		try {
			postMatchAnalysisLock.lockInterruptibly();

			if (isTerminated()) {
				broadcast("Move aborted before completion (requested).");
				return;
			}

			executed = true;
			if (queueBean.getPercentComplete() >= 99.5 && !terminated) {
				//Clean finish
				broadcast(Status.COMPLETE, 100d, "Device move(s) completed.");
			} else {
				//Scan failed
				//TODO Set message? Or is it set elsewhere?
				broadcast(Status.FAILED);
			} 
		} finally {
			//And we're done, so let other processes continue
			executionEnded();

			postMatchAnalysisLock.unlock();

			/*
			 * N.B. Broadcasting needs to be done last; otherwise the next 
			 * queue may start when we're not ready. Broadcasting should not 
			 * happen if we've been terminated.
			 */
			if (!isTerminated()) {
				broadcast();
			}
		}
	}
	
	@Override
	public void doTerminate() throws EventException {
		try {
			//Reentrant lock ensures execution method (and hence post-match 
			//analysis) completes before terminate does
			postMatchAnalysisLock.lockInterruptibly();

			//TODO Additional abort action, not handled as part of run()?
			terminated = true;

			//Wait for post-match analysis to finish
			continueIfExecutionEnded();
			
		} catch (InterruptedException iEx) {
			throw new EventException(iEx);
		} finally {
			postMatchAnalysisLock.unlock();
		}
	}
	
	@Override
	public Class<MonitorAtom> getBeanClass() {
		return MonitorAtom.class;
	}

}
