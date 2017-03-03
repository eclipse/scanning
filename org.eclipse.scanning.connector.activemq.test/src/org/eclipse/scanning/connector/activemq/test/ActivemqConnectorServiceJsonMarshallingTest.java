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
package org.eclipse.scanning.connector.activemq.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Regression tests to ensure JSON marshalling still works correctly now it is delegated to another service
 *
 * @author Colin Palmer
 *
 */
public class ActivemqConnectorServiceJsonMarshallingTest {

	// An example of a bean used by Xia2 which could be sent by another process and must deserialize correctly in current version
	private static final String JSON_FOR_PROJECT_BEAN = "{\"status\":\"COMPLETE\",\"name\":\"X1_weak_M1S1_1 - X1_weak_M1S1_1\",\"message\":\"Xia2 run completed normally\",\"percentComplete\":100.0,\"userName\":\"awa25\",\"hostName\":\"cs04r-sc-vserv-45.diamond.ac.uk\",\"runDirectory\":\"/dls/i03/data/2016/cm14451-1/processed/tmp/2016-01-27/fake085224/MultiCrystal_1\",\"uniqueId\":\"1453910139320_94ed2a2b-997e-4dbc-ad6e-0c3c04bb2c82\",\"submissionTime\":1453910139340,\"properties\":null,\"projectName\":\"MultiCrystalRerun\",\"cystalName\":\"fake085224\",\"sweeps\":[{\"name\":\"X1_weak_M1S1_1\",\"sessionId\":\"55167\",\"dataCollectionId\":\"1007379\",\"imageDirectory\":\"/dls/i03/data/2016/cm14451-1/tmp/2016-01-27/fake085224/\",\"firstImageName\":\"X1_weak_M1S1_1_0001.cbf\",\"start\":1,\"end\":900,\"wavelength\":0.979493,\"xBeam\":212.51,\"yBeam\":219.98,\"resolution\":null}],\"wavelength\":\"NaN\",\"commandLineSwitches\":\"\",\"anomalous\":true,\"spaceGroup\":null,\"unitCell\":null,\"resolution\":null}";

	private static final String JSON_FOR_WRAPPED_RECTANGULAR_ROI_LIST = "{\"object\":[\"ArrayList\",[{\"@type\":\"roi.rectangular\",\"lengths\":[8.0,6.1],\"angle\":0.0,\"point\":[-3.5,4.0]}]]}";

	private static final String[] STRING_ARRAY = { "a", "b", "c" };
	private static final String JSON_FOR_STRING_ARRAY = "[\"String[]\",[\"a\",\"b\",\"c\"]]";
	private static final String JSON_FOR_WRAPPED_STRING_ARRAY = "{\"object\":[\"String[]\",[\"a\",\"b\",\"c\"]]}";

	private ActivemqConnectorService marshaller;
	private String json;

