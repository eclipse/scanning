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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.scanning.api.malcolm.attributes.PointGeneratorAttribute;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.StringArrayData;
import org.epics.pvmarshaller.marshaller.api.IPVStructureDeserialiser;
import org.epics.pvmarshaller.marshaller.deserialisers.Deserialiser;

/**
 * Custom deserialiser for Malcolm PointGenerator
 * @author Matt Taylor
 *
 */
public class MalcolmPointGeneratorDeserialiser implements IPVStructureDeserialiser {
	
	private final String valueField = "value";
	private final String metaField = "meta";
	private final String descriptionField = "description";
	private final String writeableField = "writeable";
	private final String labelField = "label";
	private final String tagsField = "tags";

	@Override
	public Object fromPVStructure(Deserialiser deserialiser, PVStructure pvStructure) throws Exception {
		
		PVStructure metaStructure = pvStructure.getStructureField(metaField);
		String description = metaStructure.getStringField(descriptionField).get();
		boolean writeable = metaStructure.getBooleanField(writeableField).get();
		String label = metaStructure.getStringField(labelField).get();
		PVStringArray tagsArray = metaStructure.getSubField(PVStringArray.class, tagsField);
		StringArrayData tagsArrayData = new StringArrayData();
		tagsArray.get(0, tagsArray.getLength(), tagsArrayData);
		
		
		PointGeneratorAttribute attribute = new PointGeneratorAttribute();
		
		attribute.setDescription(description);
		attribute.setLabel(label);
		attribute.setTags(tagsArrayData.data);
		attribute.setWriteable(writeable);
		attribute.setName(pvStructure.getFullName());
		
		PVStructure valuePVStructure = pvStructure.getStructureField(valueField);
		
		Map<?, ?> valueMap = deserialiser.getMapDeserialiser().createMapFromPVStructure(valuePVStructure, LinkedHashMap.class, Object.class);
		
		attribute.setValue(valueMap);
		
		return attribute;
		
	}
}
