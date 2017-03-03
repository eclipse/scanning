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
package org.eclipse.scanning.points;

import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.IScanPathModel;

class GeneratorInfo {

	private Class<? extends IPointGenerator> generatorClass;
	private Class<? extends IScanPathModel>  modelClass;
	private String label;
	private String description;
	
	public Class<? extends IPointGenerator> getGeneratorClass() {
		return generatorClass;
	}
	public void setGeneratorClass(Class<? extends IPointGenerator> generatorClass) {
		this.generatorClass = generatorClass;
	}
	public Class<? extends IScanPathModel> getModelClass() {
		return modelClass;
	}
	public void setModelClass(Class<? extends IScanPathModel> modelClass) {
		this.modelClass = modelClass;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((generatorClass == null) ? 0 : generatorClass.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((modelClass == null) ? 0 : modelClass.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeneratorInfo other = (GeneratorInfo) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (generatorClass == null) {
			if (other.generatorClass != null)
				return false;
		} else if (!generatorClass.equals(other.generatorClass))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (modelClass == null) {
			if (other.modelClass != null)
				return false;
		} else if (!modelClass.equals(other.modelClass))
			return false;
		return true;
	}
	
}
