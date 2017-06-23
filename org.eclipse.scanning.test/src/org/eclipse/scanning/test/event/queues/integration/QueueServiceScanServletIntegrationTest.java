package org.eclipse.scanning.test.event.queues.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.dawnsci.nexus.builder.impl.DefaultNexusBuilderFactory;
import org.eclipse.dawnsci.remotedataset.test.mock.LoaderServiceMock;
import org.eclipse.scanning.api.device.IDeviceWatchdogService;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.script.IScriptService;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.event.queues.QueueService;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDevice;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.ScanPointGeneratorFactory;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.points.validation.ValidatorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.eclipse.scanning.sequencer.watchdog.DeviceWatchdogService;
import org.eclipse.scanning.server.servlet.AbstractConsumerServlet;
import org.eclipse.scanning.server.servlet.ScanServlet;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.eclipse.scanning.test.event.queues.RealQueueTestUtils;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandelbrotDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test is, in the first instance, heavily based on the ScanServlet test:
 * o.e.s.t.scan.servlet.ScanServletTest
 * @author wnm24546
 *
 */
public class QueueServiceScanServletIntegrationTest extends BrokerTest {
	
	//These are the fields to run the scan servlet
	protected static IRunnableDeviceService      dservice;
	protected static IScannableDeviceService     connector;
	protected static IPointGeneratorService      gservice;
	protected static IEventService               eservice;
	protected static ILoaderService              lservice;
	protected static IDeviceWatchdogService      wservice;
	protected static IScriptService              sservice;
	protected static MarshallerService           marshaller;
	protected static ValidatorService            validator;
	
	protected static AbstractConsumerServlet<?> servlet;
	
	//These fields are for the queueservice
	protected static QueueService     qservice;
	
	@BeforeClass
	public static void create() throws Exception {
		
		ScanPointGeneratorFactory.init();

		marshaller = new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(),
						new ScanningExampleClassRegistry(),
						new ScanningTestClassRegistry()),
				Arrays.asList(new PointsModelMarshaller())
				);
		ActivemqConnectorService.setJsonMarshaller(marshaller);
		eservice  = new EventServiceImpl(new ActivemqConnectorService());
		
		// We wire things together without OSGi here
		// DO NOT COPY THIS IN NON-TEST CODE
		connector = new MockScannableConnector(null);
		dservice  = new RunnableDeviceServiceImpl(connector);
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)dservice;
		impl._register(MockDetectorModel.class, MockWritableDetector.class);
		impl._register(MockWritingMandlebrotModel.class, MockWritingMandelbrotDetector.class);
		impl._register(MandelbrotModel.class, MandelbrotDetector.class);
		impl._register(DummyMalcolmModel.class, DummyMalcolmDevice.class);
		
		final MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setName("detector");
		dmodel.setExposureTime(0.001);
		impl.createRunnableDevice(dmodel);

		MandelbrotModel model = new MandelbrotModel("p", "q");
		model.setName("mandelbrot");
		model.setExposureTime(0.001);
		impl.createRunnableDevice(model);

		gservice  = new PointGeneratorService();
		wservice = new DeviceWatchdogService();
		lservice = new LoaderServiceMock();
		sservice = new MockScriptService();
		
		// Provide lots of services that OSGi would normally.
		Services.setEventService(eservice);
		Services.setRunnableDeviceService(dservice);
		Services.setGeneratorService(gservice);
		Services.setConnector(connector);
		Services.setScriptService(sservice);
		Services.setWatchdogService(wservice);

		ServiceHolder.setTestServices(lservice, new DefaultNexusBuilderFactory(), null, null, gservice);
		org.eclipse.scanning.example.Services.setPointGeneratorService(gservice);
		org.eclipse.dawnsci.nexus.ServiceHolder.setNexusFileFactory(new NexusFileFactoryHDF5());
		
		validator = new ValidatorService();
		validator.setPointGeneratorService(gservice);
		validator.setRunnableDeviceService(dservice);
		Services.setValidatorService(validator);
		
		//Set up the queue service (normally populated by OSGi)
		qservice = new QueueService(uri.toString());
		
		ServicesHolder.setEventService(eservice);
		ServicesHolder.setQueueService(qservice);
		ServicesHolder.setQueueControllerService(qservice);
		RealQueueTestUtils.initialise(uri);
		
	}
	
	@Before
	public void before() throws Exception {
		servlet = createServlet();
		if (servlet!=null) {
			servlet.getConsumer().cleanQueue(servlet.getSubmitQueue());
			servlet.getConsumer().cleanQueue(servlet.getStatusSet());
		}
		
		//Start QueueService
		qservice.startQueueService();
	}
	
	@After
	public void disconnect() throws Exception {
		qservice.stopQueueService(true);
		
		if (servlet!=null) {
			servlet.getConsumer().cleanQueue(servlet.getSubmitQueue());
			servlet.getConsumer().cleanQueue(servlet.getStatusSet());
			servlet.disconnect();
		}
		
		RealQueueTestUtils.reset();
	}
	
	@AfterClass
	public static void shutdown() throws Exception {
		qservice.disposeService();
	}

	protected AbstractConsumerServlet<ScanBean> createServlet() throws EventException, URISyntaxException {
		ScanServlet servlet = new ScanServlet();
		servlet.setBroker(uri.toString());
		servlet.setPauseOnStart(false);
		servlet.connect(); // Gets called by Spring automatically

		return servlet;
	}
	
