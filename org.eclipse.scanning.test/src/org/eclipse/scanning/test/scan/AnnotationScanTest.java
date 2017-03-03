package org.eclipse.scanning.test.scan;

import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.scanning.api.AbstractScannable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.annotation.scan.FileDeclared;
import org.eclipse.scanning.api.annotation.scan.LevelEnd;
import org.eclipse.scanning.api.annotation.scan.LevelStart;
import org.eclipse.scanning.api.annotation.scan.PointEnd;
import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.annotation.scan.PostConfigure;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.annotation.scan.ScanEnd;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.annotation.scan.WriteComplete;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.LevelInformation;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.sequencer.SubscanModerator;
import org.eclipse.scanning.test.scan.nexus.NexusTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AnnotationScanTest extends NexusTest {
	
	private static interface InjectionDevice {
		
		public <A extends Annotation> void annotatedMethodCalled(Class<A> annotationClass, Object... objects);
		
		public <A extends Annotation> Set<Object> getMethodContext(Class<A> annotationClass);
		
		@ScanStart
		public default void scanStart(IPosition position, ScanInformation scanInfo, SubscanModerator moderator, ScanBean scanBean, ScanModel scanModel) {
			annotatedMethodCalled(ScanStart.class, position, scanInfo, moderator, scanBean, scanModel);
		}
		
		@PointStart
		public default void pointStart(IPosition position, ScanInformation scanInfo, SubscanModerator moderator, ScanBean scanBean, ScanModel scanModel) {
			annotatedMethodCalled(PointStart.class, position, scanInfo, moderator, scanBean, scanModel);
		}
		
		@PointEnd
		public default void pointEnd(IPosition position, ScanInformation scanInfo, SubscanModerator moderator, ScanBean scanBean, ScanModel scanModel) {
			annotatedMethodCalled(PointEnd.class, position, scanInfo, moderator, scanBean, scanModel);
		}
		
		@ScanFinally
		public default void scanFinally(IPosition position, ScanInformation scanInfo, SubscanModerator moderator, ScanBean scanBean, ScanModel scanModel) {
			annotatedMethodCalled(ScanFinally.class, position, scanInfo, moderator, scanBean, scanModel);
		}
		
		@ScanEnd
		public default void scanEnd(IPosition position, ScanInformation scanInfo, SubscanModerator moderator, ScanBean scanBean, ScanModel scanModel) {
			annotatedMethodCalled(ScanEnd.class, position, scanInfo, moderator, scanBean, scanModel);
		}
		
		@PreConfigure
		public default void preConfigure() {
			// not called as we use AcquistionDevice directly instead of ScanProcess
			annotatedMethodCalled(PreConfigure.class);
		}
		
		@PostConfigure
		public default void postConfigure() {
			// not called as we use AcquistionDevice directly instead of ScanProcess
			annotatedMethodCalled(PostConfigure.class);
		}
		
		@LevelStart
		public default void levelStart(IPosition position, LevelInformation levelInfo, ScanInformation scanInfo, SubscanModerator moderator, ScanBean scanBean, ScanModel scanModel) {
			annotatedMethodCalled(LevelStart.class, position, levelInfo, scanInfo, moderator, scanBean, scanModel);
		}
		
		@LevelEnd
		public default void levelEnd(IPosition position, LevelInformation levelInfo, ScanInformation scanInfo, SubscanModerator moderator, ScanBean scanBean, ScanModel scanModel) {
			annotatedMethodCalled(LevelEnd.class, position, levelInfo, scanInfo, moderator, scanBean, scanModel);
		}
		
		@FileDeclared
		public default void fileDeclared(String filePath, IPosition position, ScanInformation scanInfo, SubscanModerator moderator, ScanBean scanBean, ScanModel scanModel) {
			annotatedMethodCalled(FileDeclared.class, filePath, position, scanInfo, moderator, scanBean, scanModel);
		}
		
		@WriteComplete
		public default void writeComplete(IPosition position, ScanInformation scanInfo, SubscanModerator moderator, ScanBean scanBean, ScanModel scanModel) {
			annotatedMethodCalled(WriteComplete.class, position, scanInfo, moderator, scanBean, scanModel);
		}

	}
	
	private class InjectionDetector extends AbstractRunnableDevice<Object> implements InjectionDevice {

		private final Map<Class<?>, Set<Object>> injectionContexts = new HashMap<>();
		
		protected InjectionDetector() {
			super(null);
			setName("injectionDetector");
		}

		@Override
		public void run(IPosition position) throws ScanningException, InterruptedException {
			// Do nothing
		}

		@Override
		public <A extends Annotation> void annotatedMethodCalled(Class<A> annotationClass, Object... objects) {
			if (!injectionContexts.containsKey(annotationClass)) {
				injectionContexts.put(annotationClass, new HashSet<>(Arrays.asList(objects)));
			}
		}

		@Override
		public <A extends Annotation> Set<Object> getMethodContext(Class<A> annotationClass) {
			return injectionContexts.get(annotationClass);
		}
		
	}
	
	private class InjectionMonitor extends AbstractScannable<Object> implements InjectionDevice {
		
		private final Map<Class<?>, Set<Object>> injectionContexts = new HashMap<>();

		public InjectionMonitor() {
			setName("injectionMonitor");
			setMonitorRole(MonitorRole.PER_POINT);
		}

		@Override
		public Object getPosition() throws Exception {
			return null;
		}

		@Override
		public void setPosition(Object value, IPosition position) throws Exception {
			// do nothing
		}

		@Override
		public <A extends Annotation> void annotatedMethodCalled(Class<A> annotationClass, Object... objects) {
			if (!injectionContexts.containsKey(annotationClass)) {
				injectionContexts.put(annotationClass, new HashSet<>(Arrays.asList(objects)));
			}
		}

		@Override
		public <A extends Annotation> Set<Object> getMethodContext(Class<A> annotationClass) {
			return injectionContexts.get(annotationClass);
		}
		
	}
	
	private InjectionMonitor injectionMonitor;
	private InjectionDetector injectionDetector;
	
	@Before
	public void before() {
		injectionMonitor = new InjectionMonitor();
		injectionDetector = new InjectionDetector();
	}
	
	@Test
	public void testInjectedContext() throws Exception {
		IRunnableDevice<ScanModel> scanner = createGridScan(injectionDetector, injectionMonitor, 2, 2);
		scanner.run(null);
		
		// check that each annotated method for each device has been invoked with objects
		// of the expected classes. TODO: even better would be to check the objects themselves are correct
		// not just that there is an object of the expected class
		checkInjectedContext(ScanStart.class, true, IPosition.class);
		checkInjectedContext(FileDeclared.class, true, String.class, IPosition.class);
		checkInjectedContext(PointStart.class, true, IPosition.class);
		checkInjectedContext(PointEnd.class, true, IPosition.class);
		checkInjectedContext(LevelStart.class, false, LevelInformation.class, IPosition.class);
		checkInjectedContext(LevelEnd.class, false, LevelInformation.class, IPosition.class);
		checkInjectedContext(WriteComplete.class, true, IPosition.class);
		checkInjectedContext(ScanEnd.class, true, IPosition.class);
		checkInjectedContext(ScanFinally.class, true, IPosition.class);
	}
	
	private <A extends Annotation> void checkInjectedContext(Class<A> annotationClass, boolean includeCommonContext, Class<?>... expectedContextClasses) {
		checkInjectedContext(injectionDetector, annotationClass, includeCommonContext, expectedContextClasses);
		checkInjectedContext(injectionMonitor, annotationClass, includeCommonContext, expectedContextClasses);
	}
	
	private static final Class<?>[] COMMON_CONTEXT_CLASSES = new Class<?>[] {
		ScanModel.class, SubscanModerator.class, ScanBean.class, ScanInformation.class
	};
	
	private <A extends Annotation> void checkInjectedContext(InjectionDevice device, Class<A> annotationClass, boolean includeCommonContext, Class<?>... expectedContextClasses) {
		Set<Object> injectedContext = device.getMethodContext(annotationClass);
		assertNotNull(injectedContext);

		// stream the total set of expected classes, filter out those found in context - if the resulting stream isn't empty we're missing something
		final Predicate<Class<?>> contextHasInstance = klass -> injectedContext.stream().anyMatch(obj -> klass.isInstance(obj));
		Stream<Class<?>> expectedClassStream = Arrays.stream(expectedContextClasses);
		if (includeCommonContext) {
			expectedClassStream = Stream.concat(expectedClassStream, Arrays.stream(COMMON_CONTEXT_CLASSES));
		}
		Optional<Class<?>> missingExpectedContext = expectedClassStream.filter(klass -> !contextHasInstance.test(klass)).findFirst();
		if (missingExpectedContext.isPresent()) {
			Assert.fail("Context missing expected instance of: " + missingExpectedContext.get());
		}
	}
	
	private IRunnableDevice<ScanModel> createGridScan(IRunnableDevice<?> detector,
			IScannable<?> monitor, int... size) throws Exception {
		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("xNex");
		gmodel.setFastAxisPoints(size[size.length-1]);
		gmodel.setSlowAxisName("yNex");
		gmodel.setSlowAxisPoints(size[size.length-2]);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));
		
		IPointGenerator<?> gen = gservice.createGenerator(gmodel);
		
		IPointGenerator<?>[] gens = new IPointGenerator<?>[size.length - 1];
		// We add the outer scans, if any
		if (size.length > 2) { 
			for (int dim = size.length-3; dim>-1; dim--) {
				final StepModel model;
				if (size[dim]-1>0) {
				    model = new StepModel("neXusScannable"+(dim+1), 10,20,9.99d/(size[dim]-1));
				} else {
					model = new StepModel("neXusScannable"+(dim+1), 10,20,30); // Will generate one value at 10
				}
				final IPointGenerator<?> step = gservice.createGenerator(model);
				gens[dim] = step;
			}
		}
		gens[size.length - 2] = gen;

		gen = gservice.createCompoundGenerator(gens);
	
		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		if (detector != null) {
			smodel.setDetectors(detector);
		}
		if (monitor != null) {
			smodel.setMonitors(monitor);
		}
		
		// Create a file to scan into.
		smodel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to "+smodel.getFilePath());

		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);
		
		final IPointGenerator<?> fgen = gen;
		((IRunnableEventDevice<ScanModel>)scanner).addRunListener(new IRunListener() {
			@Override
			public void runWillPerform(RunEvent evt) throws ScanningException {
				try {
					System.out.println("Running acquisition scan of size "+fgen.size());
				} catch (GeneratorException e) {
					throw new ScanningException(e);
				}
			}
		});

		return scanner;
	}

}
