package org.eclipse.scanning.test.command;

import static org.eclipse.scanning.sequencer.analysis.ClusterProcessingRunnableDevice.PROCESSING_QUEUE_NAME;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.builder.impl.DefaultNexusBuilderFactory;
import org.eclipse.dawnsci.remotedataset.test.mock.LoaderServiceMock;
import org.eclipse.scanning.api.device.IDeviceWatchdogService;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.models.ClusterProcessingModel;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.scan.IScanListener;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanEvent;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.file.MockFilePathService;
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
import org.eclipse.scanning.sequencer.analysis.ClusterProcessingRunnableDevice;
import org.eclipse.scanning.sequencer.watchdog.DeviceWatchdogService;
import org.eclipse.scanning.server.servlet.ScanServlet;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.eclipse.scanning.test.scan.mock.DummyOperationBean;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockOperationService;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandelbrotDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.eclipse.scanning.test.scan.nexus.ScanClusterProcessingChecker;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class AbstractMScanTest extends AbstractJythonTest {
	
	protected static ScanServlet servlet; 
	
	/**
	 * Fake processing consumer or null if no processing
	 */
	protected static IConsumer<StatusBean>       pconsumer;

	
	@BeforeClass
	public static void init() {
		ScanPointGeneratorFactory.init();
	}
	
	protected static IRunnableDeviceService      dservice;
	protected static IScannableDeviceService     connector;
	protected static IPointGeneratorService      gservice;
	protected static IEventService               eservice;
	protected static ILoaderService              lservice;
	protected static IDeviceWatchdogService      wservice;
	protected static ValidatorService            validator;
	protected static INexusFileFactory           fileFactory;

	@BeforeClass
	public static void create() throws Exception {
		
		createMarshaller();
		eservice  = new EventServiceImpl(new ActivemqConnectorService());
		
		// We wire things together without OSGi here
		// DO NOT COPY THIS IN NON-TEST CODE
		connector = new MockScannableConnector(null);
		dservice  = new RunnableDeviceServiceImpl(connector);
		fileFactory = new NexusFileFactoryHDF5();		

		
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)dservice;
		impl._register(MockDetectorModel.class, MockWritableDetector.class);
		impl._register(MockWritingMandlebrotModel.class, MockWritingMandelbrotDetector.class);
		impl._register(MandelbrotModel.class, MandelbrotDetector.class);
		impl._register(DummyMalcolmModel.class, DummyMalcolmDevice.class);
		impl._register(ClusterProcessingModel.class, ClusterProcessingRunnableDevice.class);
		
		final MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setName("detector");
		dmodel.setExposureTime(0.1);
		impl.createRunnableDevice(dmodel);

		MandelbrotModel model = new MandelbrotModel("xNex", "yNex");
		model.setName("mandelbrot");
		model.setExposureTime(0.00001);
		impl.createRunnableDevice(model);
		
		ClusterProcessingModel cmodel = new ClusterProcessingModel();
		cmodel.setDetectorName(null); // Intentionally not one
		cmodel.setName("processing");
		cmodel.setProcessingFilePath(null);
		impl.createRunnableDevice(cmodel);
		
		model = new MandelbrotModel("xNex", "yNex");
		model.setName("m");
		model.setExposureTime(0.00001);
		impl.createRunnableDevice(model);
		
		cmodel = new ClusterProcessingModel();
		cmodel.setDetectorName(null); // Intentionally not one
		cmodel.setName("p");
		cmodel.setProcessingFilePath(null);
		impl.createRunnableDevice(cmodel);

		gservice  = new PointGeneratorService();
		wservice = new DeviceWatchdogService();
		lservice = new LoaderServiceMock();
	
		// Provide lots of services that OSGi would normally.
		Services.setEventService(eservice);
		ServiceHolder.setEventService(eservice);
		org.eclipse.scanning.command.Services.setEventService(eservice);
		Services.setRunnableDeviceService(dservice);
		org.eclipse.scanning.command.Services.setRunnableDeviceService(dservice);
		Services.setGeneratorService(gservice);
		Services.setConnector(connector);
		Services.setWatchdogService(wservice);

		org.eclipse.scanning.sequencer.ServiceHolder.setTestServices(lservice,
				                                                     new DefaultNexusBuilderFactory(), 
				                                                     new MockOperationService(), 
				                                                     new MockFilePathService(), gservice);
	
		org.eclipse.scanning.example.Services.setPointGeneratorService(gservice);
		org.eclipse.dawnsci.nexus.ServiceHolder.setNexusFileFactory(new NexusFileFactoryHDF5());
		
		validator = new ValidatorService();
		validator.setPointGeneratorService(gservice);
		validator.setRunnableDeviceService(dservice);
		Services.setValidatorService(validator);
		
		
		// Create an object for the servlet
		/**
		 *  This would be done by spring on the GDA Server
		 *  @see org.eclipse.scanning.server.servlet.AbstractConsumerServlet
		 *  In spring we have something like:

		    {@literal <bean id="scanner" class="org.eclipse.scanning.server.servlet.ScanServlet">}
		    {@literal    <property name="broker"      value="tcp://p45-control:61616" />}
		    {@literal    <property name="submitQueue" value="uk.ac.diamond.p45.submitQueue" />}
		    {@literal    <property name="statusSet"   value="uk.ac.diamond.p45.statusSet"   />}
		    {@literal    <property name="statusTopic" value="uk.ac.diamond.p45.statusTopic" />}
		    {@literal </bean>}

		 */
		servlet = new ScanServlet();
		servlet.setBroker(uri.toString());
		servlet.setDurable(true);
		servlet.connect(); // Gets called by Spring automatically
		
	}

	@AfterClass
	public static void disconnect()  throws Exception {
		servlet.getConsumer().cleanQueue(servlet.getSubmitQueue());
		servlet.getConsumer().cleanQueue(servlet.getStatusSet());
		servlet.disconnect();
	}

	protected String path;
	private boolean requireFile;
	
	public AbstractMScanTest(boolean requireFile) {
		this.requireFile = requireFile;
	}
	
	@Before
	public void before() throws Exception {
		
		if (requireFile) {
			File output = File.createTempFile("test_nexus", ".nxs");
			output.deleteOnExit();
			path = output.getAbsolutePath().replace("\\\\", "\\").replace('\\', '/');
		}
		
		servlet.getConsumer().cleanQueue(servlet.getSubmitQueue());
		servlet.getConsumer().cleanQueue(servlet.getStatusSet());
		
		if (pconsumer!=null) {
			pconsumer.cleanQueue(PROCESSING_QUEUE_NAME);
			pconsumer.cleanQueue("scisoft.operation.STATUS_SET");
		}
	}

	protected List<ScanBean> runAndCheck(String name, boolean blocking, long maxScanTimeS) throws Exception {
		
		final IEventService eservice = Services.getEventService();

		// Let's listen to the scan and see if things happen when we run it
		final ISubscriber<IScanListener> subscriber = eservice.createSubscriber(new URI(servlet.getBroker()), servlet.getStatusTopic());
		final ISubmitter<ScanBean>       submitter  = eservice.createSubmitter(new URI(servlet.getBroker()),  servlet.getSubmitQueue());
		
		try {
			final List<ScanBean> beans = new ArrayList<>(13);
			final List<ScanBean> failed = new ArrayList<>(13);
			final List<ScanBean> startEvents = new ArrayList<>(13);
			
			final CountDownLatch latch = new CountDownLatch(1);
			subscriber.addListener(new IScanListener() {
				@Override
				public void scanEventPerformed(ScanEvent evt) {
					if (evt.getBean().getStatus()==Status.FAILED) failed.add(evt.getBean());
					if (evt.getBean().getPosition()!=null) {
						beans.add(evt.getBean());
					}
				}
	
				@Override
				public void scanStateChanged(ScanEvent evt) {
					if (evt.getBean().getStatus()==Status.FAILED) failed.add(evt.getBean());
					if (evt.getBean().scanStart()) {
						startEvents.add(evt.getBean()); // Should be just one
					}
	                if (evt.getBean().scanEnd()) {
	                	latch.countDown();
	                }
				}
			});
	
			
			// Ok done that, now we sent it off...
			pi.exec("submit("+name+", block="+(blocking?"True":"False")+", broker_uri='"+uri+"')");
			
			Thread.sleep(100);
			boolean ok = latch.await(maxScanTimeS, TimeUnit.SECONDS);
			if (!ok) throw new Exception("The latch broke before the scan finished!");
			
			if (failed.size()>0) throw new Exception(failed.get(0).getMessage());
			
			ScanBean start = startEvents.get(0);
			assertEquals(start.getSize(), beans.size());
			assertEquals(1, startEvents.size());
			
			return beans;
			
		} finally {
			subscriber.disconnect();
			submitter.disconnect();
		}
	}

	protected List<ScanBean> runAndCheck(String name, String mainDetectorName, String processingDetectorName, boolean blocking, long maxScanTimeS) throws Exception {
		
		final IEventService eservice = Services.getEventService();

		// Let's listen to the scan and see if things happen when we run it
		final ISubscriber<IScanListener> subscriber = eservice.createSubscriber(new URI(servlet.getBroker()), servlet.getStatusTopic());
		final ISubmitter<ScanBean>       submitter  = eservice.createSubmitter(new URI(servlet.getBroker()),  servlet.getSubmitQueue());
		
		try {
			final List<ScanBean> beans = new ArrayList<>(13);
			final List<ScanBean> failed = new ArrayList<>(13);
			final List<ScanBean> startEvents = new ArrayList<>(13);
			
			final CountDownLatch latch = new CountDownLatch(1);
			subscriber.addListener(new IScanListener() {
				@Override
				public void scanEventPerformed(ScanEvent evt) {
					if (evt.getBean().getStatus()==Status.FAILED) failed.add(evt.getBean());
					if (evt.getBean().getPosition()!=null) {
						beans.add(evt.getBean());
					}
				}
	
				@Override
				public void scanStateChanged(ScanEvent evt) {
					if (evt.getBean().getStatus()==Status.FAILED) failed.add(evt.getBean());
					if (evt.getBean().scanStart()) {
						startEvents.add(evt.getBean()); // Should be just one
					}
	                if (evt.getBean().scanEnd()) {
	                	latch.countDown();
	                }
				}
			});
	
			
			// Ok done that, now we sent it off...
			pi.exec("submit("+name+", block="+(blocking?"True":"False")+", broker_uri='"+uri+"')");
			
			Thread.sleep(200);
			boolean ok = latch.await(maxScanTimeS, TimeUnit.SECONDS);
			if (!ok) throw new Exception("The latch broke before the scan finished!");
			
			if (failed.size()>0) throw new Exception(failed.get(0).getMessage());
			
			ScanBean start = startEvents.get(0);
			assertEquals(start.getSize(), beans.size());
			assertEquals(1, startEvents.size());
			
			Thread.sleep(100);
			
			// Do Some checking
			ScanClusterProcessingChecker checker = new ScanClusterProcessingChecker(fileFactory, pconsumer);
			checker.setDetectorName(mainDetectorName);
			checker.setProcessingName(processingDetectorName);
			checker.setScannableNames(Arrays.asList("xNex", "yNex"));
			checker.setFilePath(path);
			
			// Check the main nexus file
			checker.checkNexusFile(2, 2);
			
			// Check the processing bean was submitted successfully
			checker.checkSubmittedBean(true);

			return beans;
			
		} finally {
			subscriber.disconnect();
			submitter.disconnect();
		}
	}
	
	protected List<ScanBean> runAndCheckNoPython(ScanBean bean, long maxScanTimeS) throws Exception {
		
		final IEventService eservice = Services.getEventService();

		// Let's listen to the scan and see if things happen when we run it
		final ISubscriber<IScanListener> subscriber = eservice.createSubscriber(new URI(servlet.getBroker()), servlet.getStatusTopic());
		final ISubmitter<ScanBean>       submitter  = eservice.createSubmitter(new URI(servlet.getBroker()),  servlet.getSubmitQueue());
		
		try {
			final List<ScanBean> beans       = new ArrayList<>(13);
			final List<ScanBean> startEvents = new ArrayList<>(13);
			final List<ScanBean> endEvents   = new ArrayList<>(13);			
			final CountDownLatch latch       = new CountDownLatch(1);
			
			subscriber.addListener(new IScanListener() {
				@Override
				public void scanEventPerformed(ScanEvent evt) {
					if (evt.getBean().getPosition()!=null) {
						beans.add(evt.getBean());
					}
				}
	
				@Override
				public void scanStateChanged(ScanEvent evt) {
					if (evt.getBean().scanStart()) {
						startEvents.add(evt.getBean()); // Should be just one
					}
	                if (evt.getBean().scanEnd()) {
	                	endEvents.add(evt.getBean());
	                	latch.countDown();
	                }
				}
			});
	
			
			// Ok done that, now we sent it off...
			submitter.submit(bean);
			
			Thread.sleep(200);
			boolean ok = latch.await(maxScanTimeS, TimeUnit.SECONDS);
			if (!ok) throw new Exception("The latch broke before the scan finished!");
			
			assertEquals(1, startEvents.size());
			assertEquals(1, endEvents.size());
			
			return beans;
			
		} finally {
			subscriber.disconnect();
			submitter.disconnect();
		}
	}

	
	protected ScanBean createStepScan() throws IOException {
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
		return bean;
	}


}
