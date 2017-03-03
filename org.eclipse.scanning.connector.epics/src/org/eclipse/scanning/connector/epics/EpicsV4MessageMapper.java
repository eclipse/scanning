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
package org.eclipse.scanning.connector.epics;

import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.FreeDrawROI;
import org.eclipse.dawnsci.analysis.dataset.roi.GridROI;
import org.eclipse.dawnsci.analysis.dataset.roi.HyperbolicROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.ParabolicROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PerimeterBoxROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PointROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolygonalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolylineROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RingROI;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.dawnsci.analysis.dataset.roi.XAxisBoxROI;
import org.eclipse.dawnsci.analysis.dataset.roi.YAxisBoxROI;
import org.eclipse.scanning.api.device.models.MalcolmModel;
import org.eclipse.scanning.api.malcolm.message.MalcolmMessage;
import org.eclipse.scanning.api.malcolm.message.Type;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.connector.epics.custommarshallers.BoundingBoxDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.BoundingBoxSerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.CircularROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.CircularROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.EllipticalROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.EllipticalROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.FreeDrawROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.FreeDrawROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.GridModelDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.GridModelSerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.GridROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.HyperbolicROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.HyperbolicROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.IPointGeneratorSerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.LinearROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.LinearROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.MalcolmMessageSerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.MalcolmPointGeneratorDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.NTScalarArrayDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.NTScalarDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.NTTableDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.ParabolicROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.ParabolicROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.PerimeterBoxROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.PerimeterBoxROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.PointROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.PointROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.PolygonalROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.PolygonalROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.PolylineROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.PolylineROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.PyDictionarySerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.RectangularROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.RectangularROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.RingROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.RingROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.SectorROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.SectorROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.SpiralModelDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.SpiralModelSerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.StepModelDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.StepModelSerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.XAxisBoxROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.XAxisBoxROISerialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.YAxisBoxROIDeserialiser;
import org.eclipse.scanning.connector.epics.custommarshallers.YAxisBoxROISerialiser;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnion;
import org.epics.pvmarshaller.marshaller.PVMarshaller;
import org.python.core.PyDictionary;

public class EpicsV4MessageMapper {
	
	private PVMarshaller marshaller;
	
	private static String ERROR_TYPE = "malcolm:core/Error:";
	
	private static String TYPE_ID_KEY = "typeid";
	
	public EpicsV4MessageMapper() {
		marshaller = new PVMarshaller();
		marshaller.registerMapTypeIdKey(TYPE_ID_KEY);
		marshaller.registerSerialiser(IPointGenerator.class, new IPointGeneratorSerialiser());
		marshaller.registerSerialiser(PyDictionary.class, new PyDictionarySerialiser());
		marshaller.registerSerialiser(MalcolmMessage.class, new MalcolmMessageSerialiser());
		
		marshaller.registerSerialiser(SpiralModel.class, new SpiralModelSerialiser());
		marshaller.registerDeserialiser("SpiralModel", new SpiralModelDeserialiser());
		marshaller.registerSerialiser(StepModel.class, new StepModelSerialiser());
		marshaller.registerDeserialiser("StepModel", new StepModelDeserialiser());
		marshaller.registerSerialiser(GridModel.class, new GridModelSerialiser());
		marshaller.registerDeserialiser("GridModel", new GridModelDeserialiser());
		
		marshaller.registerSerialiser(CircularROI.class, new CircularROISerialiser());
		marshaller.registerDeserialiser("CircularROI", new CircularROIDeserialiser());
		marshaller.registerSerialiser(EllipticalROI.class, new EllipticalROISerialiser());
		marshaller.registerDeserialiser("EllipticalROI", new EllipticalROIDeserialiser());
		marshaller.registerSerialiser(FreeDrawROI.class, new FreeDrawROISerialiser());
		marshaller.registerDeserialiser("FreeDrawROI", new FreeDrawROIDeserialiser());
		marshaller.registerSerialiser(GridROI.class, new GridROISerialiser());
		marshaller.registerSerialiser(HyperbolicROI.class, new HyperbolicROISerialiser());
		marshaller.registerDeserialiser("HyperbolicROI", new HyperbolicROIDeserialiser());
		marshaller.registerSerialiser(LinearROI.class, new LinearROISerialiser());
		marshaller.registerDeserialiser("LinearROI", new LinearROIDeserialiser());
		marshaller.registerSerialiser(ParabolicROI.class, new ParabolicROISerialiser());
		marshaller.registerDeserialiser("ParabolicROI", new ParabolicROIDeserialiser());
		marshaller.registerSerialiser(PerimeterBoxROI.class, new PerimeterBoxROISerialiser());
		marshaller.registerDeserialiser("PerimeterBoxROI", new PerimeterBoxROIDeserialiser());
		marshaller.registerSerialiser(PointROI.class, new PointROISerialiser());
		marshaller.registerDeserialiser("PointROI", new PointROIDeserialiser());
		marshaller.registerSerialiser(PolygonalROI.class, new PolygonalROISerialiser());
		marshaller.registerDeserialiser("PolygonalROI", new PolygonalROIDeserialiser());
		marshaller.registerSerialiser(PolylineROI.class, new PolylineROISerialiser());
		marshaller.registerDeserialiser("PolylineROI", new PolylineROIDeserialiser());
		marshaller.registerSerialiser(RectangularROI.class, new RectangularROISerialiser());
		marshaller.registerDeserialiser("RectangularROI", new RectangularROIDeserialiser());
		marshaller.registerSerialiser(RingROI.class, new RingROISerialiser());
		marshaller.registerDeserialiser("RingROI", new RingROIDeserialiser());
		marshaller.registerSerialiser(SectorROI.class, new SectorROISerialiser());
		marshaller.registerDeserialiser("SectorROI", new SectorROIDeserialiser());
		marshaller.registerSerialiser(XAxisBoxROI.class, new XAxisBoxROISerialiser());
		marshaller.registerDeserialiser("XAxisBoxROI", new XAxisBoxROIDeserialiser());
		marshaller.registerSerialiser(YAxisBoxROI.class, new YAxisBoxROISerialiser());
		marshaller.registerDeserialiser("YAxisBoxROI", new YAxisBoxROIDeserialiser());
		
		marshaller.registerSerialiser(BoundingBox.class, new BoundingBoxSerialiser());
		marshaller.registerDeserialiser("BoundingBox", new BoundingBoxDeserialiser());

		marshaller.registerDeserialiser("epics:nt/NTScalar:1.0", new NTScalarDeserialiser());
		marshaller.registerDeserialiser("epics:nt/NTScalarArray:1.0", new NTScalarArrayDeserialiser());
		marshaller.registerDeserialiser("epics:nt/NTTable:1.0", new NTTableDeserialiser());
		marshaller.registerDeserialiser("malcolm:core/PointGenerator:1.0", new MalcolmPointGeneratorDeserialiser());
	}
	
