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

import org.eclipse.scanning.api.malcolm.message.MalcolmMessage;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureSerialiser;
import org.epics.pvmarshaller.marshaller.serialisers.Serialiser;

/**
 * Custom serialiser for the MalcolmMessage class
 * 
 * @author Matt Taylor
 *
 */
public class MalcolmMessageSerialiser implements IPVStructureSerialiser<MalcolmMessage> {

	private Convert convert = ConvertFactory.getConvert();
	private FieldCreate fieldCreate = FieldFactory.getFieldCreate();
	
	@Override
	public Structure buildStructure(Serialiser serialiser, MalcolmMessage msg) throws Exception {
		Structure structure = null;

		switch (msg.getType()) {
		case CALL:

			Structure methodStructure = fieldCreate.createFieldBuilder().
				add("method", ScalarType.pvString).
				createStructure();

			Field field = null;
			
			if (msg.getArguments() != null) {
				
				if (msg.getArguments() instanceof Map) {
					ParamMap paramMap = new ParamMap();
					paramMap.setParameters((Map)msg.getArguments());
					field = serialiser.buildStructure(paramMap).getField("parameters");
				} else {
					field = serialiser.buildStructure(msg.getArguments());
				}
				
			} else {
				field = fieldCreate.createFieldBuilder().
						createStructure();
			}
			structure = fieldCreate.createFieldBuilder().
				add("method", methodStructure).
				add("parameters", field).
				createStructure();
			break;
		case GET:
			structure = fieldCreate.createFieldBuilder().
				add("type", ScalarType.pvString).
				add("id", ScalarType.pvLong).
				addArray("endpoint", ScalarType.pvString).
				createStructure();
			break;
		case PUT:
			structure = fieldCreate.createFieldBuilder().
				add("value", fieldCreate.createVariantUnion()).
				createStructure();
			break;
		default:
			throw new Exception("Unexpected MalcolmMessage type");
		}

		return structure;
	}

	@Override
	public void populatePVStructure(Serialiser serialiser, MalcolmMessage msg, PVStructure pvStructure) throws Exception {
		
		switch (msg.getType()) {
		case CALL:
			PVStructure methodName = pvStructure.getStructureField("method");
			PVString method = methodName.getSubField(PVString.class, "method");
			PVStructure parameters = pvStructure.getStructureField("parameters");
			
			method.put(msg.getMethod());
			
			if (msg.getArguments() != null) {
				if (msg.getArguments() instanceof Map) {
					ParamMap paramMap = new ParamMap();
					paramMap.setParametersFromObject((Map)msg.getArguments());
					PVStructure params = serialiser.toPVStructure(paramMap);
					convert.copyStructure(params.getStructureField("parameters"), parameters);
				} else {
					serialiser.setValues(msg.getArguments(), parameters);
				}
			}
			break;
		default:
			throw new Exception("Unexpected MalcolmMessage type");
		}
	}
	
	private class ParamMap {
		private LinkedHashMap<String, Object> parameters;

		public LinkedHashMap<String, Object> getParameters() {
			return parameters;
		}

		public void setParameters(Map<String, Object> mapObj) {
			this.parameters = new LinkedHashMap<>();
			for (String key : mapObj.keySet()) {
				this.parameters.put(key, mapObj.get(key));
			}
		}
		
		public void setParametersFromObject(Object parametersObject) throws Exception {
			if (parametersObject instanceof Map) {
				Map<String, Object> mapObj = (Map)parametersObject;
				setParameters(mapObj);
			} else {
				throw new Exception("Non Map Parameters not supported"); // TODO make this work
			}
		}
	}
}
