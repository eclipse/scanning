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
package org.eclipse.scanning.sequencer;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.ILevel;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.annotation.scan.AnnotationManager;
import org.eclipse.scanning.api.annotation.scan.LevelEnd;
import org.eclipse.scanning.api.annotation.scan.LevelStart;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.scan.LevelInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.event.PositionDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Runs any collection of objects using an executor service
 * by reading their levels. On service runs all the devices
 * at each level and waits for them to finish.
 * 
 * The implementing class provides the Callable which runs the
 * actual task. For instance setting a position.
 * 
 * @author Matthew Gerring
 *
 */
abstract class LevelRunner<L extends ILevel> {
	
	private static Logger logger = LoggerFactory.getLogger(LevelRunner.class);

    protected IPosition                 position;
    private volatile ForkJoinPool       eservice; // Different threads may nullify the service, better to make volatile.
	private ScanningException           abortException;
	private PositionDelegate            pDelegate;
	private boolean                     levelCachingAllowed=true;
	
	protected LevelRunner() {
		pDelegate = new PositionDelegate();
	}

	/**
	 * Get a list of the objects which we would like to order by level.
	 * @return
	 */
	protected abstract Collection<L> getDevices() throws ScanningException ;

	/**
	 * Implement this method to create a callable which willbe run by the executor service.
	 * If a given level object and position return null, no work will be done for that object at that level.
	 * 
	 * @param levelObject
	 * @param position
	 * @return a callable that returns the position reached once it has finished running. May return null to
	 * do no work for a given create level.
	 * 
	 * @throws ScanningException
	 */
	protected abstract Callable<IPosition> create(L levelObject, IPosition position)  throws ScanningException;
	
	/**
	 * Call to set the value at the location specified
	 * Same as calling run(position, true)
	 * 
	 * @param position
	 * @return
	 * @throws ScanningException
	 */
	protected boolean run(IPosition position) throws ScanningException, InterruptedException {
        return run(position, true);
	}
	
	/**
	 * Call to set the value at the location specified
	 * @param position
	 * @param block - set to true to block until the parallel tasks have completed. Set to false to
	 * return after the last level is told to execute. In this case more work can be done on the calling 
	 * thread. Use the latch() method to come back to the last level's ExecutorService and 
	 * @return
	 * @throws ScanningException
	 */
	protected boolean run(IPosition loc, boolean block) throws ScanningException, InterruptedException {
		
		if (abortException!=null) throw abortException;

		/** NOTE: The position is passed down to run in the tread pool.
		 *  A subsequent run and await could in theory return the last
		 *  run position while returning the last-1 run position.
		 *  Position is a best guess of what position happened.
		 */
		this.position = loc;
		boolean ok = pDelegate.firePositionWillPerform(loc);
        if (!ok) return false;
		
		Map<Integer, List<L>> positionMap = getLevelOrderedDevices(getDevices());
		Map<Integer, AnnotationManager> managerMap = getLevelOrderedManagerMap(positionMap);
		
		try {
			// TODO Should we actually create the service size to the size
			// of the largest level population? This would mean that you try to 
			// start everything at the same time.
			if (eservice==null) this.eservice = createService();

			Integer finalLevel = 0;
			for (Iterator<Integer> it = positionMap.keySet().iterator(); it.hasNext();) {
			    
				if (abortException!=null) throw abortException;
				
				int level = it.next();
				List<L> lobjects = positionMap.get(level);
				Collection<Callable<IPosition>> tasks = new ArrayList<>(lobjects.size());
				for (L lobject : lobjects) {
					Callable<IPosition> c = create(lobject, loc);
					if (c==null) continue; // legal to say that there is nothing to do for a given object.
					tasks.add(c);
				}
				
				managerMap.get(level).invoke(LevelStart.class, loc, new LevelInformation(level, lobjects));
				if (!it.hasNext() && !block) { 
					// The last one and we are non-blocking
					for (Callable<IPosition> callable : tasks) eservice.submit(callable);
				} else {
					// Normally we block until done.
					// Blocks until level has run
				    List<Future<IPosition>> pos = eservice.invokeAll(tasks, getTimeout(lobjects), TimeUnit.SECONDS);
				    
				    // If timed out, some isDone will be false.
				    for (Future<IPosition> future : pos) {
						if (!future.isDone()) throw new ScanningException("The timeout of "+timeout+"s has been reached waiting for level "+level+" objects "+toString(lobjects));
					}
				    pDelegate.fireLevelPerformed(level, lobjects, getPosition(loc, pos));
				}
				managerMap.get(level).invoke(LevelEnd.class, loc, new LevelInformation(level, lobjects));
			}
			
			pDelegate.firePositionPerformed(finalLevel, loc);
			
		} catch (ScanningException s) {
			throw s;
		} catch (InterruptedException i) {
			throw i;
		} catch (Exception ne) {
			if (abortException!=null) throw abortException;
			throw new ScanningException("Scanning interupted while moving to new position!", ne);
			
		} finally {
			if (block) await();
		}
		
		return true;
	}

