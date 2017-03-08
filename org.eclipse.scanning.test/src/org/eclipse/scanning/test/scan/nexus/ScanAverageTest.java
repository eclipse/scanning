package org.eclipse.scanning.test.scan.nexus;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.device.models.SlicingModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.sequencer.analysis.AveragingSlicingDevice;
import org.junit.Before;
import org.junit.Test;

public class ScanAverageTest extends NexusTest {

	private IWritableDetector<MandelbrotModel> imagedetector;
	
	@Before
	public void before() throws ScanningException {
		
		MandelbrotModel model = createMandelbrotModel();		
		imagedetector = (IWritableDetector<MandelbrotModel>)dservice.createRunnableDevice(model);
		assertNotNull(imagedetector);
		
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)dservice;
		impl._register(SlicingModel.class, AveragingSlicingDevice.class);
		
	}

	@Test 
	public void testImageNoAveraging() throws Exception {
		
		// All scannables should have their name set ok
		ScanModel smodel = createGridScanModel(imagedetector, output, true, 2, 2);
		
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);
		scanner.run(null);
	}
	
	@Test 
	public void testImageAveraging() throws Exception {
		
		// All scannables should have their name set ok
		ScanModel smodel = createGridScanModel(imagedetector, output, true, 2, 2);
		
		SlicingModel slicer = new SlicingModel();
		slicer.setName("average");
		slicer.setDataFile(output.getAbsolutePath());
		slicer.setDetectorName(imagedetector.getModel().getName());
		slicer.setTimeout(1);
		slicer.setDataRank(2);
		IRunnableDevice<SlicingModel> averager =  dservice.createRunnableDevice(slicer, null);
		final List<IRunnableDevice<?>> detectors = new ArrayList<>(smodel.getDetectors());
		detectors.add(averager);
		smodel.setDetectors(detectors);
		
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);
		scanner.run(null);
	}

}
