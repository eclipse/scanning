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
package org.eclipse.scanning.test.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.api.INamedNode;
import org.eclipse.scanning.api.ISpringParser;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ui.ControlFileNode;
import org.eclipse.scanning.api.scan.ui.ControlNode;
import org.eclipse.scanning.api.scan.ui.ControlTree;
import org.eclipse.scanning.event.util.JsonUtil;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.server.application.PseudoSpringParser;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.junit.Before;
import org.junit.Test;

public class SerializationTest {

	private IMarshallerService service;

	@Before
	public void create() throws Exception {
		// Non-OSGi for test - do not copy!
				
		service = new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(),
						new ScanningExampleClassRegistry(),
						new ScanningTestClassRegistry()),
				Arrays.asList(new PointsModelMarshaller())
				);
	}
	
	@Test
	public void testSerializeScanBean() throws Exception {
		
		final ScanBean sent = new ScanBean();
		sent.setDeviceName("fred");
		sent.setDeviceState(DeviceState.RUNNING);
		sent.setPreviousDeviceState(DeviceState.READY);
		sent.setPosition(new MapPosition("x", 0, 1.0));
		sent.setUniqueId(UUID.randomUUID().toString());
		sent.setHostName(InetAddress.getLocalHost().getHostName());
		
        String json = service.marshal(sent);
        
        ScanBean ret = service.unmarshal(json, ScanBean.class);
        
        if (!ret.equals(sent)) throw new Exception("Cannot deserialize "+ScanBean.class.getName());
        if (!ret.getPosition().equals(sent.getPosition())) throw new Exception("Cannot deserialize "+ScanBean.class.getName());
	}
	
	@Test
	public void testScanBeanPosition12() throws Exception {
		// ScanBean [deviceName=detector, beamline=null, point=12, size=25, position=[Point: stepIndex=12, yNex(2)=1.5, xNex(2)=1.5], deviceState=RUNNING, previousDeviceState=RUNNING, filePath=null, scanNumber=0, datasetPath=null StatusBean [previousStatus=RUNNING, status=RUNNING, name=null, message=null, percentComplete=52.0, userName=null, hostName=DIAMRL5294, runDirectory=null, submissionTime=0, properties=null, id=1763047e-2f22-4ca1-a5a9-d15b4041578f]]
		// ScanBean [deviceName=detector, beamline=null, point=12, size=25, position=[Point: stepIndex=12, yNex(2)=1.5, xNex(2)=1.5], deviceState=RUNNING, previousDeviceState=RUNNING, filePath=null, scanNumber=0, datasetPath=null StatusBean [previousStatus=RUNNING, status=RUNNING, name=null, message=null, percentComplete=52.0, userName=null, hostName=DIAMRL5294, runDirectory=null, submissionTime=0, properties=null, id=73a05eeb-f8cc-4a78-8819-2a33a0ae1bd7]]
		final ScanBean sent = new ScanBean();
		sent.setDeviceName("detector");
		sent.setPoint(12);
		sent.setSize(25);
		Point pnt = new Point("xNex", 2, 1.5, "yNex", 2, 1.5);
		pnt.setStepIndex(12);
		sent.setPosition(pnt);
		sent.setDeviceState(DeviceState.RUNNING);
		sent.setPreviousDeviceState(DeviceState.RUNNING);
		sent.setUniqueId(UUID.randomUUID().toString());
		sent.setPreviousStatus(Status.RUNNING);
		sent.setStatus(Status.RUNNING);
		sent.setPercentComplete(52);
		sent.setHostName(InetAddress.getLocalHost().getHostName());

        String json = service.marshal(sent);
      
        ScanBean ret = (ScanBean)service.unmarshal(json, Object.class);
        if (!ret.equals(sent)) throw new Exception("Cannot deserialize "+ScanBean.class.getName());
        if (!ret.getPosition().equals(sent.getPosition())) throw new Exception("Cannot deserialize "+ScanBean.class.getName());

	}
	
	@Test
	public void testScanBeanSerializationWithJava() throws Exception {

		ScanBean sent = new ScanBean();
  	    
		// We read a scan bean from serialization file then parse to json
		try(InputStream file = getClass().getResourceAsStream("scanbean.ser");
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer); ){
			
			//deserialize the bean
			sent = (ScanBean)input.readObject();
		} 

        // Check that this bean goes to and from json
        String json = service.marshal(sent);
      
        ScanBean ret = (ScanBean)service.unmarshal(json, Object.class);
        if (!ret.equals(sent)) throw new Exception("Cannot deserialize "+ScanBean.class.getName());

	}

	@Test
	public void testStepSerialize() throws Exception {
		ScanBean bean = createStepScan();
        String   json = service.marshal(bean);
        ScanBean naeb = service.unmarshal(json, null);
        assertEquals(bean, naeb);
	}
	
	@Test
	public void testGridSerialize() throws Exception {
		ScanBean bean = createGridScanWithRegion();
        String   json = service.marshal(bean);
        ScanBean naeb = service.unmarshal(json, null);
        assertEquals(bean, naeb);
	}
	
	@Test
	public void testSerializeBasicPosition1() throws Exception {
		// Create a simple bounding rectangle
		IPosition pos = new MapPosition("x", 0, 1.0);
		String   json = service.marshal(pos);
		IPosition sop = service.unmarshal(json, IPosition.class);
		assertEquals(pos, sop);
	}
	
	@Test
	public void testSerializeBasicPosition2() throws Exception {
		// Create a simple bounding rectangle
		IPosition pos = new MapPosition("Fred:1:0");
		pos.setStepIndex(100);
		String   json = service.marshal(pos);
		IPosition sop = service.unmarshal(json, IPosition.class);
		assertEquals(pos, sop);
	}
	
	@Test
	public void testSerializePositionWithIndices() throws Exception {
		// Create a simple bounding rectangle
		Point point = new Point("x", 100, 0.02, "y", 150, 0.03); 
		point.setStepIndex(100);
		String   json = service.marshal(point);
		IPosition tniop = service.unmarshal(json, IPosition.class);
		assertEquals(point, tniop);
	}


	@Test
	public void testSerializeRegion() throws Exception {
		// Create a simple bounding rectangle
		IROI roi = new RectangularROI(0, 0, 3, 3, 0);
		String   json = service.marshal(roi);
		IROI ior  = service.unmarshal(json, IROI.class);
		assertEquals(roi, ior);
	}

	
	
	private ScanBean createStepScan() throws IOException {
		// We write some pojos together to define the scan
		final ScanBean bean = new ScanBean();
		bean.setName("Hello Scanning World");
		
		final ScanRequest<?> req = new ScanRequest<IROI>();
		req.setCompoundModel(new CompoundModel(new StepModel("fred", 0, 9, 1)));
		req.setMonitorNames(Arrays.asList("monitor", "metadata"));

		final MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setName("detector");
		dmodel.setExposureTime(0.1);
		req.putDetector("detector", dmodel);
		
		bean.setScanRequest(req);
		return bean;
	}
	
	private ScanBean createGridScanWithRegion() throws IOException {
			
		// We write some pojos together to define the scan
		final ScanBean bean = new ScanBean();
		bean.setName("Hello Scanning World");
		
		final ScanRequest<IROI> req = new ScanRequest<IROI>();
		// Create a grid scan model
		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		GridModel gmodel = new GridModel();
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(box);
		gmodel.setFastAxisName("xNex");
		gmodel.setSlowAxisName("yNex");

		IROI roi = new RectangularROI(0, 0, 3, 3, 0);
		req.setCompoundModel(new CompoundModel(gmodel, roi));
		req.setMonitorNames(Arrays.asList("monitor"));
		
		final File tmp = File.createTempFile("scan_servlet_test", ".nxs");
		tmp.deleteOnExit();
		req.setFilePath(tmp.getAbsolutePath()); // TODO This will really come from the scan file service which is not written.
		
		final MandelbrotModel mandyModel = new MandelbrotModel();
		mandyModel.setName("mandelbrot");
		mandyModel.setRealAxisName("xNex");
		mandyModel.setImaginaryAxisName("yNex");
		req.putDetector("mandelbrot", mandyModel);
		
		bean.setScanRequest(req);
		return bean;
	}

	@Test
	public void testCompoundModel1() throws Exception {

		double[] spt = {1, 2};
		double[] ept = {3, 4};
		
		CompoundModel model = new CompoundModel();
		model.setData(new SpiralModel("x", "y", 1, new BoundingBox(0, -5, 10, 5)), new LinearROI(spt, ept));
		
		String   json = service.marshal(model, true); // TODO Should work with false here but does not, see below.

		System.out.println(json);
		// TODO This json uses the @bundle_and_class
		// It is required to have a type: field in the model refering to the simple name of the class
		// and replacing @bundle_and_class which is a java specific thing.
		CompoundModel ledom = service.unmarshal(json, CompoundModel.class);
		
		assertEquals(model, ledom);
	}
	

	@Test
	public void testCompoundModel2() throws Exception {

		CompoundModel model = new CompoundModel();
		
		model.setModelsVarArgs(new StepModel("T", 290, 300, 1), new SpiralModel("x", "y", 1, new BoundingBox(0, -5, 10, 5)), new GridModel("fast", "slow"));
		model.setRegionsVarArgs(new ScanRegion(new CircularROI(2, 0, 0), "x", "y"), new ScanRegion(new RectangularROI(1,2,0), "fast", "slow"));
		
		String   json = service.marshal(model, true); // TODO Should work with false here but does not, see below.

		// TODO This json uses the @bundle_and_class
		// It is required to have a type: field in the model refering to the simple name of the class
		// and replacing @bundle_and_class which is a java specific thing.
		CompoundModel ledom = service.unmarshal(json, CompoundModel.class);
		
		assertEquals(model, ledom);
	}

	@Test
	public void createScanCompoundModel() throws Exception {
		
		// We write some pojos together to define the scan
		final ScanBean bean = new ScanBean();
		bean.setName("Hello Scanning World");
		
		final ScanRequest<IROI> req = new ScanRequest<IROI>();
		// Create a grid scan model
		CompoundModel model = new CompoundModel();
		model.setModelsVarArgs(new StepModel("T", 290, 300, 1), new SpiralModel("x", "y", 1, new BoundingBox(0, -5, 10, 5)), new GridModel("fast", "slow"));
		model.setRegionsVarArgs(new ScanRegion(new CircularROI(2, 0, 0), "x", "y"), new ScanRegion(new RectangularROI(1,2,0), "fast", "slow"));
		req.setCompoundModel(model);
		
		final File tmp = File.createTempFile("scan_servlet_test", ".nxs");
		tmp.deleteOnExit();
		req.setFilePath(tmp.getAbsolutePath()); // TODO This will really come from the scan file service which is not written.
		
		final MandelbrotModel mandyModel = new MandelbrotModel();
		mandyModel.setName("mandelbrot");
		mandyModel.setRealAxisName("xNex");
		mandyModel.setImaginaryAxisName("yNex");
		req.putDetector("mandelbrot", mandyModel);
		
		bean.setScanRequest(req);
		
        String   json = service.marshal(bean, true);
        ScanBean naeb = service.unmarshal(json, null);
        assertEquals(bean, naeb);
	}

	
	@Test
	public void testControlFactorySerialize() throws Exception {
		
		ISpringParser parser = new PseudoSpringParser();
		InputStream in = getClass().getResourceAsStream("client-test.xml");
		parser.parse(in);
		
		assertTrue(!ControlTree.getInstance().isEmpty());
		
		ControlTree.getInstance().build();
		
		String json = service.marshal(ControlTree.getInstance());
		
		assertTrue(json!=null);
		
		ControlTree factory = service.unmarshal(json, ControlTree.class);
		
		assertEquals(factory, ControlTree.getInstance());
	}
	
	@Test
	public void testControlFactorySerialize2() throws Exception {
		
		ControlTree tree = new ControlTree("fred");
		tree.add(new ControlNode("fred", "x", 0.1));
		tree.add(new ControlFileNode("fred", "File"));
		tree.build();
		
		String json = service.marshal(tree);
		
		assertTrue(json!=null);
		
		ControlTree eert = service.unmarshal(json, ControlTree.class);
		eert.build();
	
		assertEquals(eert, tree);
	}


	@Test
	public void testControlFactoryToPosition() throws Exception {
		
		ISpringParser parser = new PseudoSpringParser();
		InputStream in = getClass().getResourceAsStream("client-test.xml");
		parser.parse(in);
		
		ControlTree tree = ControlTree.getInstance();
		assertTrue(!tree.isEmpty());
		
        Iterator<INamedNode> it = tree.iterator();
        while (it.hasNext()) {
			INamedNode iNamedNode = (INamedNode) it.next();
			if (iNamedNode instanceof ControlNode) {
				((ControlNode)iNamedNode).setValue(Math.random());
			}
		}

        tree.build();
        IPosition pos = tree.toPosition();
        assertTrue(pos.size()>0);
        it = tree.iterator();
        while (it.hasNext()) {
			INamedNode iNamedNode = (INamedNode) it.next();
			if (iNamedNode instanceof ControlNode) {
				Object value = ((ControlNode)iNamedNode).getValue();
				assertEquals(value, pos.get(iNamedNode.getName()));
			}
        }
	}

	
	@Test
	public void testRemoveDetector() throws Exception {
	    
		final String json = "{\"@type\":\"ScanBean\","+
		 "\"uniqueId\":\"5f67891f-4f01-48d4-9cab-ce373c5f9807\","+
		 "\"status\":\"SUBMITTED\","+
		 "\"name\":\"Scan [Grid(x, y)] with Detectors [mandelbrot] \","+
		 "\"percentComplete\":0.0,\"userName\":\"fcp94556\",\"hostName\":\"DIAMRL5606\",\"submissionTime\":1474893775913,"+
		 "\"scanRequest\":{\"@type\":\"ScanRequest\","+
		     "\"compoundModel\":{\"@type\":\"CompoundModel\","+
		         "\"models\":[{\"@type\":\"GridModel\",\"name\":\"Grid\",\"boundingBox\":{\"@type\":\"BoundingBox\",\"fastAxisName\":\"x\",\"slowAxisName\":\"y\",\"fastAxisStart\":-84.13637953036218,\"fastAxisLength\":43.356972243563845,\"slowAxisStart\":123.33760426169476,\"slowAxisLength\":42.80505395362201},\"fastAxisName\":\"x\",\"slowAxisName\":\"y\",\"fastAxisPoints\":5,\"slowAxisPoints\":5,\"snake\":false}]},"+
		     "\"detectors\":{\"mandelbrot\":{\"@type\":\"MandelbrotModel\",\"maxIterations\":500,\"escapeRadius\":10.0,\"columns\":301,\"rows\":241,\"points\":1000,\"maxRealCoordinate\":1.5,\"maxImaginaryCoordinate\":1.2,\"realAxisName\":\"x\",\"imaginaryAxisName\":\"y\",\"name\":\"mandelbrot\",\"exposureTime\":0.1,\"timeout\":-1}},"+
		     "\"ignorePreprocess\":false},"+
		  "\"point\":0,\"size\":0,\"scanNumber\":0}";			

		ScanBean bean = service.unmarshal(json, ScanBean.class);
		assertTrue(bean.getScanRequest().getDetectors().size()>0);
		
		assertTrue(json.indexOf("\"detectors\":{")>0);
		assertTrue(json.indexOf("\"uniqueId\":")>0);
		assertTrue(json.indexOf("\"status\":")>0);
		assertTrue(json.indexOf("\"scanRequest\":{")>0);
		assertTrue(json.indexOf("\"compoundModel\":{")>0);
		assertTrue(json.indexOf("\"models\":")>0);
		assertTrue(json.indexOf("\"point\":0")>0);

		String jsonNoDet = JsonUtil.removeProperties(json, Arrays.asList("detectors"));
		assertTrue(jsonNoDet.indexOf("\"detectors\":{")<0);
		assertTrue(jsonNoDet.indexOf("\"uniqueId\":")>0);
		assertTrue(jsonNoDet.indexOf("\"status\":")>0);
		assertTrue(jsonNoDet.indexOf("\"scanRequest\":{")>0);
		assertTrue(jsonNoDet.indexOf("\"compoundModel\":{")>0);
		assertTrue(jsonNoDet.indexOf("\"models\":")>0);
		assertTrue(jsonNoDet.indexOf("\"point\":0")>0);
		assertTrue(jsonNoDet.endsWith("\"point\":0,\"size\":0,\"scanNumber\":0}"));
		assertTrue(jsonNoDet.indexOf(",,")<0);
		
		bean = service.unmarshal(jsonNoDet, ScanBean.class);
		assertTrue(bean.getScanRequest().getDetectors()==null);
	}
	
	
	@Test
	public void testRemoveScanRequest() throws Exception {
		
		final String json = "{\"@type\":\"ScanBean\","+
		 "\"uniqueId\":\"5f67891f-4f01-48d4-9cab-ce373c5f9807\","+
		 "\"status\":\"SUBMITTED\","+
		 "\"name\":\"Scan [Grid(x, y)] with Detectors [mandelbrot] \","+
		 "\"percentComplete\":0.0,\"userName\":\"fcp94556\",\"hostName\":\"DIAMRL5606\",\"submissionTime\":1474893775913,"+
		 "\"scanRequest\":{\"@type\":\"ScanRequest\","+
		     "\"compoundModel\":{\"@type\":\"CompoundModel\","+
		         "\"models\":[{\"@type\":\"GridModel\",\"name\":\"Grid\",\"boundingBox\":{\"@type\":\"BoundingBox\",\"fastAxisName\":\"x\",\"slowAxisName\":\"y\",\"fastAxisStart\":-84.13637953036218,\"fastAxisLength\":43.356972243563845,\"slowAxisStart\":123.33760426169476,\"slowAxisLength\":42.80505395362201},\"fastAxisName\":\"x\",\"slowAxisName\":\"y\",\"fastAxisPoints\":5,\"slowAxisPoints\":5,\"snake\":false}]},"+
		     "\"detectors\":{\"mandelbrot\":{\"@type\":\"MandelbrotModel\",\"maxIterations\":500,\"escapeRadius\":10.0,\"columns\":301,\"rows\":241,\"points\":1000,\"maxRealCoordinate\":1.5,\"maxImaginaryCoordinate\":1.2,\"realAxisName\":\"x\",\"imaginaryAxisName\":\"y\",\"name\":\"mandelbrot\",\"exposureTime\":0.1,\"timeout\":-1}},"+
		     "\"ignorePreprocess\":false},"+
		  "\"point\":0,\"size\":0,\"scanNumber\":0}";			
	    
		ScanBean bean = service.unmarshal(json, ScanBean.class);
		assertTrue(bean.getScanRequest()!=null);

		String jsonNoReq = JsonUtil.removeProperties(json, Arrays.asList("scanRequest"));
		
		bean = service.unmarshal(jsonNoReq, ScanBean.class);
		assertTrue(bean.getScanRequest()==null);
	}

}
