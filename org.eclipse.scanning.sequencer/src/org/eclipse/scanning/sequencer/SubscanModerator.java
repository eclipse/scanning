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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.StaticModel;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * 
 * This class takes a position iterator and it is a compound generator,
 * attempts to remove the inner scans which subscan devices such as Malcolm will take care of
 * from the compound generator and return the outer scans.
 * 
 * 
 * 
 * @author Matthew Gerring
 *
 */
public class SubscanModerator {
	
	private Iterable<IPosition>    outerIterable;
	private Iterable<IPosition>    innerIterable;
	private IPointGeneratorService gservice;
	private CompoundModel<?>       compoundModel;

	public SubscanModerator(Iterable<IPosition> generator, List<IRunnableDevice<?>> detectors, IPointGeneratorService gservice) throws ScanningException {
		this(generator, null, detectors, gservice);
	}

	public SubscanModerator(Iterable<IPosition> generator, CompoundModel<?> cmodel, List<IRunnableDevice<?>> detectors, IPointGeneratorService gservice) throws ScanningException {
		this.gservice = gservice;
		this.compoundModel   = cmodel!=null ? cmodel : getModel(generator);
		try {
			moderate(generator, detectors);
		} catch (MalcolmDeviceException | GeneratorException e) {
			throw new ScanningException("Unable to moderate scan for malcolm devices!", e);
		}
	}
	
	private CompoundModel<?> getModel(Iterable<IPosition> it) {
		if (it instanceof IPointGenerator<?>) {
			IPointGenerator<?> gen = (IPointGenerator<?>)it;
			Object model = gen.getModel();
			return model instanceof CompoundModel ? (CompoundModel<?>)model : null;
		}
		return null;
	}

	private List<Object> outer;
	private List<Object> inner;

	private void moderate(Iterable<IPosition> generator, List<IRunnableDevice<?>> detectors) throws GeneratorException, ScanningException {
		
		outerIterable = generator; // We will reassign it to the outer scan if there is one, otherwise it is the full scan.
		if (detectors==null || detectors.isEmpty()) {
			return;
		}
		boolean malcolmDevicesFound = false;
		for (IRunnableDevice<?> device : detectors) {
			if (device.getRole()==DeviceRole.MALCOLM) {
				malcolmDevicesFound = true;
				break;
			}
		}
		if (!malcolmDevicesFound) {
			return;
		}
		
		if (!(generator instanceof IPointGenerator<?>)) {
			return;
		}
		
		List<String> axes = getAxes(detectors);
		if (axes.isEmpty()) {
			return;
		}
		
		// We need a compound model to moderate this stuff
		List<Object> orig   = compoundModel.getModels();
		if (orig.isEmpty()) throw new ScanningException("No models are provided in the compound model!");
		
		this.outer = new ArrayList<>();
		this.inner = new ArrayList<>();
		
		boolean reachedOuterScan = false;
		for (int i = orig.size()-1; i > -1; i--) {
			Object model = orig.get(i);
			if (!reachedOuterScan) {
				IPointGenerator<?> g = gservice.createGenerator(model);
				IPosition first = g.iterator().next();
				List<String> names = first.getNames();
				if (axes.containsAll(names)) {// These will be deal with by malcolm
					inner.add(0, model);
					continue; // The device will deal with it.
				}
			}
			reachedOuterScan = true; // As soon as we reach one outer scan all above are outer.
			outer.add(0, model);
		}
		
		if (!inner.isEmpty()) {
			this.innerIterable = gservice.createCompoundGenerator(compoundModel.clone(inner));
		}
		
		if (outer.isEmpty()) {
			this.outerIterable = gservice.createGenerator(new StaticModel(1));
			return;
		}
		
		this.outerIterable = gservice.createCompoundGenerator(compoundModel.clone(outer));
	}

	private List<String> getAxes(List<IRunnableDevice<?>> detectors) throws ScanningException {
		List<String> ret = new ArrayList<>();
		for (IRunnableDevice<?> device : detectors) {
			// TODO Deal with the axes of other subscan devices as they arise.
			if (device instanceof IMalcolmDevice) {
				IMalcolmDevice<?> mdevice = (IMalcolmDevice<?>)device;
				String[] axes = mdevice.getAttributeValue("axesToMove");
				if (axes!=null) ret.addAll(Arrays.asList(axes));
			}
		}
		
		return ret;
	}

	/**
	 * The outer iterable will not be null normally. Even if 
	 * all of the scan is deal with by malcolm the outer scan will still
	 * be a static generator of one point. If there are no subscan devices,
	 * then the outer scan is the full scan.
	 * @return
	 */
	public Iterable<IPosition> getOuterIterable() {
		return outerIterable;
	}
	public Iterable<IPosition> getInnerIterable() {
		return innerIterable;
	}
	public List<Object> getOuterModels() {
		return outer;
	}
	public List<Object> getInnerModels() {
		return inner;
	}

}
