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

import org.eclipse.scanning.api.points.models.GridModel;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureDeserialiser;
import org.epics.pvmarshaller.marshaller.deserialisers.Deserialiser;

/**
 * Custom deserialiser for Grid model.
 * TODO - make this non 'test' and finalise custom serialisation strategy for models 
 * @author Matt Taylor
 *
 */
public class GridModelDeserialiser implements IPVStructureDeserialiser {

	@Override
	public Object fromPVStructure(Deserialiser deserialiser, PVStructure pvStructure) throws Exception {
		GridModel gridModel = new GridModel();
		gridModel.setName(pvStructure.getSubField(PVString.class, "name").get());
		gridModel.setFastAxisName(pvStructure.getSubField(PVString.class, "fastAxisName").get());
		gridModel.setSlowAxisName(pvStructure.getSubField(PVString.class, "slowAxisName").get());
		gridModel.setFastAxisPoints(pvStructure.getSubField(PVInt.class, "fastAxisPoints").get());
		gridModel.setSlowAxisPoints(pvStructure.getSubField(PVInt.class, "slowAxisPoints").get());
		gridModel.setSnake(pvStructure.getBooleanField("snake").get());
		return gridModel;
	}
}
