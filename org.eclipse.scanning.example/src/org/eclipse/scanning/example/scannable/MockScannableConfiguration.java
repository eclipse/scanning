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
package org.eclipse.scanning.example.scannable;

/**
 * 
 * See http://confluence.diamond.ac.uk/pages/viewpage.action?pageId=37814632
 * 
 <pre>
 primary_slit:NXslit
    x_gap = {NX_NUMBER}
        @local_name = "s1gapX"
        @units = "mm"
        @controller_record = {NX_CHAR} //EPICS name
    y_gap = {NX_NUMBER}
        @local_name = "s1gapY"
        @units = "mm"
        @controller_record = {NX_CHAR} //EPICS name
    transforms:NXtransformations
        x_centre = {NX_NUMBER}
            @transformation_type = "translation"
            @vector = 1,0,0,
            @depends_on = "y_centre"
            @units = "mm"
            @controller_record = {NX_CHAR} //EPICS name
        y_centre = {NX_NUMBER}
            @transformation_type = "translation"
            @vector = 0,1,0,
            @depends_on = "."
            @offset = 0,0,-14500 //This may change
            @units = "mm"
            @controller_record = {NX_CHAR} //EPICS name
    beam:NXbeam
        //Removed distance from here, since it's in the "y_centre" above
        incident_beam_divergence[2,i] = {NX_FLOAT}
            @units = "radians"
        final_beam_divergence[2,i] = {NX_FLOAT}
            @units = "radians"
    motors:NXcollection
        downstream_x:NXpositioner
            name = "s1dsX"
            description = "Downstream X position"
            value = {NX_NUMBER}
                @units = "mm"
            controller_record = {NX_CHAR} //EPICS name
        downstream_y:NXpositioner
            name = "s1dsY"
            description = "Downstream Y position"
            value = {NX_NUMBER}
                @units = "mm"
            controller_record = {NX_CHAR} //EPICS name
        upstream_x:NXpositioner
            name = "s1usX"
            description = "Upstream X position"
            value = {NX_NUMBER}
                @units = "mm"
            controller_record = {NX_CHAR} //EPICS name
        upstream_y:NXpositioner
            name = "s1usY"
            description = "Upstream Y position"
            value = {NX_NUMBER}
                @units = "mm"
            controller_record = {NX_CHAR} //EPICS name
 </pre>
 
 * 
 * @author Matthew Gerring
 *
 */
public class MockScannableConfiguration {

	private String xGapName, yGapName, xCentreName, yCentreName;

    public MockScannableConfiguration() {
    	
    }
    
	public MockScannableConfiguration(String xGapName, String yGapName, String xCentreName, String yCentreName) {
		this.xGapName    = xGapName;
		this.yGapName    = yGapName;
		this.xCentreName = xCentreName;
		this.yCentreName = yCentreName;
	}

	public String getXGapName() {
		return xGapName;
	}

	public void setXGapName(String xGapName) {
		this.xGapName = xGapName;
	}

	public String getYGapName() {
		return yGapName;
	}

	public void setYGapName(String yGapName) {
		this.yGapName = yGapName;
	}

	public String getXCentreName() {
		return xCentreName;
	}

	public void setXCentreName(String xCentreName) {
		this.xCentreName = xCentreName;
	}

	public String getYCentreName() {
		return yCentreName;
	}

	public void setYCentreName(String yCentreName) {
		this.yCentreName = yCentreName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((xCentreName == null) ? 0 : xCentreName.hashCode());
		result = prime * result + ((xGapName == null) ? 0 : xGapName.hashCode());
		result = prime * result + ((yCentreName == null) ? 0 : yCentreName.hashCode());
		result = prime * result + ((yGapName == null) ? 0 : yGapName.hashCode());
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
		MockScannableConfiguration other = (MockScannableConfiguration) obj;
		if (xCentreName == null) {
			if (other.xCentreName != null)
				return false;
		} else if (!xCentreName.equals(other.xCentreName))
			return false;
		if (xGapName == null) {
			if (other.xGapName != null)
				return false;
		} else if (!xGapName.equals(other.xGapName))
			return false;
		if (yCentreName == null) {
			if (other.yCentreName != null)
				return false;
		} else if (!yCentreName.equals(other.yCentreName))
			return false;
		if (yGapName == null) {
			if (other.yGapName != null)
				return false;
		} else if (!yGapName.equals(other.yGapName))
			return false;
		return true;
	}
	
}