	@Before
	public void setUp() throws Exception {
		MarshallerService marshaller = new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry()),
				Arrays.asList(new PointsModelMarshaller()));
		
		ActivemqConnectorService.setJsonMarshaller(marshaller);
		this.marshaller = new ActivemqConnectorService();
	}

	@After
	public void tearDown() throws Exception {
		if (json != null) {
			// So we can see what's going on
//			System.out.println("JSON: " + json);

			// To make it easy to replace expected JSON values in the code when we're sure they're correct
			@SuppressWarnings("unused")
			String javaLiteralForJSONString = '"' + StringEscapeUtils.escapeJava(json) + '"';
//			System.out.println("Java literal:\n" + javaLiteralForJSONString);
		}
		json = null;
		marshaller = null;
		ActivemqConnectorService.setJsonMarshaller(null);
	}

	@Test(expected = NullPointerException.class)
	public void testExceptionThrownFromMarshalWhenJsonMarshallerNotSet() throws Exception {
		ActivemqConnectorService.setJsonMarshaller(null);
		marshaller.marshal(STRING_ARRAY);
	}

	@Test(expected = NullPointerException.class)
	public void testExceptionThrownFromUnmarshalWhenJsonMarshallerNotSet() throws Exception {
		ActivemqConnectorService.setJsonMarshaller(null);
		marshaller.unmarshal(JSON_FOR_STRING_ARRAY, String[].class);
	}

	@Test
	public void testProjectBeanDeserialization() throws Exception {
		marshaller.unmarshal(JSON_FOR_PROJECT_BEAN, ProjectBean.class);
	}

	@Test(expected = JsonMappingException.class)
	public void testProjectBeanDeserializationWithWrongType() throws Exception {
		marshaller.unmarshal(JSON_FOR_PROJECT_BEAN, StatusBean.class);
	}

	// TODO	remove this - just for experimenting!
	@Test
	public void testProjectBeanSerialization() throws Exception {
		ProjectBean bean = marshaller.unmarshal(JSON_FOR_PROJECT_BEAN, ProjectBean.class);
		json = marshaller.marshal(bean);
	}

	@Test
	public void testROISerialization() throws Exception {
		IROI roi = new RectangularROI(-3.5, 4.0, 8.0, 6.1, 0.0);
		json = marshaller.marshal(roi);
	}

	@Test
	public void testROIDeserialization() throws Exception {
		String roi = marshaller.marshal(new RectangularROI(-3.5, 4.0, 8.0, 6.1, 0.0));
		IROI actual = marshaller.unmarshal(roi, IROI.class);
		IROI expected = new RectangularROI(-3.5, 4.0, 8.0, 6.1, 0.0);
		assertEquals(expected, actual);
	}

	@Test
	public void testROIFieldSerialization() throws Exception {
		IROI roi = new RectangularROI(-3.5, 4.0, 8.0, 6.1, 0.0);
		ObjectWrapper<IROI> roiWrapper = new ObjectWrapper<>(roi);
		json = marshaller.marshal(roiWrapper);
		assertTrue(json.startsWith("{\"object\":{\"@type\":\"roi.rectangular\""));
	}

	@Test(expected=Exception.class) // We cannot deal with this yet
	public void testROIListFieldSerialization() throws Exception {
		IROI roi = new RectangularROI(-3.5, 4.0, 8.0, 6.1, 0.0);
		ObjectWrapper<List<IROI>> roiWrapper = new ObjectWrapper<>(Arrays.asList(roi));
		json = marshaller.marshal(roiWrapper);
		assertEquals(JSON_FOR_WRAPPED_RECTANGULAR_ROI_LIST, json);
	}

	@Test
	public void testStringArraySerialization() throws Exception {
		json = marshaller.marshal(STRING_ARRAY);
		assertEquals(JSON_FOR_STRING_ARRAY, json);
	}

	@Test
	public void testStringArrayDeserialization() throws Exception {
		String[] actual = marshaller.unmarshal(JSON_FOR_STRING_ARRAY, String[].class);
		assertArrayEquals(actual, STRING_ARRAY);
	}

	@Test
	public void testSimpleStringArrayDeserialization() throws Exception {
		String[] actual = marshaller.unmarshal(JSON_FOR_STRING_ARRAY, String[].class);
		assertArrayEquals(actual, STRING_ARRAY);
	}

	@Test
	public void testStringArrayFieldSerialization() throws Exception {
		ObjectWrapper<String[]> arrayWrapper = new ObjectWrapper<>(STRING_ARRAY);
		json = marshaller.marshal(arrayWrapper);
		assertEquals(JSON_FOR_WRAPPED_STRING_ARRAY, json);
	}

	@Test
	public void testStringArrayFieldDeserialization() throws Exception {
		ObjectWrapper<String[]> expected = new ObjectWrapper<>(STRING_ARRAY);
		ObjectWrapper<?> actual = marshaller.unmarshal(JSON_FOR_WRAPPED_STRING_ARRAY, ObjectWrapper.class);
		assertArrayEquals(expected.getObject(), (Object[]) actual.getObject());
	}

	@Test
	public void testObjectArrayDeserialization() throws Exception {
		Object[] actual = marshaller.unmarshal("[ \"a\", \"b\", 5 ]", Object[].class);
		assertArrayEquals(actual, new Object[] { "a", "b", 5 });
	}

	@Test
	public void testSimpleMapDeserialization() throws Exception {
		@SuppressWarnings("unused")
		Map<?,?> map = marshaller.unmarshal("{ \"String key\" : \"String value\", \"Int key\" : 5 }", Map.class);
	}
}

class ObjectWrapper<T> {
	T object;
	public ObjectWrapper() {
	}
	public ObjectWrapper(T object) {
		this.object = object;
	}
	public T getObject() {
		return object;
	}
	public void setObject(T object) {
		this.object = object;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object == null) ? 0 : object.hashCode());
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
		ObjectWrapper<?> other = (ObjectWrapper<?>) obj;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		return true;
	}
}