	protected String toString(List<L> lobjects) {
		final  StringBuilder buf = new StringBuilder("[");
		for (L l : lobjects) {
			buf.append(l);
			buf.append(", ");
		}
		return buf.toString();
	}

	/**
	 * The timeout is overridden by some subclasses.
	 */
	private long timeout = 10;

	/** 
	 * Blocks until all the tasks have complete. In order for this call to be worth
	 * using run(position, false) should have been used to run the service.
	 * 
	 * If executor does not shutdown within 1 minute, throws an exception.
	 * 
	 * If nothing has been run by the runner, there will be no executor service
	 * created and latch() will directly return.
	 * 
	 * @throws InterruptedException 
	 */
	protected IPosition await() throws InterruptedException, ScanningException {
        return await(getTimeout(null));
	}
	
	/** 
	 * Blocks until all the tasks have complete. In order for this call to be worth
	 * using run(position, false) should have been used to run the service.
	 * 
	 * If nothing has been run by the runner, there will be no executor service
	 * created and latch() will directly return.
	 * 
	 * @throws InterruptedException 
	 * @return the position of the last 'run' call, which may not always be what was awaited.
	 */
	protected IPosition await(long time) throws InterruptedException, ScanningException{
		if (eservice==null)          return position;
		if (eservice.isTerminated()) {
			eservice = null;
			return position;
		}
		boolean ok = eservice.awaitQuiescence(time, TimeUnit.SECONDS); 
		if (!ok) { // Might have nullified service during wait.
			throw new ScanningException("The timeout of "+timeout+"s has been reached, scan aborting. Please implement ITimeoutable to define how long your device needs to write.");
		}
		return position;
	}
	
	public void abort() {
		if (eservice==null) return; // We are already finished
		eservice.shutdownNow();
		eservice = null;
	}
	
	/**
	 * 
	 * @param device
	 * @param value
	 * @param pos
	 * @param ne
	 */
	protected void abort(INameable device, Object value, IPosition pos, Throwable ne) {
		String message = "Cannot run device named '"+device.getName()+"' value is '"+value+"' and position is '"+pos+"'\nMessage: "+ne.getMessage();
		abort(device, message, pos, ne);
	}

	protected void abort(INameable device, IPosition pos, Throwable ne) {
		String message = "Cannot run device named '"+device.getName()+"' position is '"+pos+"'\nMessage: "+ne.getMessage();
		abort(device, message, pos, ne);
	}

	private void abort(INameable device, String message, IPosition pos, Throwable ne) {
		//logger.error(message, ne); // Just for testing we make sure that the stack is visible.
        abortException = ne instanceof ScanningException 
        		       ? (ScanningException)ne
        		       : new ScanningException(ne.getMessage(), ne);
		eservice.shutdownNow();
		eservice = null;
	}
	
