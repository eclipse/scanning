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

import java.io.File;
import java.util.Collections;

import org.eclipse.scanning.api.annotation.scan.AnnotationManager;
import org.eclipse.scanning.api.annotation.scan.PostConfigure;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.MalcolmModel;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IResponseProcess;
import org.eclipse.scanning.api.event.scan.AcquireRequest;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.StaticModel;
import org.eclipse.scanning.api.scan.IFilePathService;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.server.application.Activator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcquireRequestHandler implements IResponseProcess<AcquireRequest> {

	private static final Logger logger = LoggerFactory.getLogger(AcquireRequestHandler.class);

	private final AcquireRequest bean;
	private final IPublisher<AcquireRequest> publisher;

	public AcquireRequestHandler(AcquireRequest bean, IPublisher<AcquireRequest> publisher) {
		this.bean = bean;
		this.publisher = publisher;
	}

	@Override
	public AcquireRequest getBean() {
		return bean;
	}

	@Override
	public IPublisher<AcquireRequest> getPublisher() {
		return publisher;
	}

	@Override
	public AcquireRequest process(AcquireRequest bean) throws EventException {
		try {
			bean.setStatus(Status.RUNNING);
			IRunnableDevice<?> device = createRunnableDevice(bean);
			device.run(null);

			bean.setStatus(Status.COMPLETE);
			bean.setMessage(null);
		} catch (Exception e) {
			bean.setStatus(Status.FAILED);
			bean.setMessage(e.getMessage());
			logger.error("Cannot acquire data for detector " + getBean().getDetectorName(), e);
			throw (e instanceof EventException) ? (EventException)e : new EventException(e);
		}

		return bean;
	}

	private IRunnableDevice<?> createRunnableDevice(AcquireRequest request) throws EventException {
		// get the services we need
		final IRunnableDeviceService deviceService = Services.getRunnableDeviceService();
		final IPointGeneratorService pointGenService = Services.getGeneratorService();

		final ScanModel scanModel = new ScanModel();

		try {
			IPointGenerator<?> gen = pointGenService.createGenerator(new StaticModel());
			scanModel.setPositionIterable(gen);

			scanModel.setFilePath(getOutputFilePath(request));
			IRunnableDevice<?> detector = deviceService.getRunnableDevice(bean.getDetectorName());
			scanModel.setDetectors(detector);
			scanModel.setScannables(Collections.emptyList());
			initializeMalcolmDevice(request, gen);

			configureDetector(detector, request.getDetectorModel(), scanModel, gen);
			return deviceService.createRunnableDevice(scanModel, null);
		} catch (EventException e) {
			throw e;
		} catch (Exception e) {
			throw new EventException(e);
		}
	}

	/**
	 * Initialise the malcolm device with the point generator and the malcolm model
	 * with its output directory. This needs to be done before validation as these values
	 * are sent to the actual malcolm device over the connection for validation.
	 * @param gen
	 * @throws ScanningException
	 */
	private void initializeMalcolmDevice(AcquireRequest req, IPointGenerator<?> gen) throws ScanningException {

		// check for a malcolm device, if one is found, set its output dir on the model
		// and point generator on the malcolm device itself
		if (bean.getFilePath() == null) return;

		if (!(req.getDetectorModel() instanceof MalcolmModel)) return;
		final MalcolmModel malcolmModel = (MalcolmModel) req.getDetectorModel();

		// Set the malcolm output directory. This is new dir in the same parent dir as the
		// scan file and with the same name as the scan file (minus the file extension)
		final File scanFile = new File(bean.getFilePath());
		final File scanDir = scanFile.getParentFile();
		String scanFileNameNoExtn = scanFile.getName();
		final int dotIndex = scanFileNameNoExtn.indexOf('.');
		if (dotIndex != -1) {
			scanFileNameNoExtn = scanFileNameNoExtn.substring(0, dotIndex);
		}
		final File malcolmOutputDir = new File(scanDir, scanFileNameNoExtn);
		malcolmOutputDir.mkdir(); // create the new malcolm output directory for the scan
		malcolmModel.setFileDir(malcolmOutputDir.toString());
		logger.debug("Device {} set malcolm output dir to {}", malcolmModel.getName(), malcolmOutputDir);

		// Set the point generator for the malcolm device
		// We must set it explicitly here because validation checks for a generator and will fail.
		final IRunnableDeviceService service = Services.getRunnableDeviceService();
		IRunnableDevice<?> malcolmDevice = service.getRunnableDevice(malcolmModel.getName());
		((IMalcolmDevice<?>) malcolmDevice).setPointGenerator(gen);
		logger.debug("Malcolm device(s) initialized");
	}

	private String getOutputFilePath(AcquireRequest request) throws EventException {
		if (request.getFilePath() == null) {
			IFilePathService filePathService = Services.getFilePathService();
			try {
				request.setFilePath(filePathService.getNextPath(request.getDetectorName() + "-acquire"));
			} catch (Exception e) {
				throw new EventException(e);
			}
		}

		return request.getFilePath();
	}

	@SuppressWarnings("unchecked")
	private void configureDetector(IRunnableDevice<?> detector, Object detectorModel,
			ScanModel scanModel, IPointGenerator<?> gen) throws Exception {

		ScanInformation info = new ScanInformation();
		info.setRank(0);
		info.setShape(new int[0]);
		info.setSize(1);

		info.setFilePath(scanModel.getFilePath());

		AnnotationManager manager = new AnnotationManager(Activator.createResolver());
		manager.addContext(info);
		manager.addDevices(detector);

		manager.invoke(PreConfigure.class, detectorModel, gen);
		((IRunnableDevice<Object>) detector).configure(detectorModel);
		manager.invoke(PostConfigure.class, detectorModel, gen);
	}

}
