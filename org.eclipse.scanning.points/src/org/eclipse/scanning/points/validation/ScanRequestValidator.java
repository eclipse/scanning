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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.IValidator;
import org.eclipse.scanning.api.IValidatorService;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.IMalcolmModel;
import org.eclipse.scanning.api.device.models.ScanMode;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.scan.ScanningException;

class ScanRequestValidator implements IValidator<ScanRequest<?>> {
	
	
	private IValidatorService vservice;

	@Override
	public void setService(IValidatorService vservice) {
		this.vservice = vservice;
	}
	
	@Override
	public void validate(ScanRequest<?> req) throws ValidationException, InstantiationException, IllegalAccessException {

	    final CompoundModel<?> cm = req.getCompoundModel();
		if (cm!=null && cm.getModels()!=null && !cm.getModels().isEmpty()) {
			vservice.validate(cm);
		} else {
			throw new ModelValidationException("There is no compound model available", req, "compoundModel");
		}
		try {
			Map<String, Object> dmodels = req.getDetectors();
			if (dmodels!=null && !dmodels.isEmpty()) { // No detectors is allowed.
				validateMalcolmRules(dmodels);
				validateDetectors(dmodels);
				validateAnnotations(dmodels);
			}
			
		} catch (ScanningException ne) {
            throw new ValidationException(ne);
		}
	}
	
	private void validateAnnotations(Map<String, Object> dmodels) throws ValidationException, IllegalArgumentException, IllegalAccessException {
		
		for (String name : dmodels.keySet()) {
			
			// The model we will validate
			Object model = dmodels.get(name);
			
			// If the model has an annotated field which points at 
			// a detector, that detector must be in the scan.
			Field[] fields = model.getClass().getDeclaredFields();
			for (Field field : fields) {
				Annotation[] anots = field.getAnnotations();
				for (Annotation annotation : anots) {
					if (annotation instanceof FieldDescriptor) {
						
						FieldDescriptor des = (FieldDescriptor)annotation;
						if (des.device()==DeviceType.RUNNABLE) { // Then its value must be in the devices.
							
							boolean accessible = field.isAccessible();
							try {
								field.setAccessible(true);
								String value = (String)field.get(model);
								if (!dmodels.containsKey(value)) {
									String label = des.label()!=null && des.label().length()>0 ? des.label() : field.getName();
									throw new ModelValidationException("The value of '"+label+"' references a device ("+value+") not in the scan!", model, field.getName());
								}
							} finally {
								field.setAccessible(accessible);
							}
						}
					}
				}
			}
		}
	}

	private void validateMalcolmRules(Map<String, Object> dmodels)  throws ValidationException, ScanningException {
		ScanMode scanMode = dmodels.values().stream().anyMatch(IMalcolmModel.class::isInstance) ?
				ScanMode.HARDWARE : ScanMode.SOFTWARE;

		IRunnableDeviceService dservice = ValidatorService.getRunnableDeviceService();
		if (dservice!=null) {
			Map<DeviceRole, Integer> deviceRoleCount = new HashMap<>();
			for (DeviceRole role : DeviceRole.values()) deviceRoleCount.put(role, 0);
			for (String name : dmodels.keySet()) {
				DeviceRole role = null;
				DeviceInformation<?> info = dservice.getDeviceInformation(name);
				if (info==null) {
					try {
						final IRunnableDevice<?> device = dservice.createRunnableDevice(dmodels.get(name));
						if (device.getRole()==DeviceRole.PROCESSING) {
							role = device.getRole();
						} else {
							// Only processing may be created on the fly, the others must have names.
							throw new ValidationException("Detector '"+name+"' cannot be found!");
						}
					} catch (ScanningException ne) {
						throw ne; // If we cannot make a device with this model, the scan request is not valid.
					}
				} else{
					// devices that can run either as a standard hardware detector or as a hardware
					// triggered detector will be switched to the appropriate role according to the scan type
					if (info != null && !info.getSupportedScanModes().contains(scanMode)) {
						throw new ValidationException(MessageFormat.format("The device ''{0}'' does not support a {1} scan",
								info.getName(), scanMode.toString().toLowerCase()));
					}
					role = info.getDeviceRole();
				}
					
				if (role==null) throw new ValidationException("Detector '"+name+"' cannot be found!");
				Integer c = deviceRoleCount.get(role);
				deviceRoleCount.put(role, ++c);
			}
			if (deviceRoleCount.get(DeviceRole.MALCOLM)>1) {
				throw new ValidationException("Only one malcolm device may be used per scan.");
			}
		}
	}

	private void validateDetectors(Map<String, Object> dmodels) throws ValidationException, InstantiationException, IllegalAccessException {
	
		// All the models must validate too
		for (Object model : dmodels.values()) {
			IValidator<Object> validator = vservice.getValidator(model);
			if (validator!=null) validator.validate(model); // We just ignore those without validators.
		}
		
	}

}
