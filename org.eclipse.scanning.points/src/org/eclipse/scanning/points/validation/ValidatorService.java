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
package org.eclipse.scanning.points.validation;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.IValidator;
import org.eclipse.scanning.api.IValidatorService;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorService implements IValidatorService {
	
	private static Logger logger = LoggerFactory.getLogger(ValidatorService.class);
	
	static {
		System.out.println("Starting ValidatorService");
	}
	
	private static IPointGeneratorService factory;
	public void setPointGeneratorService(IPointGeneratorService pservice) {
		factory = pservice;
	}
	
	private static IRunnableDeviceService dservice;
	private static IEventService          eservice;
	
	public void setEventService(IEventService leservice) {
		eservice = leservice;
	}
	
	private static ComponentContext context;
	
	public static IPointGeneratorService getPointGeneratorService() {
		return factory;
	}
	public static IRunnableDeviceService getRunnableDeviceService() {
		// On the server we have a direct IRunnableDeviceService available.
		// On the client we must use a remote one.
		// Since remote one works on server, we always use it.
		if (dservice==null) {
			try {
				dservice = eservice.createRemoteService(new URI(CommandConstants.getScanningBrokerUri()), IRunnableDeviceService.class);
			} catch (EventException | URISyntaxException e) {
				ServiceReference<IRunnableDeviceService> ref = context.getBundleContext().getServiceReference(IRunnableDeviceService.class);
				dservice = context.getBundleContext().getService(ref);
			}
		}
		return dservice;
	}
	
	public void setRunnableDeviceService(IRunnableDeviceService service) {
		dservice = service;
	}
	
	public void start(ComponentContext lcontext) {
		context = lcontext;
	}


	@SuppressWarnings("rawtypes")
	private static final Map<Class<?>, Class<? extends IValidator>> validators;
	static {
		@SuppressWarnings("rawtypes")
		Map<Class<?>, Class<? extends IValidator>> tmp = new HashMap<>();
		tmp.put(BoundingBox.class,   BoundingBoxValidator.class);
		tmp.put(CompoundModel.class, CompoundValidator.class);
		tmp.put(ScanRequest.class,   ScanRequestValidator.class);
		
		validators = Collections.unmodifiableMap(tmp);
	}

	@Override
	public <T> void validate(T model) throws ValidationException, InstantiationException, IllegalAccessException {
		
		if (model==null) throw new ValidationException("The object to validate is null and cannot be checked!");
		IValidator<T> validator = getValidator(model);
		if (validator==null) throw new IllegalAccessException("There is no validator for "+model.getClass().getSimpleName());
		validator.setService(this);
		validator.validate(model);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IValidator<T> getValidator(T model) throws InstantiationException, IllegalAccessException {
		
		if (model==null) throw new NullPointerException("The model is null!");
		
		if (model instanceof IValidator) throw new IllegalArgumentException("Models should be vanilla and not contain logic for validating themselves!");
		
		if (validators.containsKey(model.getClass())) {
			return validators.get(model.getClass()).newInstance();
		}
	
		if (model instanceof IScanPathModel) { // Ask a generator
			try {
				IScanPathModel     pmodel = (IScanPathModel)model;
				IPointGenerator<?> gen    = factory.createGenerator(pmodel);
				return (IValidator<T>)gen;
				
			} catch (GeneratorException e) {
				throw new IllegalAccessException(e.getMessage());
			}
		} else {
			try {
				IPointGenerator<?> gen    = factory.createGenerator(model);
				return (IValidator<T>)gen;
			} catch (Exception legallyPossible) {
				// Do nothing
			}
		}
		
		return getDeviceFromModel(model);
	}
	
	private <T> IRunnableDevice<T> getDeviceFromModel(T model) {
		
		if (dservice==null) return null;

		IRunnableDevice<T> device = null;
		if (model instanceof INameable) {
			try {
				final String deviceName = ((INameable) model).getName();
				device = dservice.getRunnableDevice(deviceName);
				if (device == null) {
					device = dservice.createRunnableDevice(model, false);
				}
			} catch (ScanningException e) {
				logger.trace("No device found for "+model, e);
			}
		} else {
			try {
				Method getName = model.getClass().getMethod("getName");
				String name = (String)getName.invoke(model);
				device = dservice.getRunnableDevice(name);

			} catch (Exception ne) {
				try { 
					device  = dservice.createRunnableDevice(model, false);
				} catch (Exception legallyPossible) {
					// Do nothing, if we cannot get it, then we cannot get it.
				}
			}

		}

		return device;
	}

}
