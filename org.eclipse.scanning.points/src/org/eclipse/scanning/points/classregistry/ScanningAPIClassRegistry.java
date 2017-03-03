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
package org.eclipse.scanning.points.classregistry;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.persistence.IClassRegistry;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.device.models.ClusterProcessingModel;
import org.eclipse.scanning.api.device.models.MalcolmModel;
import org.eclipse.scanning.api.device.models.ProcessingModel;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.queues.beans.MonitorAtom;
import org.eclipse.scanning.api.event.queues.beans.MoveAtom;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;
import org.eclipse.scanning.api.event.scan.AcquireRequest;
import org.eclipse.scanning.api.event.scan.DeviceRequest;
import org.eclipse.scanning.api.event.scan.PositionerRequest;
import org.eclipse.scanning.api.event.scan.SampleData;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanEvent;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.event.status.AdministratorMessage;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.malcolm.MalcolmTable;
import org.eclipse.scanning.api.malcolm.attributes.BooleanArrayAttribute;
import org.eclipse.scanning.api.malcolm.attributes.BooleanAttribute;
import org.eclipse.scanning.api.malcolm.attributes.ChoiceAttribute;
import org.eclipse.scanning.api.malcolm.attributes.MalcolmAttribute;
import org.eclipse.scanning.api.malcolm.attributes.NumberArrayAttribute;
import org.eclipse.scanning.api.malcolm.attributes.NumberAttribute;
import org.eclipse.scanning.api.malcolm.attributes.PointGeneratorAttribute;
import org.eclipse.scanning.api.malcolm.attributes.StringArrayAttribute;
import org.eclipse.scanning.api.malcolm.attributes.StringAttribute;
import org.eclipse.scanning.api.malcolm.attributes.TableAttribute;
import org.eclipse.scanning.api.malcolm.event.MalcolmEventBean;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.StaticPosition;
import org.eclipse.scanning.api.points.models.ArrayModel;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.BoundingLine;
import org.eclipse.scanning.api.points.models.CollatedStepModel;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.LissajousModel;
import org.eclipse.scanning.api.points.models.MultiStepModel;
import org.eclipse.scanning.api.points.models.OneDEqualSpacingModel;
import org.eclipse.scanning.api.points.models.OneDStepModel;
import org.eclipse.scanning.api.points.models.RandomOffsetGridModel;
import org.eclipse.scanning.api.points.models.RasterModel;
import org.eclipse.scanning.api.points.models.RepeatedPointModel;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.points.models.SinglePointModel;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StaticModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.AxisConfiguration;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.event.Location;
import org.eclipse.scanning.api.scan.models.ScanMetadata;
import org.eclipse.scanning.api.scan.ui.ControlEnumNode;
import org.eclipse.scanning.api.scan.ui.ControlFileNode;
import org.eclipse.scanning.api.scan.ui.ControlGroup;
import org.eclipse.scanning.api.scan.ui.ControlNode;
import org.eclipse.scanning.api.scan.ui.ControlTree;
import org.eclipse.scanning.api.script.ScriptLanguage;
import org.eclipse.scanning.api.script.ScriptRequest;
import org.eclipse.scanning.api.script.ScriptResponse;

/**
 * 
 * The registry is here because it makes dependencies on DAWNSCI
 * in order to link in beans to the marshaller. 
 * 
 * @author Martin Gaughran
 * @author Matthew Gerring
 *
 */
public class ScanningAPIClassRegistry implements IClassRegistry {

