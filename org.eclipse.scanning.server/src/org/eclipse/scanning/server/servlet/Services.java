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
package org.eclipse.scanning.server.servlet;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.IValidatorService;
import org.eclipse.scanning.api.device.IDeviceWatchdogService;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.IMessagingService;
import org.eclipse.scanning.api.malcolm.IMalcolmService;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.scan.IFilePathService;
import org.eclipse.scanning.api.scan.process.IPreprocessor;
import org.eclipse.scanning.api.script.IScriptService;


/**
 * This class holds services for the scanning server servlets. Services should be configured to be optional and dynamic
 * and will then be injected correctly by Equinox DS.
 *
 * @author Matthew Gerring
 * @author Colin Palmer
 *
 */
public class Services {

	private static IEventService           eventService;
	private static IPointGeneratorService  generatorService;
	private static IRunnableDeviceService  runnableDeviceService;
	private static IScannableDeviceService connector;
	private static IMalcolmService         malcService;
	private static IFilePathService        filePathService;
	private static IScriptService          scriptService;
	private static IMessagingService       messagingService;
	private static IValidatorService       validatorService;
	private static IDeviceWatchdogService  watchdogService;

	private static final Set<IPreprocessor> preprocessors = new LinkedHashSet<>();

	public static IFilePathService getFilePathService() {
		return filePathService;
	}

	public static void setFilePathService(IFilePathService filePathService) {
		Services.filePathService = filePathService;
	}

	public static IEventService getEventService() {
		return eventService;
	}

	public static void setEventService(IEventService eventService) {
		Services.eventService = eventService;
	}

	public static IPointGeneratorService getGeneratorService() {
		return generatorService;
	}

	public static void setGeneratorService(IPointGeneratorService generatorService) {
		Services.generatorService = generatorService;
	}

	public static IRunnableDeviceService getRunnableDeviceService() {
		return runnableDeviceService;
	}

	public static void setRunnableDeviceService(IRunnableDeviceService deviceService) {
		Services.runnableDeviceService = deviceService;
	}

	public static IScannableDeviceService getConnector() {
		return connector;
	}

	public static void setConnector(IScannableDeviceService connector) {
		Services.connector = connector;
	}

	public static IMalcolmService getMalcService() {
		return malcService;
	}

	public static void setMalcService(IMalcolmService malcService) {
		Services.malcService = malcService;
	}

	public static IScriptService getScriptService() {
		return scriptService;
	}

	public static void setScriptService(IScriptService scriptService) {
		Services.scriptService = scriptService;
	}

	public static synchronized void addPreprocessor(IPreprocessor preprocessor) {
		preprocessors.add(preprocessor);
	}

	public static synchronized void removePreprocessor(IPreprocessor preprocessor) {
		preprocessors.remove(preprocessor);
	}

	public static Collection<IPreprocessor> getPreprocessors() {
		return preprocessors;
	}

	public static IMessagingService getMessagingService() {
		return messagingService;
	}

	public static void setMessagingService(IMessagingService messagingService) {
		Services.messagingService = messagingService;
	}

	public static IValidatorService getValidatorService() {
		return validatorService;
	}

	public static void setValidatorService(IValidatorService validatorService) {
		Services.validatorService = validatorService;
	}

	public static IDeviceWatchdogService getWatchdogService() {
		return watchdogService;
	}

	public static void setWatchdogService(IDeviceWatchdogService watchdogService) {
		Services.watchdogService = watchdogService;
	}
}
