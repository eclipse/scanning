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

import org.eclipse.scanning.api.malcolm.attributes.BooleanAttribute;
import org.eclipse.scanning.api.malcolm.attributes.ChoiceAttribute;
import org.eclipse.scanning.api.malcolm.attributes.NumberAttribute;
import org.eclipse.scanning.api.malcolm.attributes.StringAttribute;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.StringArrayData;
import org.epics.pvmarshaller.marshaller.api.IPVStructureDeserialiser;
import org.epics.pvmarshaller.marshaller.deserialisers.Deserialiser;

/**
 * Custom deserialiser for NTScalar
 * @author Matt Taylor
 *
 */
public class NTScalarDeserialiser implements IPVStructureDeserialiser {
		
	private final String valueField = "value";
	private final String numberTypeField = "dtype";
	private final String choicesTagField = "choices";
	private final String metaField = "meta";
	private final String descriptionField = "description";
	private final String writeableField = "writeable";
	private final String labelField = "label";
	private final String tagsField = "tags";

	@Override
	public Object fromPVStructure(Deserialiser deserialiser, PVStructure pvStructure) throws Exception {
		
		PVStructure metaStructure = pvStructure.getStructureField(metaField);
		String metaId = metaStructure.getStructure().getID();
		String description = metaStructure.getStringField(descriptionField).get();
		boolean writeable = metaStructure.getBooleanField(writeableField).get();
		String label = metaStructure.getStringField(labelField).get();
		PVStringArray tagsArray = metaStructure.getSubField(PVStringArray.class, tagsField);
		StringArrayData tagsArrayData = new StringArrayData();
		tagsArray.get(0, tagsArray.getLength(), tagsArrayData);
		
		if (metaId.startsWith(ChoiceAttribute.CHOICE_ID)) {
			ChoiceAttribute attribute = new ChoiceAttribute();
			
			attribute.setDescription(description);
			attribute.setLabel(label);
			attribute.setTags(tagsArrayData.data);
			attribute.setWriteable(writeable);
			attribute.setName(pvStructure.getFullName());
			
			PVStringArray choicesArray = metaStructure.getSubField(PVStringArray.class, choicesTagField);
			StringArrayData choicesArrayData = new StringArrayData();
			choicesArray.get(0, choicesArray.getLength(), choicesArrayData);
			attribute.setChoices(choicesArrayData.data);
			
			String value = pvStructure.getStringField(valueField).get();
			attribute.setValue(value);
			return attribute;
		} else if (metaId.startsWith(StringAttribute.STRING_ID)) {
			StringAttribute attribute = new StringAttribute();
			
			attribute.setDescription(description);
			attribute.setLabel(label);
			attribute.setTags(tagsArrayData.data);
			attribute.setWriteable(writeable);
			attribute.setName(pvStructure.getFullName());
			
			String value = pvStructure.getStringField(valueField).get();
			attribute.setValue(value);
			return attribute;
		} else if (metaId.startsWith(BooleanAttribute.BOOLEAN_ID)) {
			BooleanAttribute attribute = new BooleanAttribute();
			
			attribute.setDescription(description);
			attribute.setLabel(label);
			attribute.setTags(tagsArrayData.data);
			attribute.setWriteable(writeable);
			attribute.setName(pvStructure.getFullName());
			
			boolean value = pvStructure.getBooleanField(valueField).get();
			attribute.setValue(value);
			return attribute;
		} else if (metaId.startsWith(NumberAttribute.NUMBER_ID)) {
			NumberAttribute attribute = new NumberAttribute();
			
			attribute.setDescription(description);
			attribute.setLabel(label);
			attribute.setTags(tagsArrayData.data);
			attribute.setWriteable(writeable);
			attribute.setName(pvStructure.getFullName());
			
			String numberType = metaStructure.getStringField(numberTypeField).get();
			attribute.setDtype(numberType);
			
			PVField valuePVField = pvStructure.getSubField(valueField);
			
			// Use scalar deserialiser to get value. Class passed in can be null as it's only used
			// to determine between String and char, and we 'know' it's a number here
			Object value = deserialiser.getScalarDeserialiser().deserialise(valuePVField, null);

			if (value instanceof Number) {
				Number number = (Number)value;
				attribute.setValue(number);
			} else {
				throw new Exception(pvStructure.getFullName() + " has a number field that isn't a number");
			}
			
			return attribute;
		}
		
		throw new Exception("Unrecognised NTScalar type: " + metaId);
	}
}
