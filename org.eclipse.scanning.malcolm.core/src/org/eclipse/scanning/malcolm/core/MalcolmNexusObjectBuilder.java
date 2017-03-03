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
package org.eclipse.scanning.malcolm.core;

import static org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice.DATASETS_TABLE_COLUMN_FILENAME;
import static org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice.DATASETS_TABLE_COLUMN_NAME;
import static org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice.DATASETS_TABLE_COLUMN_PATH;
import static org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice.DATASETS_TABLE_COLUMN_RANK;
import static org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice.DATASETS_TABLE_COLUMN_TYPE;
import static org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice.DATASETS_TABLE_COLUMN_UNIQUEID;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXmonitor;
import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NexusBaseClass;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.scanning.api.device.models.IMalcolmModel;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.MalcolmTable;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * A helper class that knows how to build the NeXus objects and the {@link NexusObjectProvider}s
 * that wrap and describe them for an {@link IMalcolmDevice}. Each instance of this object
 * should only be used once.
 * 
 * @author Matthew Dickie
 */
class MalcolmNexusObjectBuilder<M extends IMalcolmModel> {
	
	private static final String PROPERTY_NAME_UNIQUE_KEYS = "uniqueKeys";

	private final AbstractMalcolmDevice<M> malcolmDevice;
	
	// The name (last segment only) of the malcolm output dir
	private final String malcolmOutputDirName;
	
	private final Map<String, NexusObjectWrapper<NXobject>> nexusWrappers;
	
	MalcolmNexusObjectBuilder(AbstractMalcolmDevice<M> malcolmDevice) {
		this.malcolmDevice = malcolmDevice;
		nexusWrappers = new HashMap<>();
		malcolmOutputDirName = new File(malcolmDevice.getModel().getFileDir()).getName();
	}
	
	/**
	 * Build the nexus objects for a malcolm device according to the value of the
	 * "datasets" attribute.
	 * @param scanInfo 
	 * @return nexus object
	 * @throws ScanningException 
	 */
	public List<NexusObjectProvider<?>> buildNexusObjects(NexusScanInfo scanInfo) throws ScanningException {
		MalcolmTable datasetsTable = (MalcolmTable) malcolmDevice.getAttributeValue(
				AbstractMalcolmDevice.ATTRIBUTE_NAME_DATASETS);
		
		for (Map<String, Object> datasetRow : datasetsTable) {
			final String datasetFullName = (String) datasetRow.get(DATASETS_TABLE_COLUMN_NAME);
			final String externalFileName = (String) datasetRow.get(DATASETS_TABLE_COLUMN_FILENAME);
			final String datasetPath = (String) datasetRow.get(DATASETS_TABLE_COLUMN_PATH);
			final int datasetRank = ((Integer) datasetRow.get(DATASETS_TABLE_COLUMN_RANK)).intValue();
			final MalcolmDatasetType datasetType = MalcolmDatasetType.valueOf(
					((String) datasetRow.get(DATASETS_TABLE_COLUMN_TYPE)).toUpperCase());
			final String uniqueIdPath = (String) datasetRow.get(DATASETS_TABLE_COLUMN_UNIQUEID);

			final String[] nameSegments = datasetFullName.split("\\.");
			final String deviceName = nameSegments[0];
			final String datasetName = nameSegments[1];
			
			// get the nexus object and its wrapper, creating it if necessary
			final NexusObjectWrapper<NXobject> nexusWrapper = getNexusProvider(deviceName,
					datasetType.getNexusBaseClass());
			final NXobject nexusObject = nexusWrapper.getNexusObject();

			// create the external link to the hdf5 file written by the malcolm device
			// TODO: use relative path when bug with loading relative external links is fixed
			final String externalFilePath = malcolmOutputDirName + "/" + externalFileName; // path relative to parent dir of scan file
			nexusWrapper.addExternalLink(nexusObject, datasetName, externalFilePath,
					datasetPath, datasetRank);
			
			if (uniqueIdPath != null && !uniqueIdPath.isEmpty()) {
				nexusWrapper.setPropertyValue(PROPERTY_NAME_UNIQUE_KEYS, uniqueIdPath);
			}
			
			// configure the nexus wrapper for the dataset
			configureNexusWrapperForDataset(datasetType, datasetName, nexusWrapper);
		}
		
		return new ArrayList<>(nexusWrappers.values());
	}

	/**
	 * Configure the nexus wrapper to describe the wrapped nexus object appropriately. 
	 * @param datasetType
	 * @param datasetName
	 * @param nexusWrapper
	 */
	private void configureNexusWrapperForDataset(final MalcolmDatasetType datasetType,
			final String datasetName, final NexusObjectWrapper<NXobject> nexusWrapper) {
		switch (datasetType) {
			case PRIMARY: {
				nexusWrapper.setPrimaryDataFieldName(datasetName);
				break;
			}
			case SECONDARY: {
				nexusWrapper.addAdditionalPrimaryDataFieldName(datasetName);
				break;
			}
			case MONITOR: {
				nexusWrapper.addAxisDataFieldName(datasetName);
				if (nexusWrapper.getPrimaryDataFieldName() == null) {
					nexusWrapper.setPrimaryDataFieldName(datasetName);
				}
				break;
			}
			case POSITION_VALUE: {
				nexusWrapper.addAxisDataFieldName(datasetName);
				nexusWrapper.setPrimaryDataFieldName(datasetName);
				break;
			}
			case POSITION_SET: {
				nexusWrapper.addAxisDataFieldName(datasetName);
				nexusWrapper.setDefaultAxisDataFieldName(datasetName);
				break;
			}
		}
	}
	
	private NexusObjectWrapper<NXobject> getNexusProvider(String deviceName, NexusBaseClass nexusBaseClass) {
		if (nexusWrappers.containsKey(deviceName)) {
			return nexusWrappers.get(deviceName);
		}
		
		final NXobject nexusObject = NexusNodeFactory.createNXobjectForClass(nexusBaseClass);
		
		if (nexusBaseClass == NexusBaseClass.NX_DETECTOR) {
			((NXdetector) nexusObject).setCount_timeScalar(malcolmDevice.getModel().getExposureTime());
		} else if (nexusBaseClass == NexusBaseClass.NX_MONITOR) {
			((NXmonitor) nexusObject).setCount_timeScalar(malcolmDevice.getModel().getExposureTime());
		}
		
		final NexusObjectWrapper<NXobject> nexusWrapper = new NexusObjectWrapper<>(deviceName, nexusObject);
		nexusWrappers.put(deviceName, nexusWrapper);
		
		return nexusWrapper;
	}

}
