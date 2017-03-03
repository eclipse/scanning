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

import org.eclipse.scanning.api.points.models.BoundingBox;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureSerialiser;
import org.epics.pvmarshaller.marshaller.serialisers.Serialiser;

/**
 * Custom serialiser for Bounding Box.
 * TODO - make this non 'test' and finalise custom serialisation strategy for models 
 * @author Matt Taylor
 *
 */
public class BoundingBoxSerialiser implements IPVStructureSerialiser<BoundingBox> {

	@Override
	public Structure buildStructure(Serialiser serialiser, BoundingBox model) throws Exception {
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();
				
		Structure structure = fieldCreate.createFieldBuilder().
			add("fastAxisStart", ScalarType.pvDouble).
			add("slowAxisStart", ScalarType.pvDouble).
			add("fastAxisLength", ScalarType.pvDouble).
			add("slowAxisLength", ScalarType.pvDouble).
			setId("BoundingBox").
			createStructure();
		return structure;
	}

	@Override
	public void populatePVStructure(Serialiser serialiser, BoundingBox model, PVStructure pvStructure) throws Exception {
		PVDouble fastAxisStart = pvStructure.getSubField(PVDouble.class, "fastAxisStart");
		fastAxisStart.put(model.getFastAxisStart());
		PVDouble slowAxisStart = pvStructure.getSubField(PVDouble.class, "slowAxisStart");
		slowAxisStart.put(model.getSlowAxisStart());
		PVDouble fastAxisLength = pvStructure.getSubField(PVDouble.class, "fastAxisLength");
		fastAxisLength.put(model.getFastAxisLength());
		PVDouble slowAxisLength = pvStructure.getSubField(PVDouble.class, "slowAxisLength");
		slowAxisLength.put(model.getSlowAxisLength());
	}
	
}
