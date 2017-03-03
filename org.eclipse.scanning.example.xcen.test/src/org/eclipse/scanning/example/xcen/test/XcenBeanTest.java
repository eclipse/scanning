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
package org.eclipse.scanning.example.xcen.test;

import org.eclipse.dawnsci.analysis.dataset.roi.GridROI;
import org.eclipse.dawnsci.analysis.dataset.roi.json.GridROIBean;
import org.eclipse.dawnsci.analysis.dataset.roi.json.ROIBeanFactory;
import org.eclipse.scanning.example.xcen.beans.XcenBean;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class XcenBeanTest {
	
	// Future tests
	// Multiple thread test
	// Submit many runs test (how large can queue get?)
	// 

	
	@Test
	public void deserializeFixedString() throws Exception {
		
		final String str = "{\"status\":\"SUBMITTED\",\"name\":\"Test Xcen\",\"message\":\"A test xcen execution\",\"percentComplete\":0.0,\"userName\":\"fcp94556\",\"hostName\":null,\"runDirectory\":\"C:/tmp\",\"uniqueId\":\"1441796619081_780ede90-6f30-4aaa-bd1b-c7a09fa12319\",\"submissionTime\":1441796619734,\"properties\":null,\"beamline\":\"i04-1\",\"visit\":\"nt5073-40\",\"collection\":\"sapA-x56_A\",\"x\":0.0,\"y\":0.0,\"z\":0.0,\"grids\":null}";
		
		final ObjectMapper mapper = new ObjectMapper();
		
		mapper.readValue(str, XcenBean.class);
	}
	
	@Test
	public void serializeDeserializeTest() throws Exception {
		
		final XcenBean bean = new XcenBean();
		bean.setBeamline("i04-1");
		bean.setVisit("nt5073-40");
		bean.setCollection("sapA-x56_A");
		bean.setName("Test Xcen");
		bean.setMessage("A test xcen execution");
		bean.setRunDirectory("C:/tmp");
		
		final GridROI roi = new GridROI(1, 2, 3, 4, 5, 6, 7, true, true);
        bean.setGrids((GridROIBean)ROIBeanFactory.encapsulate(roi));
        
        
		final ObjectMapper mapper = new ObjectMapper();
        final String       str    = mapper.writeValueAsString(bean);
        
        final Object read = mapper.readValue(str, XcenBean.class);
        
        if (!bean.equals(read)) {
        	throw new Exception("Cannot serialize and deserilize an XcenBean with grids!");
        }
        
	}

}
