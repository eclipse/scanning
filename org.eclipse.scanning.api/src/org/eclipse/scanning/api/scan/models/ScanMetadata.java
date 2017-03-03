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
 * Encapsulates metadata about the scan of a particular type, e.g. metadata about the sample.
 */
public class ScanMetadata {
	
	/**
	 * The type of metadata. This determines where location of the metadata in
	 * the scan output (e.g. NeXus file)
	 */
	public enum MetadataType {
		
		ENTRY,
		SAMPLE,
		INSTRUMENT,
		USER
	}
	
	/**
	 * The type of the metadata contained in this object.
	 */
	private MetadataType type; 
	
	/**
	 * A map from field name to value for the metadata contained within this object. 
	 */
	private Map<String, Object> fields = new HashMap<>();
	
	public ScanMetadata() {
		// no-args constructor for json marshalling
	}
	
	public MetadataType getType() {
		return type;
	}

	public void setType(MetadataType type) {
		this.type = type;
	}
	
	public ScanMetadata(MetadataType type) {
		this.type = type;
	}
	
	public void addField(String fieldName, Object value) {
		fields.put(fieldName, value);
	}
	
	public Object getFieldValue(String fieldName) {
		return fields.get(fieldName);
	}
	
	public Map<String, Object> getFields() {
		return fields;
	}
	
	@Deprecated
	public void setFields(Map<String, Object> fields) {
		// for use when marshalling, addField(String, Object) should be used in code
		this.fields = fields;
	}

}
