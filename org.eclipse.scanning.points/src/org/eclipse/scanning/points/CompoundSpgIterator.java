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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.points.IDeviceDependentIterable;
import org.eclipse.scanning.api.points.IMutator;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.jython.JythonObjectFactory;
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

	private int index = -1;

	public CompoundSpgIterator(CompoundGenerator gen) {
		this.gen       = gen;
		this.iterators = initIterators();
		this.pos       = createFirstPosition();

		// Throw an exception if iterator is device dependent and can't be processed by SPG
		for (Iterator<? extends IPosition>it : this.iterators) {
			if (IDeviceDependentIterable.class.isAssignableFrom(it.getClass())) {
				throw new IllegalArgumentException();
			}
		}

		JythonObjectFactory<ScanPointIterator> compoundGeneratorFactory = ScanPointGeneratorFactory.JCompoundGeneratorFactory();

        Object[] excluders = getExcluders(gen.getModel().getRegions());
        Object[] mutators = getMutators(gen.getModel().getMutators());
        double duration = gen.getModel().getDuration();

        ScanPointIterator iterator = compoundGeneratorFactory.createObject(
				iterators, excluders, mutators, duration);

        index = -1;
        pyIterator = iterator;
	}

	private IPosition createFirstPosition(){
	    IPosition pos = new MapPosition();
		for (int i = 0; i < iterators.length-1; i++) {
			pos = iterators[i].next().compound(pos);
		}
		return pos;
	}

	@Override
    public PyDictionary toDict() {
		return ((PySerializable) pyIterator).toDict();
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

	/**
	 * Creates an array of python objects representing the mutators
	 * @param mutators
	 * @return
	 */
	private Object[] getMutators(Collection<IMutator> mutators) {
		LinkedList<Object> pyMutators = new LinkedList<>();
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
		// regions are grouped into excluders by scan axes covered
		// two regions are in the same excluder iff they have the same axes
		LinkedHashMap<List<String>, List<Object>> excluders = new LinkedHashMap<>();
		JythonObjectFactory<?> excluderFactory = ScanPointGeneratorFactory.JExcluderFactory();
		if (regions != null) {
			for (Object region : regions) {
				if (region instanceof ScanRegion) {
					ScanRegion<?> sr = (ScanRegion<?>) region;
					Optional<List<Object>> excluderOptional = excluders.entrySet().stream()
							.filter(e -> sr.getScannables().containsAll(e.getKey()))
							.map(Map.Entry::getValue)
							.findFirst();
					List<Object> rois = excluderOptional.orElse(new LinkedList<>());
					if (!excluderOptional.isPresent()) {
						excluders.put(sr.getScannables(), rois);
					}
					try {
						Object pyRoi = makePyRoi(region);
						if (pyRoi != null) rois.add(pyRoi);
					} catch (Exception e) {
						logger.error("Could not convert ROI to PyRoi", e);
					}
				} else {
					logger.error("Region wasn't of type ScanRegion");
				}
			}
		}
		List<Object> pyExcluders = excluders.entrySet().stream()
				.filter(e -> !e.getValue().isEmpty())
				.map(e -> excluderFactory.createObject(e.getValue().toArray(), e.getKey()))
				.collect(Collectors.toList());
		return pyExcluders.toArray();
	}

	@Override
	public String toString() {
		return "CompoundSpgIterator [gen=" + gen + ", pos=" + pos + ", iterators=" + Arrays.toString(iterators)
				+ ", index=" + index + "]";
	}
}
