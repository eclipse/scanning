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

import org.eclipse.dawnsci.analysis.api.persistence.IMarshaller;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.IPosition;

@SuppressWarnings("unchecked")
public class PointsModelMarshaller implements IMarshaller {

	@Override
	public Class<IPosition> getObjectClass() {
		return IPosition.class;
	}

	@Override
	public Class<PositionSerializer> getSerializerClass() {
		return PositionSerializer.class;
	}

	@Override
	public Class<PositionDeserializer> getDeserializerClass() {
		return PositionDeserializer.class;
	}
	
	@Override
	public Class<ScanRequest> getMixinAnnotationType() {
		return ScanRequest.class;
	}
	
	@Override
	public Class<ScanRequestMixIn> getMixinAnnotationClass() {
		return ScanRequestMixIn.class;
	}

}
