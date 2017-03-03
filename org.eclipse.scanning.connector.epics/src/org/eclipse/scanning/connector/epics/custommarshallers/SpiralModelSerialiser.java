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

import org.eclipse.scanning.api.points.models.SpiralModel;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureSerialiser;
import org.epics.pvmarshaller.marshaller.serialisers.Serialiser;

/**
 * Custom serialiser for Spiral model.
 * TODO - make this non 'test' and finalise custom serialisation strategy for models 
 * @author Matt Taylor
 *
 */
public class SpiralModelSerialiser implements IPVStructureSerialiser<SpiralModel> {

	@Override
	public Structure buildStructure(Serialiser serialiser, SpiralModel model) throws Exception {
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();
		
		Structure boundingBoxStructure = serialiser.buildStructure(model.getBoundingBox());
		
		Structure structure = fieldCreate.createFieldBuilder().
			add("name", ScalarType.pvString).
			add("boundingBox", boundingBoxStructure).
			add("fastAxisName", ScalarType.pvString).
			add("slowAxisName", ScalarType.pvString).
			add("scale", ScalarType.pvDouble).
			setId("SpiralModel").
			createStructure();
		return structure;
	}

	@Override
	public void populatePVStructure(Serialiser serialiser, SpiralModel model, PVStructure pvStructure) throws Exception {
		PVString name = pvStructure.getSubField(PVString.class, "name");
		name.put(model.getName());	
		PVString fastAxisName = pvStructure.getSubField(PVString.class, "fastAxisName");
		fastAxisName.put(model.getFastAxisName());		
		PVString slowAxisName = pvStructure.getSubField(PVString.class, "slowAxisName");
		slowAxisName.put(model.getSlowAxisName());		
		PVDouble scale = pvStructure.getSubField(PVDouble.class, "scale");
		scale.put(model.getScale());				
		PVStructure bbStructure = pvStructure.getStructureField("boundingBox");
		serialiser.setValues(model.getBoundingBox(), bbStructure);
	}
	
}
