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

import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.epics.pvdata.pv.DoubleArrayData;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureDeserialiser;
import org.epics.pvmarshaller.marshaller.deserialisers.Deserialiser;

/**
 * Custom deserialiser for Rectangular ROI.
 * TODO - make this non 'test' and finalise custom serialisation strategy for ROIs 
 * @author Matt Taylor
 *
 */
public class RectangularROIDeserialiser implements IPVStructureDeserialiser {

	@Override
	public Object fromPVStructure(Deserialiser deserialiser, PVStructure pvStructure)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchFieldException, SecurityException {
		
		RectangularROI rectangularRoi = new RectangularROI();
		
		PVDoubleArray pDoubleArray = pvStructure.getSubField(PVDoubleArray.class, "point");
		DoubleArrayData pDoubleArrayData = new DoubleArrayData();
		pDoubleArray.get(0, pDoubleArray.getLength(), pDoubleArrayData);
		rectangularRoi.setPoint(pDoubleArrayData.data);
		
		rectangularRoi.setAngle(pvStructure.getSubField(PVDouble.class, "angle").get());
		
		PVDoubleArray lDoubleArray = pvStructure.getSubField(PVDoubleArray.class, "lengths");
		DoubleArrayData lDoubleArrayData = new DoubleArrayData();
		lDoubleArray.get(0, lDoubleArray.getLength(), lDoubleArrayData);
		rectangularRoi.setLengths(lDoubleArrayData.data);
		
		return rectangularRoi;
	}
}
