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

import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;
import org.eclipse.dawnsci.nexus.builder.NexusBuilderFactory;
import org.eclipse.scanning.api.device.IDeviceWatchdogService;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.scan.IFilePathService;
import org.eclipse.scanning.api.scan.IParserService;

public class ServiceHolder {
	
	// OSGi stuff
	private static NexusBuilderFactory factory;

	public static NexusBuilderFactory getFactory() {
		return factory;
	}

	public void setFactory(NexusBuilderFactory factory) {
		ServiceHolder.factory = factory;
	}

    private static IOperationService operationService;

	public static IOperationService getOperationService() {
		return operationService;
	}

	public void setOperationService(IOperationService operationService) {
		ServiceHolder.operationService = operationService;
	}
	
    private static IDeviceWatchdogService watchdogService;

	public static IDeviceWatchdogService getWatchdogService() {
		return watchdogService;
	}

	public static void setWatchdogService(IDeviceWatchdogService watchdogService) {
		ServiceHolder.watchdogService = watchdogService;
	}

    
	private static IPersistenceService persistenceService;

	public static IPersistenceService getPersistenceService() {
		return persistenceService;
	}

	public void setPersistenceService(IPersistenceService persistenceService) {
		ServiceHolder.persistenceService = persistenceService;
	}
	
	private static ILoaderService loaderService;

	public static ILoaderService getLoaderService() {
		return loaderService;
	}

	public void setLoaderService(ILoaderService loaderService) {
		ServiceHolder.loaderService = loaderService;
	}

	private static IEventService eventService;
	
	public static IEventService getEventService() {
		return eventService;
	}
	
	public static void setEventService(IEventService eventService) {
		ServiceHolder.eventService = eventService;
	}
	
	private static IFilePathService filePathService;

	public static IFilePathService getFilePathService() {
		return filePathService;
	}
	
	public void setFilePathService(IFilePathService filePathService) {
		ServiceHolder.filePathService = filePathService;
	}
	
	private static IRunnableDeviceService runnableDeviceService;
	
	public static IRunnableDeviceService getRunnableDeviceService() {
		return runnableDeviceService;
	}
	
	public static void setRunnableDeviceService(IRunnableDeviceService runnableDeviceService) {
		ServiceHolder.runnableDeviceService = runnableDeviceService;
	}
	
	private static IPointGeneratorService generatorService;

	
	public static IPointGeneratorService getGeneratorService() {
		return generatorService;
	}

	public void setGeneratorService(IPointGeneratorService generatorService) {
		ServiceHolder.generatorService = generatorService;
	}
	
	private static IParserService parserService;

	public static IParserService getParserService() {
		return parserService;
	}

	public void setParserService(IParserService parserService) {
		ServiceHolder.parserService = parserService;
	}
	
	private static IMarshallerService marshallerService;
	

	public static IMarshallerService getMarshallerService() {
		return marshallerService;
	}

	public void setMarshallerService(IMarshallerService marshallerService) {
		ServiceHolder.marshallerService = marshallerService;
	}

	/**
	 * Used to provide services when tests running in non-OSGi mode.
	 */
	@SuppressWarnings("javadoc")
	public static void setTestServices(ILoaderService ls,
			NexusBuilderFactory defaultNexusBuilderFactory, IOperationService oservice) {
		loaderService = ls;
		factory = defaultNexusBuilderFactory;
		operationService = oservice;
	}
	
	/**
	 * Used to provide services when tests running in non-OSGi mode.
	 */
	@SuppressWarnings("javadoc")
	public static void setTestServices(ILoaderService ls,
			NexusBuilderFactory defaultNexusBuilderFactory, IOperationService oservice,
			IFilePathService fpservice, IPointGeneratorService gService) {
		loaderService = ls;
		factory = defaultNexusBuilderFactory;
		operationService = oservice;
		filePathService = fpservice; 
		generatorService = gService;
	}

}
