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
package org.eclipse.scanning.api.event.scan;

import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.points.IPosition;

public class PositionerRequest extends IdBean {

	private PositionRequestType positionType = PositionRequestType.GET;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3212765978085542676L;

	
	private IPosition position;

	public IPosition getPosition() {
		return position;
	}


	public void setPosition(IPosition position) {
		this.position = position;
	}


	public PositionRequestType getPositionType() {
		return positionType;
	}


	public void setPositionType(PositionRequestType positionType) {
		this.positionType = positionType;
	}
}
