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
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureDeserialiser;
import org.epics.pvmarshaller.marshaller.deserialisers.Deserialiser;

/**
 * Custom deserialiser for Bounding Box.
 * TODO - make this non 'test' and finalise custom serialisation strategy for models 
 * @author Matt Taylor
 *
 */
public class BoundingBoxDeserialiser implements IPVStructureDeserialiser {

	@Override
	public Object fromPVStructure(Deserialiser deserialiser, PVStructure pvStructure) throws Exception {
		BoundingBox boundingBox = new BoundingBox();
		boundingBox.setFastAxisStart(pvStructure.getSubField(PVDouble.class, "fastAxisStart").get());
		boundingBox.setSlowAxisStart(pvStructure.getSubField(PVDouble.class, "slowAxisStart").get());
		boundingBox.setFastAxisLength(pvStructure.getSubField(PVDouble.class, "fastAxisLength").get());
		boundingBox.setSlowAxisLength(pvStructure.getSubField(PVDouble.class, "slowAxisLength").get());
		return boundingBox;
	}
}
