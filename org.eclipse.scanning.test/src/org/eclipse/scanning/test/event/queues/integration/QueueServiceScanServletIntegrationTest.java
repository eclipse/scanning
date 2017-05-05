package org.eclipse.scanning.test.event.queues.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.scan.IScanListener;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanEvent;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.script.IScriptService;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
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
		
		ServicesHolder.setEventService(eservice);
		RealQueueTestUtils.initialise(uri);
	}
	
	@Before
	public void before() throws Exception {
		servlet = createServlet();
		if (servlet!=null) {
			servlet.getConsumer().cleanQueue(servlet.getSubmitQueue());
			servlet.getConsumer().cleanQueue(servlet.getStatusSet());
		}
	}
	
	@After
	public void disconnect()  throws Exception {
		if (servlet!=null) {
			servlet.getConsumer().cleanQueue(servlet.getSubmitQueue());
			servlet.getConsumer().cleanQueue(servlet.getStatusSet());
			servlet.disconnect();
		}
		
		RealQueueTestUtils.reset();
	}

	protected AbstractConsumerServlet<ScanBean> createServlet() throws EventException, URISyntaxException {
		ScanServlet servlet = new ScanServlet();
		servlet.setBroker(uri.toString());
		servlet.setPauseOnStart(false);
		servlet.connect(); // Gets called by Spring automatically

		return servlet;
	}
	@Test
	public void testStepScan() throws Exception {
		// We write some pojos together to define the scan
		final ScanBean bean = new ScanBean();
		bean.setName("Hello Scanning World");

		final ScanRequest<?> req = new ScanRequest<>();
		req.setCompoundModel(new CompoundModel(new StepModel("fred", 0, 9, 1)));
		req.setMonitorNames(Arrays.asList("monitor"));

		final MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setName("detector");
		dmodel.setExposureTime(0.001);
		req.putDetector("detector", dmodel);

		bean.setScanRequest(req);
		
		final List<ScanBean> startEvents = new ArrayList<>();
		final List<ScanBean> endEvents   = new ArrayList<>(13);
		CountDownLatch latch = RealQueueTestUtils.createScanEndEventWaitLatch(bean, servlet.getStatusTopic());
		
		final ISubmitter<ScanBean> submitter  = eservice.createSubmitter(uri,  servlet.getSubmitQueue());
		submitter.submit(bean);
		submitter.disconnect();
		
		RealQueueTestUtils.waitForEvent(latch, 60000);
//		boolean ok = latch.await(60, TimeUnit.SECONDS);
//		if (!ok) throw new Exception("The latch broke before the scan finished!");
		
		assertEquals(1, RealQueueTestUtils.getStartEvents().size());
		assertTrue(RealQueueTestUtils.getEndEvents().size() > 0);
	}
	
//	protected CountDownLatch runAndCheck(ScanBean bean, List<ScanBean> startEvents, List<ScanBean> endEvents) throws Exception {
//
//		// Let's listen to the scan and see if things happen when we run it
//		final ISubscriber<IScanListener> subscriber = eservice.createSubscriber(new URI(servlet.getBroker()), servlet.getStatusTopic());
//		
//			final CountDownLatch latch       = new CountDownLatch(1);
//			
//			subscriber.addListener(new IScanListener() {
//
//				@Override
//				public void scanStateChanged(ScanEvent evt) {
//					if (evt.getBean().scanStart()) {
//						startEvents.add(evt.getBean()); // Should be just one
//					}
//	                if (evt.getBean().scanEnd()) {
//	                	endEvents.add(evt.getBean());
//	                	latch.countDown();
//	                }
//				}
//			});
//
//			return latch;
//	}

}
