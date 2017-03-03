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

package org.eclipse.scanning.device.ui.vis;


import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.scanning.api.IValidatorService;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.IBoundingBoxModel;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.swt.widgets.Display;

class PathInfoCalculatorJob extends Job {

	static final int MAX_POINTS_IN_ROI = 100000; // 100,000

	// Services
	private IPointGeneratorService pointGeneratorFactory;
	private IValidatorService      vservice;

	// Model
	private Object                   scanPathModel;
	private List<ScanRegion<IROI>>   scanRegions;

	// Controller
	private PlottingController controller;


	public PathInfoCalculatorJob(final PlottingController controller) {
		super("Calculating scan path");
		this.controller = controller;
		setSystem(true);
		setUser(false);
		setPriority(Job.INTERACTIVE);
		this.pointGeneratorFactory = ServiceHolder.getGeneratorService();
		this.vservice              = ServiceHolder.getValidatorService();
	}

	protected void schedule(Object model, List<ScanRegion<IROI>> scanRegions) {
		this.scanPathModel = model;
		this.scanRegions   = scanRegions;
		schedule();
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		
		if (scanPathModel==null) return Status.CANCEL_STATUS;
		if (scanRegions==null || scanRegions.isEmpty())    {
			setPathVisible(false);
			return Status.CANCEL_STATUS;
		}
		
		{ // Put trace out of scope, drawing the line should not depend on it.
			final IImageTrace trace = controller.getImageTrace();
			if (trace!=null) {
				if (!trace.hasTrueAxes()) throw new IllegalArgumentException(getClass().getSimpleName()+" should act on true axis images!");
			}
		}
		
		monitor.beginTask("Calculating points for scan path", IProgressMonitor.UNKNOWN);
		
		PathInfo pathInfo = new PathInfo();
		if (!(scanPathModel instanceof IBoundingBoxModel)) {
			setPathVisible(false);
			return Status.CANCEL_STATUS;// No path to draw.
		}
		IBoundingBoxModel boxModel = (IBoundingBoxModel) scanPathModel;
		String xAxisName = boxModel.getFastAxisName();
		String yAxisName = boxModel.getSlowAxisName();
		try {
			vservice.validate(scanPathModel); // Throws exception if invalid.
			
			final Collection<IROI> rois = pointGeneratorFactory.findRegions(scanPathModel, scanRegions); // Out of the regions defined finds in the ones for this model.
			if (rois==null || rois.isEmpty()) {
				setPathVisible(false);
				return Status.CANCEL_STATUS;// No path to draw.
			}
			
			final Iterable<IPosition> pointIterable = pointGeneratorFactory.createGenerator(scanPathModel, rois);
			double lastX = Double.NaN;
			double lastY = Double.NaN;
			for (IPosition point : pointIterable) {
				
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				pathInfo.pointCount++;
				
				double[] pnt = new double[]{point.getValue(xAxisName), point.getValue(yAxisName)};
				//pnt = controller.getPointInImageCoordinates(pnt);

				if (pathInfo.pointCount > 1) {
					double thisXStep = Math.abs(pnt[0] - lastX);
					double thisYStep = Math.abs(pnt[1] - lastY);
					double thisAbsStep = Math.sqrt(Math.pow(thisXStep, 2) + Math.pow(thisYStep, 2));
					if (thisXStep > 0) {
						pathInfo.smallestXStep = Math.min(pathInfo.smallestXStep, thisXStep);
					}
					if (thisYStep > 0) {
						pathInfo.smallestYStep = Math.min(pathInfo.smallestYStep, thisYStep);
					}
					pathInfo.smallestAbsStep = Math.min(pathInfo.smallestAbsStep, thisAbsStep);
				}

				lastX = pnt[0];
				lastY = pnt[1];
				if (pathInfo.size() <= MAX_POINTS_IN_ROI) {
					pathInfo.add(Double.valueOf(lastX), Double.valueOf(lastY));
				}
			}
			monitor.done();
			
			// Update the plot, waiting until it has suceeded before
			// returning and allowing this job to run again.
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					controller.plot(pathInfo);
				}
			});
			
		} catch (ModelValidationException mve) {
			return Status.CANCEL_STATUS;
			
		} catch (Exception e) {
			return new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Error calculating scan path", e);
		}
		return Status.OK_STATUS;
	}

	private void setPathVisible(boolean b) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				controller.setPathVisible(b);
			}
		});
	}

}