	public PVStructure convertMalcolmMessageToPVStructure(MalcolmMessage malcolmMessage) throws Exception {
				
		PVStructure pvRequest = marshaller.toPVStructure(malcolmMessage);
				
		return pvRequest;
	}
	
	public MalcolmMessage convertCallPVStructureToMalcolmMessage(PVStructure structure, MalcolmMessage message) throws Exception {
		MalcolmMessage result = new MalcolmMessage();
		result.setType(Type.RETURN);
		result.setEndpoint(message.getEndpoint());
		result.setId(message.getId());
		result.setRawValue(structure.toString());
				
		if (structure.getStructure().getID().startsWith(ERROR_TYPE)) {
			result.setType(Type.ERROR);
			PVString errorMessage = structure.getSubField(PVString.class, "message");
			result.setMessage(errorMessage.get());
		} else {
			Object returnedObject = marshaller.fromPVStructure(structure, Object.class);
			
			result.setValue(returnedObject);
		}
		
		return result;
	}
	
	public MalcolmMessage convertSubscribeUpdatePVStructureToMalcolmMessage(PVStructure structure, MalcolmMessage message) throws Exception {
		MalcolmMessage result = new MalcolmMessage();
		result.setType(Type.UPDATE);
		result.setEndpoint(message.getEndpoint());
		result.setId(message.getId());
		result.setRawValue(structure.toString());
				
		Object returnedObject = getEndpointObjectFromPVStructure(structure, message.getEndpoint());
		
		result.setValue(returnedObject);
		
		return result;
	}
	
	public MalcolmMessage convertGetPVStructureToMalcolmMessage(PVStructure structure, MalcolmMessage message)
	{
		MalcolmMessage result = new MalcolmMessage();
		result.setType(Type.RETURN);
		result.setEndpoint(message.getEndpoint());
		result.setId(message.getId());
		result.setRawValue(structure.toString());
						
		if (structure.getStructure().getID().startsWith(ERROR_TYPE)) {
			result.setType(Type.ERROR);
			PVString errorMessage = structure.getSubField(PVString.class, "message");
			result.setMessage(errorMessage.get());
		} else {
			try {
				result.setValue(getEndpointObjectFromPVStructure(structure, message.getEndpoint()));
			} catch (Exception e) {
				e.printStackTrace();
				result.setMessage("Error converting " + message.getEndpoint() + ": " + e.getMessage());
				result.setType(Type.ERROR); 
			}
		}
		
		return result;
	}
	
	public void populatePutPVStructure(PVStructure pvStructure, MalcolmMessage message) throws Exception {
		PVField endPointField = pvStructure.getSubField(message.getEndpoint());
		if (endPointField.getField().getType().equals(org.epics.pvdata.pv.Type.union)) {
			// Create from scratch for union
			PVUnion unionField = (PVUnion)endPointField;
			PVStructure newStructure = marshaller.toPVStructure(message.getValue());
			unionField.set(newStructure);
		} else {
			marshaller.setFieldWithValue(pvStructure, message.getEndpoint(), message.getValue());
		}
	}
	
	public PVStructure pvMarshal(Object anyObject) throws Exception {
		return marshaller.toPVStructure(anyObject);
	}

	public <U> U pvUnmarshal(PVStructure anyObject, Class<U> beanClass) throws Exception {
		return marshaller.fromPVStructure(anyObject, beanClass);
	} 
	
	private Object getEndpointObjectFromPVStructure(PVStructure pvStructure, String endpoint) throws Exception {

		if (endpoint.isEmpty()) {
			return marshaller.fromPVStructure(pvStructure, Object.class);
		} else if (endpoint.contains(",")) {
			return marshaller.fromPVStructure(pvStructure, Object.class);
		} else {
			PVStructure parentStructure = null;
			String[] requestArray = endpoint.split("\\.");
			
			if (requestArray.length == 1) {
				parentStructure = pvStructure;
			} else {
				String parentStructureString = "";
				for (int i = 0; i < requestArray.length - 1; i++) {
					parentStructureString += requestArray[i];
				}
				parentStructure = pvStructure.getStructureField(parentStructureString);
			}

			return marshaller.getObjectFromField(parentStructure, requestArray[requestArray.length-1]);
		}
	}
}
