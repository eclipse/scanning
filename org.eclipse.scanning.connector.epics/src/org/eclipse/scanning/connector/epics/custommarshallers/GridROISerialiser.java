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
package org.eclipse.scanning.connector.epics.custommarshallers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.dawnsci.analysis.dataset.roi.GridROI;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureSerialiser;
import org.epics.pvmarshaller.marshaller.serialisers.Serialiser;

/**
 * Custom serialiser for Circular ROI.
 * TODO - make this non 'test' and finalise custom serialisation strategy for ROIs 
 * @author Matt Taylor
 *
 */
public class GridROISerialiser implements IPVStructureSerialiser<GridROI> {

	@Override
	public Structure buildStructure(Serialiser serialiser, GridROI roi) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();
		
		Structure structure = fieldCreate.createFieldBuilder().
			add("xSpacing", ScalarType.pvDouble).
			add("ySpacing", ScalarType.pvDouble).
			addArray("spacing", ScalarType.pvDouble).
			addArray("point", ScalarType.pvDouble).
			setId("RingROI").
			createStructure();
		return structure;
	}

	@Override
	public void populatePVStructure(Serialiser serialiser, GridROI roi, PVStructure pvStructure) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		PVDouble xSpacing = pvStructure.getSubField(PVDouble.class, "xSpacing");
		xSpacing.put(roi.getxSpacing());		
		PVDouble ySpacing = pvStructure.getSubField(PVDouble.class, "ySpacing");
		ySpacing.put(roi.getySpacing());
		PVDoubleArray spacing = pvStructure.getSubField(PVDoubleArray.class, "spacing");
		spacing.put(0, roi.getSpacing().length, roi.getSpacing(), 0);
		//PVDoubleArray gridPoints = pvStructure.getSubField(PVDoubleArray.class, "gridPoints");
		//gridPoints.put(0, roi.getGridPoints()().length, roi.getSpacing(), 0);
		// TODO can't do grid points as array of arrays
		PVDoubleArray point = pvStructure.getSubField(PVDoubleArray.class, "point");
		point.put(0, roi.getPoint().length, roi.getPoint(), 0);
	}
	
}