//	@Test
	public void testMove() throws Exception {
		//Some sort of latch here
		CountDownLatch latch = null; //Make one
		
		//Set up PositionerAtom
		PositionerAtom mvAt = new PositionerAtom("testMove", null, null); //TODO change me!!

		//... and an enclosing SubTaskAtom...
		SubTaskAtom stAt = new SubTaskAtom(null, "testSubTask");
		stAt.addAtom(mvAt);

		//... and an enclosing TaskBean
		TaskBean tBean = new TaskBean(null, "testTask");
		tBean.addAtom(stAt);

		//Submit it and wait!
		String jqID = qservice.getJobQueueID();
		qservice.submit(tBean, jqID);
		RealQueueTestUtils.waitForEvent(latch, 60000);
		
		/* These should test the move happened
		 * assertEquals(1, RealQueueTestUtils.getStartEvents().size());
		 * assertTrue(RealQueueTestUtils.getEndEvents().size() > 0); 
		 */
		
	}
	
	@Test
	public void testStepScan() throws Exception {
		//Set up ScanAtom...
		final List<IScanPathModel> paths = Arrays.asList(new IScanPathModel[]{new StepModel("fred", 0, 9, 1)});
		final MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setName("detector");
		dmodel.setExposureTime(0.001);
		Map<String, Object> detectors = new HashMap<>();
		detectors.put("detector", dmodel);
		ScanAtom scAt = new ScanAtom("testScan", paths, detectors, null);
		
		//... and an enclosing SubTaskAtom...
		SubTaskAtom stAt = new SubTaskAtom(null, "testSubTask");
		stAt.addAtom(scAt);
		
		//... and an enclosing TaskBean
		TaskBean tBean = new TaskBean(null, "testTask");
		tBean.addAtom(stAt);
		
		//Create latches to wait for activity
		CountDownLatch scanLatch = RealQueueTestUtils.createScanEndEventWaitLatch(servlet.getStatusTopic());
		CountDownLatch queueLatch = RealQueueTestUtils.createFinalStateBeanWaitLatch(tBean, qservice.getJobQueueID());
		
		//Submit it and wait!
		String jqID = qservice.getJobQueueID();
		qservice.submit(tBean, jqID);
		RealQueueTestUtils.waitForEvent(scanLatch, 60000);
	
		assertEquals(1, RealQueueTestUtils.getStartEvents().size());
		assertTrue(RealQueueTestUtils.getEndEvents().size() > 0);
		
		RealQueueTestUtils.waitForEvent(queueLatch);
	}
	
}
