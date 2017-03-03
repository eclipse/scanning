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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolylineROI;
import org.epics.pvdata.pv.DoubleArrayData;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnion;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.UnionArrayData;
import org.epics.pvmarshaller.marshaller.api.IPVStructureDeserialiser;
import org.epics.pvmarshaller.marshaller.deserialisers.Deserialiser;

/**
 * Custom deserialiser for Circular ROI.
 * TODO - make this non 'test' and finalise custom serialisation strategy for ROIs 
 * @author Matt Taylor
 *
 */
public class PolylineROIDeserialiser implements IPVStructureDeserialiser {

	@Override
	public Object fromPVStructure(Deserialiser deserialiser, PVStructure pvStructure)
			throws Exception {
		
		PVDoubleArray doubleArray = pvStructure.getSubField(PVDoubleArray.class, "point");
		DoubleArrayData doubleArrayData = new DoubleArrayData();
		doubleArray.get(0, doubleArray.getLength(), doubleArrayData);
		
		PVUnionArray unionArray = pvStructure.getSubField(PVUnionArray.class, "points");
		UnionArrayData unionArrayData = new UnionArrayData();
		unionArray.get(0, unionArray.getLength(), unionArrayData);
		
		List<IROI> iroiList = new LinkedList<>();
		
		for (int i = 0; i < unionArrayData.data.length; i++) {
			PVUnion union = unionArrayData.data[i];
			PVField pvField = union.get();
			if (pvField instanceof PVStructure) {
				PVStructure iroiPVStructure = (PVStructure)union.get();
				IROI deserialisedIROI = deserialiser.fromPVStructure(iroiPVStructure, IROI.class);
				iroiList.add(deserialisedIROI);
			}
			else
			{
				throw new Exception("Unexpected field whilst deserialising PolylineROI");
			}
		}
				
		PolylineROI roi = new PolylineROI();
		roi.setPoint(doubleArrayData.data);
		roi.setPoints(iroiList);
		
		return roi;
	}
}
