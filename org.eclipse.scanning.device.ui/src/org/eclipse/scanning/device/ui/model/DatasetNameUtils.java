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
package org.eclipse.scanning.device.ui.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.dawnsci.analysis.api.conversion.IConversionContext.ConversionScheme;
import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.january.metadata.IMetadata;
import org.eclipse.scanning.device.ui.ServiceHolder;


class DatasetNameUtils {

	
	public static Map<String, int[]> getDatasetInfo(String path, ConversionScheme scheme) {
		IMetadata meta;
		final Map<String, int[]>     names  = new HashMap<String, int[]>();
		try {
			meta = ServiceHolder.getLoaderService().getMetadata(path, null);
		} catch (Exception e) {
			return names;
		}
        
        if (meta!=null && !meta.getDataNames().isEmpty()){
        	for (String name : meta.getDataShapes().keySet()) {
        		int[] shape = meta.getDataShapes().get(name);
        		if (shape != null) {
        			//squeeze to get usable rank
        			int[] ss = squeezeShape(shape, false);
        			if (scheme==null || scheme.isRankSupported(ss.length)) {
        				names.put(name, shape);
        			} 
        		} else {
        			//null shape is a bad sign
        			names.clear();
        			break;
        		}
        	}
        }
        
        if (names.isEmpty()) {
        	IDataHolder dataHolder;
			try {
				dataHolder = ServiceHolder.getLoaderService().getData(path, null);
			} catch (Exception e) {
				return names;
			}
        	if (dataHolder!=null) for (String name : dataHolder.getNames()) {
        		if (name.contains("Image Stack")) continue;
        		if (!names.containsKey(name)) {

        			int[] shape = dataHolder.getLazyDataset(name).getShape();
        			int[] ss = squeezeShape(shape, false);
        			if (scheme==null || scheme.isRankSupported(ss.length)) {
        				names.put(name, shape);
        			} 

        		}
        	}
        }
	    return sortedByRankThenLength(names);
	}
	
	/**
	 * Remove dimensions of 1 in given shape - from both ends only, if true
	 * 
	 * @param oshape
	 * @param onlyFromEnds
	 * @return newly squeezed shape (or original if unsqueezed)
	 */
	private static int[] squeezeShape(final int[] oshape, boolean onlyFromEnds) {
		int unitDims = 0;
		int rank = oshape.length;
		int start = 0;

		if (onlyFromEnds) {
			int i = rank - 1;
			for (; i >= 0; i--) {
				if (oshape[i] == 1) {
					unitDims++;
				} else {
					break;
				}
			}
			for (int j = 0; j <= i; j++) {
				if (oshape[j] == 1) {
					unitDims++;
				} else {
					start = j;
					break;
				}
			}
		} else {
			for (int i = 0; i < rank; i++) {
				if (oshape[i] == 1) {
					unitDims++;
				}
			}
		}

		if (unitDims == 0) {
			return oshape;
		}

		int[] newDims = new int[rank - unitDims];
		if (unitDims == rank)
			return newDims; // zero-rank dataset

		if (onlyFromEnds) {
			rank = newDims.length;
			for (int i = 0; i < rank; i++) {
				newDims[i] = oshape[i+start];
			}
		} else {
			int j = 0;
			for (int i = 0; i < rank; i++) {
				if (oshape[i] > 1) {
					newDims[j++] = oshape[i];
					if (j >= newDims.length)
						break;
				}
			}
		}

		return newDims;
	}
	
	private static Map<String, int[]> sortedByRankThenLength(Map<String, int[]> map) {
		
		List<Entry<String, int[]>> ll = new LinkedList<Entry<String, int[]>>(map.entrySet());
		
		Collections.sort(ll, new Comparator<Entry<String, int[]>>() {

			@Override
			public int compare(Entry<String, int[]> o1, Entry<String, int[]> o2) {
				int val = Integer.compare(o2.getValue().length, o1.getValue().length);
				
				if (val == 0) val = Integer.compare(o1.getKey().length(), o2.getKey().length());
				
				return val;
			}
		});
		
		Map<String, int[]> lhm = new LinkedHashMap<String, int[]>();
		
		for (Entry<String, int[]> e : ll) lhm.put(e.getKey(), e.getValue());
		
		return lhm;
		
	}
	
}
