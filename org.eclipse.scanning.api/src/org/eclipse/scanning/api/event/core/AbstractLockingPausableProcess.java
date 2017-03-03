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
package org.eclipse.scanning.api.event.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;

public abstract class AbstractLockingPausableProcess<T extends StatusBean> implements IConsumerProcess<T> {
	
	protected final T                      bean;
	protected final IPublisher<T>          publisher;
	private boolean                        isCancelled = false;

	/*
	 * Concurrency design recommended by Keith Ralphs after investigating
	 * how to pause and resume a collection cycle using Reentrant locks.
	 * Design requires these three fields.
	 */
	private ReentrantLock    lock;
	private Condition        paused;
	private volatile boolean awaitPaused;

	// Logging
	protected PrintStream out = System.out;

	protected AbstractLockingPausableProcess(T bean, IPublisher<T> publisher) {
		this.bean = bean;
		this.publisher = publisher;
		this.lock      = new ReentrantLock();
		this.paused    = lock.newCondition();
	}
	
	@Override
	public T getBean() {
		return bean;
	}

	@Override
	public IPublisher<T> getPublisher() {
		return publisher;
	}

	/**
	 * Blocks until process is not paused.
	 * 
	 * @throws EventException
	 */
	protected void checkPaused() throws EventException {
		
		// Check the locking using a condition
		try {
	    	if(!lock.tryLock(1, TimeUnit.SECONDS)) {
	    		throw new EventException("Internal Error - Could not obtain lock to run device!");    		
	    	}
	    	try {
	       	    if (awaitPaused) {
	         		paused.await(); // Until unpaused
	       	    }
	    	} finally {
	    		lock.unlock();
	    	}
		} catch (InterruptedException ne) {
			if (bean.getStatus().isTerminated()) return;
			throw new EventException(ne);
		}
		
	}

	/**
	 * Implements paused using a standard design
	 */
	@Override
	public void pause() throws EventException {
		try {
			lock.lockInterruptibly();
			
			awaitPaused = true;
			
			doPause();
			bean.setPreviousStatus(Status.REQUEST_PAUSE);
			bean.setStatus(Status.PAUSED);
			publisher.broadcast(bean);
			
		} catch (EventException ne) {
			throw ne;
		} catch (Exception ne) {
			throw new EventException(ne);
		} finally {
			lock.unlock();
		}

	}
	
	/**
	 * Override this method to do work on a pause once the pause lock has been received.
	 */
	protected void doPause()  throws Exception {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Implements resume using a standard design
	 */
	@Override
	public void resume() throws EventException {
		
		try {
			lock.lockInterruptibly();
		
			try {
				awaitPaused = false;
				
				doResume();
				bean.setPreviousStatus(Status.REQUEST_RESUME);
				bean.setStatus(Status.RESUMED);
				publisher.broadcast(bean);
				
				// We don't have to actually start anything again because the getMessage(...) call reconnects automatically.
				paused.signalAll();
				
			} finally {
				lock.unlock();
			}
		} catch (EventException ne) {
			throw ne;
		} catch (Exception ne) {
			throw new EventException(ne);
		}

	}
	
	@Override
	public boolean isPaused() {
		return awaitPaused;
	}

	/**
	 * Override this method to do work on a resume once the pause lock has been received.
	 */
	protected void doResume()  throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void terminate() throws EventException {
		
		try {
			lock.lockInterruptibly();
		
			try {
				awaitPaused = false;
				
				doTerminate();
				
				bean.setPreviousStatus(bean.getStatus());
				bean.setStatus(Status.TERMINATED);
				publisher.broadcast(bean);
				
				// We don't have to actually start anything again because the getMessage(...) call reconnects automatically.
				paused.signalAll();
				
			} finally {
				lock.unlock();
			}
		} catch (Exception ne) {
			bean.setPreviousStatus(bean.getStatus());
			bean.setStatus(Status.FAILED);
			bean.setMessage(ne.getMessage());
			publisher.broadcast(bean);

			if (!(ne instanceof EventException)) throw new EventException(ne);
			throw (EventException)ne;
		}
	}

	protected void doTerminate() throws Exception {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @return true if windows
	 */
	protected final static boolean isWindowsOS() {
		return (System.getProperty("os.name").indexOf("Windows") == 0);
	}
	


	/**
	 * @param dir
	 * @param template
	 * @param ext
	 * @param i
	 * @return file
	 */
	protected final File getUnique(final File dir, final String template, int i) {
		
		final File file = new File(dir, template + i );
		if (!file.exists()) {
			return file;
		}

		return getUnique(dir, template, ++i);
	}
	
	protected void setLoggingFile(File logFile) throws IOException {
		setLoggingFile(logFile, false);
	}
	/**
	 * Calling this method redirects the logging of this Java object
	 * which is available through the field 'out' to a known file.
	 * 
	 * @param logFile
	 * @throws IOException 
	 */
	protected void setLoggingFile(File logFile, boolean append) throws IOException {
		if (!logFile.exists()) logFile.createNewFile();
		this.out = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile, append)), true, "UTF-8");
		publisher.setLoggingStream(out);
	}

	

	/**
	 * Writes the project bean at the point where it is run.
	 * 
	 * @param processingDir2
	 * @param fileName - name of file
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	protected void writeProjectBean(final String dir, final String fileName) throws Exception {
		
		writeProjectBean(new File(dir), fileName);
	}
	
	/**
	 * 
	 * @param dir
	 * @param fileName
	 * @throws Exception
	 */
	protected void writeProjectBean(final File dir, final String fileName) throws Exception {
		
		final File beanFile = new File(dir, fileName);
     	beanFile.getParentFile().mkdirs();
    	if (!beanFile.exists()) beanFile.createNewFile();
    	
    	final FileOutputStream stream = new FileOutputStream(beanFile);
    	try {
    		String json = publisher.getConnectorService().marshal(bean);
    		stream.write(json.getBytes("UTF-8"));
    	} finally {
    		stream.close();
    	}
	}


	/**
	 * Notify any clients of the beans status
	 * @param bean
	 */
	public void broadcast(StatusBean tbean) {
		try {
			bean.merge(tbean);
			publisher.broadcast(bean);
		} catch (Exception e) {
			throw new RuntimeException("Cannot broadcast", e);
		}
 	}

	protected void dryRun() throws EventException, InterruptedException {
		dryRun(100);
	}
	protected void dryRun(int size) throws EventException, InterruptedException {
        dryRun(size, true);
	}
	
	protected void dryRun(int size, boolean complete) throws EventException, InterruptedException {
	
		bean.setPreviousStatus(Status.SUBMITTED);
		bean.setStatus(Status.RUNNING);
		bean.setPercentComplete(0d);
		
		for (int i = 0; i < size; i++) {
			
			checkPaused();
			if (isCancelled) {
				bean.setStatus(Status.TERMINATED);
				broadcast(bean);
				return;
			}
			if (bean.getStatus()==Status.REQUEST_TERMINATE ||
			    bean.getStatus()==Status.TERMINATED) {
				return;
			}
			Thread.sleep(100);
			//System.out.println("Dry run : "+bean.getPercentComplete());
			bean.setPercentComplete(i);
			broadcast(bean);
		}
		
		if (complete) {
			bean.setStatus(Status.COMPLETE);
			bean.setPercentComplete(100);
			bean.setMessage("Dry run complete (no software run)");
			broadcast(bean);
		}
	}

	


	public boolean isCancelled() {
		return isCancelled;
	}


	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
	


}
