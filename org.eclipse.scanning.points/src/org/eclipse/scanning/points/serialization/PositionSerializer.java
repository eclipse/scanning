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
package org.eclipse.scanning.points.serialization;

import java.io.IOException;

import org.eclipse.scanning.api.points.IPosition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class PositionSerializer extends JsonSerializer<IPosition> {

	@Override
	public void serialize(IPosition pos, JsonGenerator gen, SerializerProvider prov) throws IOException, JsonProcessingException {

		try {
			final PositionBean bean = new PositionBean(pos);
			gen.writeObject(bean);
		} catch (Throwable ne) {
			ne.printStackTrace();
			throw ne;
		}
	}

	@Override
	public void serializeWithType(IPosition pos, JsonGenerator gen, SerializerProvider prov, TypeSerializer typeSer)
			throws IOException, JsonProcessingException {
		serialize(pos, gen, prov);
	}
}
