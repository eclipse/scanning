/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.scanning.example.xcen.consumer;

import java.io.File;
import java.io.IOException;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.AbstractLockingPausableProcess;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.example.xcen.beans.XcenBean;

/**
 * Rerun of several collections as follows:
 * o Write the Xia2 command file, automatic.xinfo
 * o Runs Xia2 with file
 * o Progress reported by stating xia2.txt
 * o Runs xia2 html to generate report.
 * 
 * NOTE:
 * Most of this has been rewritten using examples from DryRunProcess for 
 * proper synchronisation, plus corrections and simplifications.
 * 
 * This is only to produce a correct series of messages when testing. I
 * would not trust this code when running more complicated processes.
 * - Martin
 * 
 * @author Matthew Gerring
 * @author Martin Gaughran
 *
 */
public class XcenProcess extends AbstractLockingPausableProcess<XcenBean>{
	
	private boolean terminated;

	private Thread thread;
	
	private String processingDir;
	
	public XcenProcess(XcenBean bean, IPublisher<XcenBean> status) {
		
		super(bean, status);
				
        final String runDir;
        
        if (bean.getRunDirectory()==null) bean.setRunDirectory("xcenrun");
               
		if (isWindowsOS()) {
			// We are likely to be a test consumer, anyway the unix paths
			// from ISPyB will certainly not work, so we process in C:/tmp/
			runDir  = "C:/tmp/"+bean.getRunDirectory();
		} else {
			runDir  = "/tmp/"+bean.getRunDirectory();
		}

 		final File   xcenDir = getUnique(new File(runDir), "Xcen_", 1);
 		xcenDir.mkdirs();
		
	    processingDir = xcenDir.getAbsolutePath();
		bean.setRunDirectory(processingDir);
		
 		try {
			setLoggingFile(new File(xcenDir, "xcenJavaProcessLog.txt"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(processingDir, "xcenBean.json");
		} catch (Exception e) {
			e.printStackTrace(out);
		}
		
	}

	@Override
	public void execute() throws EventException, InterruptedException {
		
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					XcenProcess.this.run();
				} catch (EventException ne) {
					System.out.println("Cannot complete dry run");
					ne.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();	
	}
	
	private void run()  throws EventException {
		
		this.thread = Thread.currentThread();
		getBean().setPreviousStatus(Status.QUEUED);
		getBean().setStatus(Status.RUNNING);
		getBean().setPercentComplete(0d);

		terminated = false;
		for (int i = 0; i < 100; i++) {
			
			if (isTerminated())	return;
			
			System.out.println("Dry run : "+getBean().getPercentComplete()+" : "+getBean().getName());
			getBean().setPercentComplete(i);
			getPublisher().broadcast(getBean());
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				if (isTerminated()) return;
				System.out.println("Cannot complete dry run");
				e.printStackTrace();
			}
			
			checkPaused();
		}
		
		if (!isCancelled()) finishRun();
	}
	
	protected void finishRun() throws EventException {
		
		XcenBean xbean = (XcenBean)bean;
		xbean.setPreviousStatus(xbean.getStatus());
		xbean.setStatus(Status.COMPLETE);
		xbean.setPercentComplete(100);
		xbean.setMessage("Dry run complete (no software run)");
		xbean.setX(Math.random()*100);
		xbean.setY(Math.random()*100);
		xbean.setZ(Math.random()*100);
		broadcast(bean);
	}

	/**
	 * TODO Please implement the running of xcen properly
	 */
	protected void runDataCollection() throws EventException {
		
		// TODO Run a data collection			

		bean.setStatus(Status.COMPLETE);
		bean.setMessage("Reconstruction run completed normally");
		bean.setPercentComplete(100);
		broadcast(bean);
	}

	@Override
	public void doTerminate() throws EventException {
		if (thread!=null) thread.interrupt();
		terminated = true;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}

	public String getProcessingDir() {
		return processingDir;
	}

	public void setProcessingDir(String processingDir) {
		this.processingDir = processingDir;
	}

}
