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
package org.eclipse.scanning.api.scan.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Adding objects of this class to a {@link ScanDataModel} allows
 * control over how the data fields for a device are added to
 * an NXdata group (or groups) within the NeXus file for the scan. 
 */
public class ScanDeviceModel {
	
	/**
	 * An object of this class allows control over how a data field
	 * within the NeXus group for a device is added to a NXdata group (or groups).
	 */
	public static class ScanFieldModel {
		
		private Integer defaultAxisDimension = null;
		
		private int[] dimensionMappings = null;
		
		private String destinationFieldName = null;
		
		public ScanFieldModel() {
			// do nothing
		}
		
		public Integer getDefaultAxisDimension() {
			return defaultAxisDimension;
		}

		public void setDefaultAxisDimension(Integer defaultAxisDimension) {
			this.defaultAxisDimension = defaultAxisDimension;
		}

		public int[] getDimensionMappings() {
			return dimensionMappings;
		}

		public void setDimensionMappings(int[] dimensionMappings) {
			this.dimensionMappings = dimensionMappings;
		}

		public String getDestinationFieldName() {
			return destinationFieldName;
		}

		public void setDestinationFieldName(String destinationFieldName) {
			this.destinationFieldName = destinationFieldName;
		}
		
	}
	
	private Integer defaultAxisDimension = null;
	
	private int[] defaultDimensionMappings = null;
	
	private Boolean isPrimary = null;
	
	private boolean addNamedFieldsOnly = false;

	private Map<String, ScanFieldModel> fieldModels = new HashMap<>();
	
	public ScanDeviceModel() {
		// do nothing
	}
	
	public ScanDeviceModel(boolean useDeviceName) {
		this.isPrimary = useDeviceName;
	}
	
	public ScanDeviceModel(int... defaultDimensionMappings) {
		this.defaultDimensionMappings = defaultDimensionMappings;
	}
	
	public ScanDeviceModel(boolean useDeviceName, Integer defaultAxisDimension, int... defaultDimensionMappings) {
		this.isPrimary = useDeviceName;
		this.defaultAxisDimension = defaultAxisDimension;
		this.defaultDimensionMappings = defaultDimensionMappings;
	}

	public Integer getDefaultAxisDimension() {
		return defaultAxisDimension;
	}
	
	/**
	 * Set the default axis dimension. This applies to the demand data field for the
	 * device, if present, otherwise to the default writeable dataset,
	 * (e.g. "data" for an NXdetector)
	 * @param defaultAxisDimension default axis dimension
	 */
	public void setDefaultAxisDimension(int defaultAxisDimension) {
		this.defaultAxisDimension = defaultAxisDimension;
	}

	public int[] getDefaultDimensionMappings() {
		return defaultDimensionMappings;
	}

	/**
	 * Set the default dimension mappings for this device, mapping
	 * the dimension of the data fields for this device to the main
	 * data field of the detectors.
	 * This applies to all data fields unless a field specific dimension
	 * mapping is specified using {@link #setFieldDimensionMapping(String, Integer, int...)}
	 * Note: a special case is where a field has a dimension of 1 and a
	 * defaultAxisDimension is set for that field, in which case the
	 * dimension mapping is an array of length 1, mapping to that dimension
	 * of the default data field.
	 * @param defaultDimensionMappings
	 */
	public void setDefaultDimensionMappings(int... defaultDimensionMappings) {
		this.defaultDimensionMappings = defaultDimensionMappings;
	}

	public Map<String, ScanFieldModel> getFieldDimensionModels() {
		return fieldModels;
	}

	/**
	 * Adds the given fields as source fields to be added to an NXdata
	 * group for this device. Note that these fields must exist in the
	 * NeXus object for this device. Note that this method does not normally
	 * need to be called, as the names of data fields can be retrieved
	 * from the device. This method is useful to add additional fields
	 * to write (assuming that by default the device does not return
	 * the list of all writable fields), or if
	 * {@link #setAddNamedFieldsOnly(boolean)} has been set to true,
	 * in which case <em>only</em> the fields added to this
	 * {@link ScanDataModel} will be added to the NXdata group. 
	 * 
	 * @param sourceFieldNames source field names for this device
	 */
	public void addSourceFields(String... sourceFieldNames) {
		for (String sourceFieldName : sourceFieldNames) {
			fieldModels.put(sourceFieldName, null);
		}
	}
	
	public ScanFieldModel getScanFieldModel(String sourceFieldName, boolean create) {
		ScanFieldModel fieldModel = fieldModels.get(sourceFieldName);
		if (fieldModel == null && create) {
			fieldModel = new ScanFieldModel();
			fieldModels.put(sourceFieldName, fieldModel);
		}
		
		return fieldModel;
	}
	
	/**
	 * Sets the destination field name for the source field for this device with the given name
	 * @param sourceFieldName source field name
	 * @param destinationFieldName destination field name
	 */
	public void setDestinationFieldName(String sourceFieldName, String destinationFieldName) {
		getScanFieldModel(sourceFieldName, true).setDestinationFieldName(destinationFieldName);
	}
	
	public String getDestinationFieldName(String sourceFieldName) {
		ScanFieldModel fieldModel = getScanFieldModel(sourceFieldName, false);
		if (fieldModel != null) {
			return fieldModel.getDestinationFieldName();
		}
		
		return null;
	}
	
	/**
	 * Sets the field dimension mappings and (optionally) the default axis dimension
	 * for the source field for this device with the given name
	 * @param sourceFieldName source field name
	 * @param defaultAxisDimension the axis of the default data field of the NXdata
	 *   group for which the field with the given name provides a default axis,
	 *   or <code>null</code> if none
	 * @param dimensionMappings the dimension mappings between 
	 */
	public void setFieldDimensionMapping(String sourceFieldName,
			Integer defaultAxisDimension, int... dimensionMappings) {
		ScanFieldModel fieldModel = getScanFieldModel(sourceFieldName, true);
		fieldModel.setDefaultAxisDimension(defaultAxisDimension);
		if (dimensionMappings != null && dimensionMappings.length > 0) {
			fieldModel.setDimensionMappings(dimensionMappings);
		}
	}
	
	public boolean getAddNamedFieldsOnly() {
		return addNamedFieldsOnly;
	}

	/**
	 * Sets whether to add the field set within this {@link ScanDeviceModel}
	 * to the NXdata. If this is set to <code>false</code> then the names of the
	 * data fields to add for this device is retrieved from the device itself
	 * (in addition to any fields added to this {@link ScanDataModel}). Note
	 * that the fields must exist within the NeXus object for the device.
	 * @param addNamedFieldsOnly <code>true</code> to add the named fields only
	 *   <code>false</code> to use the default fields for the device
	 */
	public void setAddNamedFieldsOnly(boolean addNamedFieldsOnly) {
		this.addNamedFieldsOnly = addNamedFieldsOnly;
	}

}
