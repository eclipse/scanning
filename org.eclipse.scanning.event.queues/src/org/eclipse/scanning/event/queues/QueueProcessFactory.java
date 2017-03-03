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
package org.eclipse.scanning.event.queues;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.IQueueProcess;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.event.queues.processes.MoveAtomProcess;
import org.eclipse.scanning.event.queues.processes.ScanAtomProcess;
import org.eclipse.scanning.event.queues.processes.SubTaskAtomProcess;
import org.eclipse.scanning.event.queues.processes.TaskBeanProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Michael Wharmby
 *
 */
/*
 * The use of raw types instead of declared generics is not ideal, but since 
 * the T parameter is passed in from IConsumerProcess and the Q parameter is 
 * passed in from IQueueProcess, there isn't any other way to do it. 
 * Furthermore since all the classes only require one type to be passed in (T),
 * using IQueueProcess<?> throughout leads to errors (? should be Q). I found 
 * that using IConsumerProcess<?> also doesn't work.
 * 
 * MTW - 14.01.2016
 */
public class QueueProcessFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(QueueProcessFactory.class);
	
	@SuppressWarnings("rawtypes")
	private static final Map<String, Class<? extends IQueueProcess>> PROCESSES;
	
	static {
		PROCESSES = new HashMap<>();
		initialize();
	}
	
	/**
	 * No argument constructor for OSGi.
	 * As this is a static class there is no need to use it.
	 */
	public QueueProcessFactory() {
		
	}
	
	/**
	 * This registers the default processors within the map. It removes any 
	 * entries in the map before doing this (useful for tests).
	 */
	public static void initialize() {
		if (PROCESSES.size() > 0) {
			PROCESSES.clear();
		}
		
		//Register default processors
		try {
			QueueProcessFactory.registerProcesses(MoveAtomProcess.class,
					ScanAtomProcess.class, SubTaskAtomProcess.class, 
					TaskBeanProcess.class);// MonitorAtomProcessor.class,
		} catch (EventException evEx) {
			logger.error("Initial configuration of QueueProcessorFactory failed. Could not register processor(s): "+evEx.getMessage());
		}
	}
	
	/**
	 * Supply a series of processors to be registered with the factory.
	 * 
	 * @param clazzez Two or more processors to be registered.
	 * @throws EventException Registering processor is unsuccessful.
	 */
	@SuppressWarnings("rawtypes")
	@SafeVarargs
	public static void registerProcesses(Class<? extends IQueueProcess>... clazzez) throws EventException {
		for (Class<? extends IQueueProcess> clazz : clazzez) {
			registerProcess(clazz);
		}
	}
	
	/**
	 * Adds a given processor to the PROCESSORS map, with the key set as the 
	 * class name of the bean which is processes.
	 * 
	 * @param clazz
	 * @throws EventException If getting the String name of the bean or adding 
	 *                        the processor to the map fails.
	 */
	@SuppressWarnings("rawtypes")
	public static void registerProcess(Class<? extends IQueueProcess> clazz) throws EventException {
		try {
			//public static final variable BEAN_CLASS_NAME must be set to <queueBean>.class.getName() in each processor
			String beanClassName = (String) clazz.getDeclaredField("BEAN_CLASS_NAME").get(null);
			PROCESSES.put(beanClassName, clazz);
		} catch (Exception ex) {
			logger.error("Failed to register processor '"+clazz.getName()+"': \""+ex.getMessage()+"\".");
			throw new EventException("Failed to register processor", ex);
		}
		
	}
	
	/**
	 * Return the registry of processes.
	 * 
	 * @return Map<String, Class> of processors and with bean class names as 
	 *         keys.
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, Class<? extends IQueueProcess>> getProcessors() {
		return PROCESSES;
	}
	
	/**
	 * Return a new instance of a processor capable of processing the supplied 
	 * bean type.
	 * 
	 * @param bean extending {@link Queueable} to be processed.
	 * @param publisher status {@link IPublisher} to publish updates through.
	 * @param blocking boolean indicating whether process will stop subsequent 
	 *        queue processes starting.
	 * @return IQueueProcessor instance of class capable of processing bean.
	 * @throws EventException If no processor registered for bean type or if 
	 *                        processor cannot be instantiated.
	 */
	@SuppressWarnings("rawtypes")
	public static <T extends Queueable> IQueueProcess getProcessor(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		Class<? extends IQueueProcess> clazz = PROCESSES.get(bean.getClass().getName());
		
		if (clazz == null) throw new EventException("No processor registered for bean type '"+bean.getClass().getName()+"'");
		try {
			return clazz.getConstructor(Queueable.class, IPublisher.class, Boolean.class)
					.newInstance(bean, publisher, blocking);
		} catch (Exception ex) {
			logger.error("Could not create new instance of class '"+clazz.getName()+"'.");
			throw new EventException("Could not create new instance of processor class", ex);
		}
	}

}