	private static final Map<String, Class<?>> idToClassMap;
	static {
		Map<String, Class<?>> tmp = new HashMap<String, Class<?>>();
		
		// event.scan
		registerClass(tmp, DeviceRequest.class);
		registerClass(tmp, MonitorRole.class);  // TODO Is this wrong? Should enums not need explicit registration.
		registerClass(tmp, PositionerRequest.class);
		registerClass(tmp, AcquireRequest.class);
		registerClass(tmp, ScanBean.class);
		registerClass(tmp, ScanEvent.class);
		registerClass(tmp, SampleData.class);
		registerClass(tmp, ScanRequest.class);
		registerClass(tmp, ScanMetadata.class);
		
		// points
		registerClass(tmp, StaticPosition.class);
		registerClass(tmp, MapPosition.class);
		registerClass(tmp, Point.class);
		registerClass(tmp, Scalar.class);
		
		// points.models
		registerClass(tmp, ArrayModel.class);
		registerClass(tmp, BoundingBox.class);
		registerClass(tmp, BoundingLine.class);
		registerClass(tmp, CollatedStepModel.class);
		registerClass(tmp, CompoundModel.class);
		registerClass(tmp, StaticModel.class);
		registerClass(tmp, GridModel.class);
		registerClass(tmp, LissajousModel.class);
		registerClass(tmp, OneDEqualSpacingModel.class);
		registerClass(tmp, OneDStepModel.class);
		registerClass(tmp, RandomOffsetGridModel.class);
		registerClass(tmp, RasterModel.class);
		registerClass(tmp, ScanRegion.class);
		registerClass(tmp, SinglePointModel.class);
		registerClass(tmp, SpiralModel.class);
		registerClass(tmp, RepeatedPointModel.class);
		registerClass(tmp, StepModel.class);
		registerClass(tmp, MultiStepModel.class);
		
		// scan.ui
		registerClass(tmp, ControlEnumNode.class);
		registerClass(tmp, ControlFileNode.class);
		registerClass(tmp, ControlGroup.class);
		registerClass(tmp, ControlNode.class);
		registerClass(tmp, ControlTree.class);
		registerClass(tmp, AxisConfiguration.class);
		
		// event.alive
		registerClass(tmp, HeartbeatBean.class);
		registerClass(tmp, KillBean.class);
		registerClass(tmp, PauseBean.class);
		
		// event.status
		registerClass(tmp, AdministratorMessage.class);
		registerClass(tmp, StatusBean.class);
		
		// event.queues.beans
		registerClass(tmp, QueueRequest.class);
		registerClass(tmp, MonitorAtom.class);
		registerClass(tmp, MoveAtom.class);
		registerClass(tmp, ScanAtom.class);
		registerClass(tmp, SubTaskAtom.class);
		registerClass(tmp, TaskBean.class);
		
		// malcolm.event
		registerClass(tmp, MalcolmModel.class);
		registerClass(tmp, Float.class);
		registerClass(tmp, MalcolmEventBean.class);
		registerClass(tmp, MalcolmTable.class);
		registerClass(tmp, ChoiceAttribute.class);
		registerClass(tmp, BooleanArrayAttribute.class);
		registerClass(tmp, BooleanAttribute.class);
		registerClass(tmp, MalcolmAttribute.class);
		registerClass(tmp, NumberArrayAttribute.class);
		registerClass(tmp, NumberAttribute.class);
		registerClass(tmp, PointGeneratorAttribute.class);
		registerClass(tmp, StringArrayAttribute.class);
		registerClass(tmp, StringAttribute.class);
		registerClass(tmp, TableAttribute.class);

		// api.scan
		registerClass(tmp, PositionEvent.class);

		// scan.event
		registerClass(tmp, Location.class);
		
		// device.models
		registerClass(tmp, ProcessingModel.class);
		registerClass(tmp, ClusterProcessingModel.class);

		// script
		registerClass(tmp, ScriptLanguage.class);
		registerClass(tmp, ScriptRequest.class);
		registerClass(tmp, ScriptResponse.class);
		

		idToClassMap = tmp;
	}
	
	public ScanningAPIClassRegistry() {
		
	}
	public ScanningAPIClassRegistry(Class<?>... extras) {
		if (extras!=null) {
			for (Class<?> class1 : extras) {
				registerClass(idToClassMap, class1);
			}
		}
	}
	
	private static void registerClass(Map<String, Class<?>> map, Class<?> clazz) {
		map.put(clazz.getSimpleName(), clazz);
	}

	@Override
	public Map<String, Class<?>> getIdToClassMap() {
		return idToClassMap;
	}
}
