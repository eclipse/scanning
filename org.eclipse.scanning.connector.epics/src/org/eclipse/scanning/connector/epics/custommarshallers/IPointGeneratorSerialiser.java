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

import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.points.PySerializable;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Structure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureSerialiser;
import org.epics.pvmarshaller.marshaller.serialisers.Serialiser;
import org.python.core.PyDictionary;

/**
 * Custom serialiser for IPointGenerator.
 * TODO - make this non 'test' and finalise custom serialisation strategy for IPointGenerator 
 * @author Matt Taylor
 *
 */
public class IPointGeneratorSerialiser implements IPVStructureSerialiser<IPointGenerator> {

	@Override
	public Structure buildStructure(Serialiser serialiser, IPointGenerator generator) throws Exception {
		if (generator instanceof PySerializable) {
			PySerializable pySerializableGenerator = (PySerializable)generator;
			PyDictionary generatorAsMap = pySerializableGenerator.toDict();
			return serialiser.buildStructure(generatorAsMap);
		} else {
			throw new Exception("IPointGenerator wasn't PySerializable. Unable to serialise");
		}
	}

	@Override
	public void populatePVStructure(Serialiser serialiser, IPointGenerator generator, PVStructure pvStructure) throws Exception {
		if (generator instanceof PySerializable) {
			PySerializable pySerializableGenerator = (PySerializable)generator;
			PyDictionary generatorAsMap = pySerializableGenerator.toDict();
			serialiser.setValues(generatorAsMap, pvStructure);
		} else {
			throw new Exception("IPointGenerator wasn't PySerializable. Unable to serialise");
		}
	}
	
}
