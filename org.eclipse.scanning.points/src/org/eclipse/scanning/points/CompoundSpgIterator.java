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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PointROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolygonalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.scanning.api.points.AbstractPosition;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IDeviceDependentIterable;
import org.eclipse.scanning.api.points.IMutator;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.points.ScanPointGeneratorFactory.JythonObjectFactory;
import org.python.core.PyDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We are trying to make it super efficient to iterate
 * compound generators by doing this. Otherwise the createPoints(...) 
 * would do.
 * 
 * @author Matthew Gerring
 *
 */
public class CompoundSpgIterator extends AbstractScanPointIterator {

	private static Logger logger = LoggerFactory.getLogger(CompoundSpgIterator.class);
	
	private CompoundGenerator     gen;
	private IPosition             pos;
	private Iterator<? extends IPosition>[] iterators;
	
	public SerializableIterator<IPosition> pyIterator;
	private IPosition currentPoint;
	private int index = -1;

	public CompoundSpgIterator(CompoundGenerator gen) throws GeneratorException {
		this.gen       = gen;
		this.iterators = initIterators();
		this.pos       = createFirstPosition();
		
		// Throw an exception if iterator is device dependent and can't be processed by SPG
		for (Iterator<? extends IPosition>it : this.iterators) {
			if (IDeviceDependentIterable.class.isAssignableFrom(it.getClass())) {
				throw new IllegalArgumentException();
			}
		}
		
		JythonObjectFactory compoundGeneratorFactory = ScanPointGeneratorFactory.JCompoundGeneratorFactory();
		
        Object[] excluders = {}; //getExcluders(gen.getModel().getRegions()); TODO put back in when excluders are fixed in Python
        Object[] mutators = getMutators(gen.getModel().getMutators());
        
        @SuppressWarnings("unchecked")
		SerializableIterator<IPosition> iterator = (SerializableIterator<IPosition>)  compoundGeneratorFactory.createObject(
				iterators, excluders, mutators);
        
        index = -1;
        pyIterator = iterator;
	}

	private IPosition createFirstPosition() throws GeneratorException {
		
	    IPosition pos = new MapPosition();
		for (int i = 0; i < iterators.length-1; i++) {
			pos = iterators[i].next().compound(pos);
		}
		return pos;
	}
	
	@Override
    public PyDictionary toDict() {
		return pyIterator.toDict();
    }
    
	@Override
	public boolean hasNext() {
		// TODO: Commented out until Python ROIs are ready
		IPosition point;
//		double x;
//		double y;
		
		while (pyIterator.hasNext()) {
			point = pyIterator.next();
//			x = point.getX();
//			y = point.getY();
//			
//			if (gen.containsPoint(x, y)) {
			currentPoint = point;
			index++;
			if (currentPoint!=null) currentPoint.setStepIndex(index);
			return true;
//			}
		}
		
		return false;
	}

	@Override
	public IPosition next() {
		// TODO: This will return null if called without calling hasNext() and when the
		// ROI will exclude all further points. Raise error if called without hasNext()
		// first, or if point is null?
		if (currentPoint == null) {
			hasNext();
		}
		IPosition point = currentPoint;
		currentPoint = null;
		
		return point;
	}
	
	public IPosition getNext() {
		
		for (int i = iterators.length-1; i > -1; i--) {
			if (iterators[i].hasNext()) {
				IPosition next = iterators[i].next();
				pos = next.compound(pos);
				((AbstractPosition)pos).setDimensionNames(gen.getDimensionNames());
				return pos;
			} else if (i>0) {
				iterators[i]    = gen.getGenerators()[i].iterator();
				IPosition first = iterators[i].next();
				pos = first.compound(pos);
				((AbstractPosition)pos).setDimensionNames(gen.getDimensionNames());
			}
		}
		return null;
	}


	private Iterator<? extends IPosition>[] initIterators() {
		final IPointGenerator<?>[] gs = gen.getGenerators();
		@SuppressWarnings("unchecked")
		Iterator<? extends IPosition>[] ret = new Iterator[gs.length];
		for (int i = 0; i < gs.length; i++) {
			ret[i] = gs[i].iterator();
		}
		return ret;
	}

