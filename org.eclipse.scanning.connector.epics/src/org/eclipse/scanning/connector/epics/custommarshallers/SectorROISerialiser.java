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

import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVInt;
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
public class SectorROISerialiser implements IPVStructureSerialiser<SectorROI> {

	@Override
	public Structure buildStructure(Serialiser serialiser, SectorROI roi) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();
		
		Structure structure = fieldCreate.createFieldBuilder().
			add("averageArea", ScalarType.pvBoolean).
			add("dpp", ScalarType.pvDouble).
			add("symmetry", ScalarType.pvInt).
			addArray("angles", ScalarType.pvDouble).
			addArray("anglesDegrees", ScalarType.pvDouble).
			addArray("radii", ScalarType.pvDouble).
			addArray("point", ScalarType.pvDouble).
			setId("SectorROI").
			createStructure();
		return structure;
	}

	@Override
	public void populatePVStructure(Serialiser serialiser, SectorROI roi, PVStructure pvStructure) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		PVBoolean averageArea = pvStructure.getSubField(PVBoolean.class, "averageArea");
		averageArea.put(roi.isAverageArea());		
		PVDouble dpp = pvStructure.getSubField(PVDouble.class, "dpp");
		dpp.put(roi.getDpp());			
		PVInt symmetry = pvStructure.getSubField(PVInt.class, "symmetry");
		symmetry.put(roi.getSymmetry());
		PVDoubleArray angles = pvStructure.getSubField(PVDoubleArray.class, "angles");
		angles.put(0, roi.getAngles().length, roi.getAngles(), 0);
		PVDoubleArray anglesDegrees = pvStructure.getSubField(PVDoubleArray.class, "anglesDegrees");
		anglesDegrees.put(0, roi.getAnglesDegrees().length, roi.getAnglesDegrees(), 0);		
		PVDoubleArray radii = pvStructure.getSubField(PVDoubleArray.class, "radii");
		radii.put(0, roi.getRadii().length, roi.getRadii(), 0);
		PVDoubleArray point = pvStructure.getSubField(PVDoubleArray.class, "point");
		point.put(0, roi.getPoint().length, roi.getPoint(), 0);
	}
	
}
