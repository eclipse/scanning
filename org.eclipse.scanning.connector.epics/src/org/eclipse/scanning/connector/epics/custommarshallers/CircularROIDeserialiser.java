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

import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.epics.pvdata.pv.DoubleArrayData;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureDeserialiser;
import org.epics.pvmarshaller.marshaller.deserialisers.Deserialiser;

/**
 * Custom deserialiser for Circular ROI.
 * TODO - make this non 'test' and finalise custom serialisation strategy for ROIs 
 * @author Matt Taylor
 *
 */
public class CircularROIDeserialiser implements IPVStructureDeserialiser {

	@Override
	public Object fromPVStructure(Deserialiser deserialiser, PVStructure pvStructure)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchFieldException, SecurityException {
		
		PVDoubleArray doubleArray = pvStructure.getSubField(PVDoubleArray.class, "point");
		DoubleArrayData doubleArrayData = new DoubleArrayData();
		doubleArray.get(0, doubleArray.getLength(), doubleArrayData);
		
		double radius = pvStructure.getSubField(PVDouble.class, "radius").get();
		double angle = pvStructure.getSubField(PVDouble.class, "angle").get();
		
		CircularROI roi = new CircularROI();
		roi.setRadius(radius);
		roi.setPoint(doubleArrayData.data);
		roi.setAngle(angle);
		
		return roi;
	}
}
