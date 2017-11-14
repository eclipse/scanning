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
package org.eclipse.scanning.points;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PointROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolygonalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractScanPointIterator implements ScanPointIterator, PySerializable {

	public static final PyObject[] EMPTY_PY_ARRAY = new PyObject[0];

	private static Logger logger = LoggerFactory.getLogger(AbstractScanPointIterator.class);

	private static Map<Class<?>, Function<IROI, PyObject>> roiDispatchMap = new HashMap<>();

	protected ScanPointIterator pyIterator;

	private int index = 0;

	public Iterator<IPosition> getPyIterator() {
		return pyIterator;
	}

	public void setPyIterator(ScanPointIterator pyIterator) {
		this.pyIterator = pyIterator;
	}

	static {
		roiDispatchMap.put(CircularROI.class, r -> ScanPointGeneratorFactory.JCircularROIFactory().createObject(
				((CircularROI) r).getCentre(), ((CircularROI) r).getRadius()));
		roiDispatchMap.put(EllipticalROI.class, r -> ScanPointGeneratorFactory.JEllipticalROIFactory().createObject(
				((EllipticalROI) r).getPoint(), ((EllipticalROI) r).getSemiAxes(), ((EllipticalROI) r).getAngle()));
		roiDispatchMap.put(LinearROI.class, r -> null); // not supported
		roiDispatchMap.put(PointROI.class, r -> ScanPointGeneratorFactory.JPointROIFactory().createObject(
				((PointROI) r).getPoint()));
		roiDispatchMap.put(PolygonalROI.class, r -> {
			PolygonalROI p = (PolygonalROI) r;
			double[] xPoints = new double[p.getNumberOfPoints()];
			double[] yPoints = new double[p.getNumberOfPoints()];
			for (int i = 0; i < xPoints.length; i++) {
				PointROI point = p.getPoint(i);
				xPoints[i] = point.getPointX();
				yPoints[i] = point.getPointY();
			}
			return ScanPointGeneratorFactory.JPolygonalROIFactory().createObject(xPoints, yPoints);
		});
		roiDispatchMap.put(RectangularROI.class, r -> ScanPointGeneratorFactory.JRectangularROIFactory().createObject(
				((RectangularROI) r).getPoint(), ((RectangularROI) r).getLength(0), ((RectangularROI) r).getLength(1),
				((RectangularROI) r).getAngle()));
		roiDispatchMap.put(SectorROI.class, r -> ScanPointGeneratorFactory.JSectorROIFactory().createObject(
				((SectorROI) r).getPoint(), ((SectorROI) r).getRadii(), ((SectorROI) r).getAngles()));
	}

	protected static PyObject makePyRoi(Object region) {
		IROI roi = null;
		if (region instanceof ScanRegion<?>) {
			region = ((ScanRegion<?>) region).getRoi();
		}
		if (region instanceof IROI) {
			roi = (IROI) region;
		} else {
			logger.error("Unknown region type: " + region.getClass());
			return null;
		}
		if (roiDispatchMap.containsKey(roi.getClass())) {
			return roiDispatchMap.get(roi.getClass()).apply(roi);
		} else {
			logger.error("Unsupported region type: " + roi.getClass());
			return null;
		}
	}

	@Override
	public PyDictionary toDict() {
		return null;
	}

	@Override
	public boolean hasNext() {
		return pyIterator.hasNext();
	}

	@Override
	public IPosition next() {
		final IPosition position = pyIterator.next();
		position.setStepIndex(index);
		index++;
		return position;
	}

	@Override
	public int size() {
		return pyIterator.size();
	}

	@Override
	public int[] getShape() {
		return pyIterator.getShape();
	}

	@Override
	public int getRank() {
		return pyIterator.getRank();
	}

	@Override
	public int getIndex() {
		return index;
	}

}
