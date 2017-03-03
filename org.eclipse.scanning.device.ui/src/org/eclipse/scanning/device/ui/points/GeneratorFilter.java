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
package org.eclipse.scanning.device.ui.points;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.richbeans.widgets.table.ISeriesItemDescriptor;
import org.eclipse.richbeans.widgets.table.ISeriesItemFilter;
import org.eclipse.richbeans.widgets.table.SeriesTable;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GeneratorFilter implements ISeriesItemFilter {
	
	private static final Logger logger = LoggerFactory.getLogger(GeneratorFilter.class);
	
	private final IPointGeneratorService pservice;
	private final SeriesTable            table; // Gets the table which provides access the series via getAdapter(...)
	private final IAdaptable             parent;

	public GeneratorFilter(IPointGeneratorService pservice, SeriesTable table, IAdaptable parent) {
		this.pservice     = pservice;
		this.table        = table;
		this.parent       = parent;
	}
	
	@Override
	public Collection<ISeriesItemDescriptor> getDescriptors(String contents, int position, ISeriesItemDescriptor previous) {
		
		try {
			// Reassign previous, if required
			final Collection<String> ids = pservice.getRegisteredGenerators();			

			// Get sorted generator list.
			final Collection<ISeriesItemDescriptor> ret = new ArrayList<ISeriesItemDescriptor>(7);
			
			for (String id : ids) {
									
				final GeneratorDescriptor des = new GeneratorDescriptor(table, id, pservice, parent);
				if (!des.isVisible()) continue;
				if (contents!=null && !des.matches(contents)) continue;
				ret.add(des);
			}

			return ret;
			
		} catch (Exception e) {
			logger.error("Cannot get operations!", e);
			return null;
		}
	}
	
	public List<GeneratorDescriptor<?>> createDescriptors(List<? extends IScanPathModel> models) throws Exception {
		
	    List<GeneratorDescriptor<?>> descriptions = new ArrayList<>();
		if (models!=null && models.size()>0) {
			for (IScanPathModel model : models) {
				final GeneratorDescriptor<?> des = new GeneratorDescriptor<>(table, model, pservice, parent);
				if (!des.isVisible()) continue;
				descriptions.add(des);
			}
		}
		return descriptions;
	}

	
	public List<Object> getModels(List<ISeriesItemDescriptor> seriesItems) throws Exception {
		List<Object> models = new ArrayList<>();
		for (ISeriesItemDescriptor des : seriesItems) {
			if (des instanceof GeneratorDescriptor) {
				GeneratorDescriptor<?> gdes = (GeneratorDescriptor<?>)des;
				models.add(gdes.getModel());
			}
		}
		if (models.size()>0) {
		    return models;
		} else {
			return null;
		}
	}
	
}