	/**
	 * Attempts to close the thread pool and log exceptions
	 */
	public void close() {
		if (eservice==null) return; // We are already finished
		try {
			eservice.shutdown();
			eservice.awaitTermination(getTimeout(null), TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.debug("Unexpected forced termination of pool", e);
		} finally {
		    eservice = null;
		}
	}

	public void reset() {
		abortException = null;
	}

	private SoftReference<Map> sortedObjects;
	/**
	 * Get the scannables, ordered by level, lowest first
	 * @param position
	 * @return
	 * @throws ScanningException 
	 */
	protected Map<Integer, List<L>> getLevelOrderedDevices(final Collection<L> objects) throws ScanningException {
		
		if (objects==null) return Collections.emptyMap();
		
		if (sortedObjects!=null && sortedObjects.get()!=null) return sortedObjects.get();
		
		final Map<Integer, List<L>> ret = new TreeMap<>();
		for (L object : objects) {
			final int level = object.getLevel();
		
			if (!ret.containsKey(level)) ret.put(level, new ArrayList<L>(7));
			ret.get(level).add(object);
		}
		if (isLevelCachingAllowed()) sortedObjects = new SoftReference<Map>(ret);
		
		return ret;
	}
	
	private SoftReference<Map> sortedManagers;

	private Map<Integer, AnnotationManager> getLevelOrderedManagerMap(Map<Integer, List<L>> positionMap) {
		
		if (sortedManagers!=null && sortedManagers.get()!=null) return sortedManagers.get();

		final Map<Integer, AnnotationManager> ret = new HashMap<>();
		for (Integer position : positionMap.keySet()) {
			ret.put(position, new AnnotationManager(SequencerActivator.getInstance(), LevelStart.class, LevelEnd.class));	// Less annotations is more efficient
			ret.get(position).addDevices(positionMap.get(position));
		}
		if (isLevelCachingAllowed()) sortedManagers = new SoftReference<Map>(ret);
		return ret;
	}
	

	protected ForkJoinPool createService() {
		// TODO Need spring config for this.
		Integer processors = Integer.getInteger("org.eclipse.scanning.level.runner.pool.count");
		if (processors==null || processors<1) processors = Runtime.getRuntime().availableProcessors();
		return new ForkJoinPool(processors);
        // Slightly faster than thread pool executor @see ScanAlgorithmBenchMarkTest
	}

	public void addPositionListener(IPositionListener listener) {
		pDelegate.addPositionListener(listener);
	}

	public void removePositionListener(IPositionListener listener) {
		pDelegate.removePositionListener(listener);
	}

	/**
	 * 
	 * @return the position of the last 'run' call, which may not always be what was run.
	 */
	public IPosition getPosition()  throws ScanningException {
		return position;
	}

	private IPosition getPosition(IPosition position, List<Future<IPosition>> futures) throws InterruptedException, ExecutionException {
		MapPosition ret = new MapPosition();
	    for (Future<IPosition> future : futures) {
	    	// Faster than using composite
	    	IPosition pos = future.get();
	    	if (pos==null) continue;
	    	ret.putAll(pos);
	    	ret.putAllIndices(pos);
		}
	    if (ret.size()<1) return position;
	    return ret;
	}

	public static <T extends ILevel> LevelRunner<T> createEmptyRunner() {
		return new LevelRunner<T>() {
			
			@Override
			protected boolean run(IPosition position, boolean block) {
				this.position = position;
				return true;
			}
			@Override
			protected IPosition await(long time) throws InterruptedException {
				return position;
			}

			@Override
			protected Collection<T> getDevices() throws ScanningException {
				return null;
			}

			@Override
			protected Callable<IPosition> create(T levelObject, IPosition position) throws ScanningException {
				return null;
			}
			
		};
	}

	/**
	 * The await and maximum run time in seconds.
	 * @param the objects at this level. If null then the maximum of all possible objects in the runner should be used.
	 * @return
	 */
	public long getTimeout(List<L> objects) {
		if (Boolean.getBoolean("org.eclipse.scanning.sequencer.debug")) {
			return Long.MAX_VALUE; // So long that hell may have frozen over...
		}
		return timeout;
	}

	/**
	 * The await time in sceonds.
	 * @return time
	 */
	public void setTimeout(long time) {
		this.timeout = time;
	}

	public boolean isLevelCachingAllowed() {
		return levelCachingAllowed;
	}

	public void setLevelCachingAllowed(boolean levelCachingAllowed) {
		this.levelCachingAllowed = levelCachingAllowed;
	}

}
