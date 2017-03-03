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
package org.eclipse.scanning.device.ui;

import java.net.URI;

import org.eclipse.dawnsci.analysis.api.expressions.IExpressionService;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.io.IRemoteDatasetService;
import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.dawnsci.plotting.api.IPlottingService;
import org.eclipse.scanning.api.ISpringParser;
import org.eclipse.scanning.api.IValidatorService;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.scan.IParserService;
import org.eclipse.scanning.api.stashing.IStashingService;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.api.ui.auto.IInterfaceService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;

public class ServiceHolder {

	private static IEventConnectorService eventConnectorService;
	private static IEventService          eventService;
	private static IPointGeneratorService generatorService;
	private static IValidatorService      validatorService;
	private static IExpressionService     expressionService;
	private static ILoaderService         loaderService;
	private static ISpringParser          springParser;
	private static IMarshallerService     marshallerService;
	private static IPlottingService       plottingService;
	private static IRemoteDatasetService  remoteDatasetService;
	private static IParserService         parserService;
	private static IInterfaceService      interfaceService;
	private static IStashingService       stashingService;
	private static EventAdmin             eventAdmin;
	
	private static BundleContext context;

	public static IEventConnectorService getEventConnectorService() {
		if (eventConnectorService==null) eventConnectorService = getService(IEventConnectorService.class);
		return eventConnectorService;
	}

	public void setEventConnectorService(IEventConnectorService eventService) {
		ServiceHolder.eventConnectorService = eventService;
	}

	public static IEventService getEventService() {
		if (eventService==null) eventService = getService(IEventService.class);
		return eventService;
	}

	public static void setEventService(IEventService eventService) {
		ServiceHolder.eventService = eventService;
	}

	public static IPointGeneratorService getGeneratorService() {
		if (generatorService==null) generatorService = getService(IPointGeneratorService.class);
		return generatorService;
	}

	public void setGeneratorService(IPointGeneratorService generatorService) {
		ServiceHolder.generatorService = generatorService;
	}

	public static IExpressionService getExpressionService() {
		if (expressionService==null) expressionService = getService(IExpressionService.class);
		return expressionService;
	}

	public static void setExpressionService(IExpressionService expressionService) {
		ServiceHolder.expressionService = expressionService;
	}

	public static ILoaderService getLoaderService() {
		if (loaderService==null) loaderService = getService(ILoaderService.class);
		return loaderService;
	}

	public void setLoaderService(ILoaderService loaderService) {
		ServiceHolder.loaderService = loaderService;
	}

	public void start(BundleContext context) {
		ServiceHolder.context = context;
	}
	
	private static <T> T getService(Class<T> clazz) {
		if (context == null) return null;
		try {
			ServiceReference<T> ref = context.getServiceReference(clazz);
	        return context.getService(ref);
		} catch (NullPointerException npe) {
			return null;
		}
	}
	
	public static <T> T getRemote(Class<T> clazz) throws Exception {
		return (T)ServiceHolder.getEventService().createRemoteService(new URI(CommandConstants.getScanningBrokerUri()), clazz);
	}

	public static IValidatorService getValidatorService() {
		if (validatorService==null) validatorService = getService(IValidatorService.class);
		return validatorService;
	}

	public static void setValidatorService(IValidatorService validatorService) {
		ServiceHolder.validatorService = validatorService;
	}

	public static ISpringParser getSpringParser() {
		if (springParser==null) springParser = getService(ISpringParser.class);
		return springParser;
	}

	public static void setSpringParser(ISpringParser springParser) {
		ServiceHolder.springParser = springParser;
	}

	public static IMarshallerService getMarshallerService() {
		if (marshallerService==null) marshallerService = getService(IMarshallerService.class);
		return marshallerService;
	}

	public static void setMarshallerService(IMarshallerService marshallerService) {
		ServiceHolder.marshallerService = marshallerService;
	}

	public static IPlottingService getPlottingService() {
		if (plottingService==null) plottingService = getService(IPlottingService.class);
		return plottingService;
	}

	public static void setPlottingService(IPlottingService plottingService) {
		ServiceHolder.plottingService = plottingService;
	}

	public static IRemoteDatasetService getRemoteDatasetService() {
		if (remoteDatasetService==null) remoteDatasetService = getService(IRemoteDatasetService.class);
		return remoteDatasetService;
	}

	public static void setRemoteDatasetService(IRemoteDatasetService remoteDatasetService) {
		ServiceHolder.remoteDatasetService = remoteDatasetService;
	}

	public static EventAdmin getEventAdmin() {
		if (eventAdmin==null) eventAdmin = getService(EventAdmin.class);
		return eventAdmin;
	}

	public static void setEventAdmin(EventAdmin eventAdmin) {
		ServiceHolder.eventAdmin = eventAdmin;
	}

	public static IParserService getParserService() {
		if (parserService==null) parserService = getService(IParserService.class);
		return parserService;
	}

	public static void setParserService(IParserService parserService) {
		ServiceHolder.parserService = parserService;
	}

	public static IInterfaceService getInterfaceService() {
		if (interfaceService==null) interfaceService = getService(IInterfaceService.class);
		return interfaceService;
	}

	public static void setInterfaceService(IInterfaceService interfaceService) {
		ServiceHolder.interfaceService = interfaceService;
	}

	public static IStashingService getStashingService() {
		if (stashingService==null) stashingService = getService(IStashingService.class);
		return stashingService;
	}

	public static void setStashingService(IStashingService stashingService) {
		ServiceHolder.stashingService = stashingService;
	}
}
