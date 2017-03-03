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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.scanning.api.malcolm.MalcolmTable;
import org.eclipse.scanning.api.malcolm.attributes.TableAttribute;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.StringArrayData;
import org.epics.pvmarshaller.marshaller.api.IPVStructureDeserialiser;
import org.epics.pvmarshaller.marshaller.deserialisers.Deserialiser;

/**
 * Custom deserialiser for Malcolm Table
 * @author Matt Taylor
 *
 */
public class NTTableDeserialiser implements IPVStructureDeserialiser {
	
	private final String valueField = "value";
	private final String headingsTagField = "labels";
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
		
		// TODO read column meta data?
		
		TableAttribute attribute = new TableAttribute();
		
		attribute.setDescription(description);
		attribute.setLabel(label);
		attribute.setTags(tagsArrayData.data);
		attribute.setWriteable(writeable);
		attribute.setName(pvStructure.getFullName());
		
		PVStringArray headingsArray = pvStructure.getSubField(PVStringArray.class, headingsTagField);
		StringArrayData headingsArrayData = new StringArrayData();
		headingsArray.get(0, headingsArray.getLength(), headingsArrayData);
		attribute.setHeadings(headingsArrayData.data);
		
		PVStructure valuePVStructure = pvStructure.getStructureField(valueField);
		
		Map<String, ArrayList<Object>> valueMap = deserialiser.getMapDeserialiser().createMapFromPVStructure(valuePVStructure, LinkedHashMap.class, Object.class);
		
		Map<String, Class<?>> dataTypeMap = new LinkedHashMap<>();
		
		// Use the PVStructure to work out the column classes
		for (String heading : valueMap.keySet()) {
			Field field = valuePVStructure.getSubField(heading).getField();
			if (field instanceof ScalarArray) {
				ScalarArray scalarArray = (ScalarArray)field;
				ScalarType scalarType = scalarArray.getElementType();
				switch (scalarType) {
					case pvInt:
						dataTypeMap.put(heading, Integer.class);
						break;
					case pvShort:
						dataTypeMap.put(heading, Short.class);
						break;
					case pvLong:
						dataTypeMap.put(heading, Long.class);
						break;
					case pvByte:
						dataTypeMap.put(heading, Byte.class);
						break;
					case pvBoolean:
						dataTypeMap.put(heading, Boolean.class);
						break;
					case pvFloat:
						dataTypeMap.put(heading, Float.class);
						break;
					case pvDouble:
						dataTypeMap.put(heading, Double.class);
						break;
					case pvString:
						dataTypeMap.put(heading, String.class);
						break;
					default:
						throw new Exception("Unsupported data type: " + scalarType);
							
				}
			}
		}
		
		MalcolmTable malcolmTable = new MalcolmTable(valueMap, dataTypeMap);
				
		attribute.setValue(malcolmTable);
		
		return attribute;
		
	}
	
	
}
