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
package org.eclipse.scanning.device.ui.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.expressions.IExpressionEngine;
import org.eclipse.dawnsci.analysis.api.expressions.IExpressionService;
import org.eclipse.richbeans.widgets.decorator.IDecoratorValidator;
import org.eclipse.scanning.api.annotation.ui.FieldUtils;
import org.eclipse.scanning.api.annotation.ui.FieldValue;
import org.eclipse.scanning.device.ui.ServiceHolder;

class ValidIfDecorator implements IDecoratorValidator {
	
	private Object model;
	private String fieldName;
	private String expression;

	public ValidIfDecorator(String fieldName, Object model, String expression) {
		this.model = model;
		this.fieldName = fieldName;
		this.expression = expression;
	}

	public boolean check(String svalue, String delta) {

   		final IExpressionService service = ServiceHolder.getExpressionService();
   		if (service==null) return true;
		try {			
			Number value = parseValue(svalue);
	   		final IExpressionEngine  engine  = service.getExpressionEngine();
	   		engine.createExpression(expression);
	   		
	   		final Map<String, Object>    values = new HashMap<>();
	   		final Collection<FieldValue> fields = FieldUtils.getModelFields(model);
	   		for (FieldValue field : fields) {
	   			Object val = field.get();
	   			if (val instanceof Enum) val = ((Enum)val).name();
	   			values.put(field.getName(), val);
			}
		    values.put(fieldName, value);
	
	   		engine.setLoadedVariables(values);
	   		
	   		return (Boolean)engine.evaluate();
	   		
		} catch (Exception ne) {
			ne.printStackTrace();
			return false;
		}
	}

	public Object getModel() {
		return model;
	}

	public void setModel(Object model) {
		this.model = model;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

}