	public void remove() {
        throw new UnsupportedOperationException("remove");
    }

	public int size() {
		return pyIterator.size();
	}
	
	/**
	 * Creates an array of python objects representing the mutators
	 * @param mutators
	 * @return
	 */
	private Object[] getMutators(Collection<IMutator> mutators) {
		LinkedList<Object> pyMutators = new LinkedList<Object>();
		if (mutators != null) {
			for (IMutator mutator : mutators) {
				pyMutators.add(mutator.getMutatorAsJythonObject());
			}
		}
		return pyMutators.toArray();
	}
	
	/**
	 * Creates an array of python objects representing the excluders
	 * @param regions
	 * @return
	 */
	public static Object[] getExcluders(Collection<?> regions) {
		LinkedList<Object> pyRegions = new LinkedList<Object>();
		JythonObjectFactory excluderFactory = ScanPointGeneratorFactory.JExcluderFactory();
		JythonObjectFactory circularROIFactory = ScanPointGeneratorFactory.JCircularROIFactory();
		JythonObjectFactory ellipticalROIFactory = ScanPointGeneratorFactory.JEllipticalROIFactory();
		JythonObjectFactory pointROIFactory = ScanPointGeneratorFactory.JPointROIFactory();
		JythonObjectFactory polygonalROIFactory = ScanPointGeneratorFactory.JPolygonalROIFactory();
		JythonObjectFactory rectangularROIFactory = ScanPointGeneratorFactory.JRectangularROIFactory();
		JythonObjectFactory sectorROIFactory = ScanPointGeneratorFactory.JSectorROIFactory();
		
		if (regions != null) {
			for (Object region : regions) {
				
				if (region instanceof ScanRegion) {
					ScanRegion<?> sr = (ScanRegion<?>)region;
					Object roi = sr.getRoi();
					Object pyRoi = null;
					
					if (roi instanceof CircularROI) {
						CircularROI cRoi = (CircularROI) roi;
						pyRoi = circularROIFactory.createObject(cRoi.getCentre(), cRoi.getRadius());
					} else if (roi instanceof EllipticalROI) {
						EllipticalROI eRoi = (EllipticalROI) roi;
						pyRoi = ellipticalROIFactory.createObject(eRoi.getPoint(), eRoi.getSemiAxes(), eRoi.getAngle());
					} else if (roi instanceof LinearROI) {
						// LinearROIs are not supported so do not add
					} else if (roi instanceof PointROI) {
						PointROI pRoi = (PointROI) roi;
						pyRoi = pointROIFactory.createObject(pRoi.getPoint());
					} else if (roi instanceof PolygonalROI) {
						PolygonalROI pRoi = (PolygonalROI) roi;
						double[] xPoints = new double[pRoi.getNumberOfPoints()];
						double[] yPoints = new double[pRoi.getNumberOfPoints()];
						for (int i = 0; i < pRoi.getNumberOfPoints(); i++) {
							PointROI pointRoi = pRoi.getPoint(i);
							xPoints[i] = pointRoi.getPointX();
							yPoints[i] = pointRoi.getPointY();
						}
						pyRoi = polygonalROIFactory.createObject(xPoints, yPoints);
					} else if (roi instanceof RectangularROI) {
						RectangularROI rRoi = (RectangularROI) roi;
						pyRoi = rectangularROIFactory.createObject(rRoi.getPoint(), rRoi.getLength(0), rRoi.getLength(1), rRoi.getAngle());
					} else if (roi instanceof SectorROI) {
						SectorROI sRoi = (SectorROI) roi;
						pyRoi = sectorROIFactory.createObject(sRoi.getPoint(), sRoi.getRadii(), sRoi.getAngles());
					} else {
						logger.error("Unsupported ROI tyoe: " + roi.getClass());
					}
					if (pyRoi != null) {
						Object pyExcluder = excluderFactory.createObject(pyRoi, sr.getScannables());
						pyRegions.add(pyExcluder);
					}
				} else {
					logger.error("Region wasn't of type ScanRegion");
				}
			}
		}
		return pyRegions.toArray();
	}

}
