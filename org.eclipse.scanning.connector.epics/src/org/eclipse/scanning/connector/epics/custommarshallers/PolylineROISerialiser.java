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
import java.util.List;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolylineROI;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnion;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Union;
import org.epics.pvmarshaller.marshaller.api.IPVStructureSerialiser;
import org.epics.pvmarshaller.marshaller.serialisers.Serialiser;

/**
 * Custom serialiser for Circular ROI.
 * TODO - make this non 'test' and finalise custom serialisation strategy for ROIs 
 * @author Matt Taylor
 *
 */
public class PolylineROISerialiser implements IPVStructureSerialiser<PolylineROI> {

	@Override
	public Structure buildStructure(Serialiser serialiser, PolylineROI roi) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		Union union = fieldCreate.createVariantUnion();
		
		Structure structure = fieldCreate.createFieldBuilder().
			addArray("point", ScalarType.pvDouble).
			addArray("points", union).
			setId("PolylineROI").
			createStructure();
		return structure;
	}

	@Override
	public void populatePVStructure(Serialiser serialiser, PolylineROI roi, PVStructure pvStructure) throws Exception {
		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
		
		PVDoubleArray point = pvStructure.getSubField(PVDoubleArray.class, "point");
		point.put(0, roi.getPoint().length, roi.getPoint(), 0);
		PVUnionArray points = pvStructure.getSubField(PVUnionArray.class, "points");
		
		List<IROI> pointsList = roi.getPoints();
		PVUnion[] pvUnionArray = new PVUnion[pointsList.size()];
		for (int i = 0; i < pointsList.size(); i++) {
			PVUnion pvUnion = pvDataCreate.createPVVariantUnion();
			pvUnion.set(serialiser.toPVStructure(pointsList.get(i)));
			pvUnionArray[i] = pvUnion;
		}
		
		points.put(0, pvUnionArray.length, pvUnionArray, 0);
	}
	
}
