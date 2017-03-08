package org.eclipse.scanning.test.scan.nexus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.january.IMonitor;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.PositionIterator;
import org.eclipse.january.dataset.Slice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.models.SlicingModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.detector.RandomLineModel;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.eclipse.scanning.sequencer.analysis.AveragingSlicingDevice;
import org.junit.Before;
import org.junit.Test;

public class ScanAverageTest extends NexusTest {

	private IRunnableDevice<?> imagedetector, linedetector;
	
	@Before
	public void before() throws ScanningException {
		
		MandelbrotModel model = createMandelbrotModel();		
		imagedetector = dservice.createRunnableDevice(model);
		assertNotNull(imagedetector);
		
		linedetector = dservice.createRunnableDevice(new RandomLineModel());
		assertNotNull(linedetector);
		
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)dservice;
		impl._register(SlicingModel.class, AveragingSlicingDevice.class);
		
	}

	@Test 
	public void testLineNoAveraging() throws Exception {
		
		// All scannables should have their name set ok
		IRunnableDevice<ScanModel> scanner = createScanner(linedetector, 1, false, 2, 2);
		scanner.run(null);
	}
	
	@Test 
	public void testLineAveraging() throws Exception {
		
		// All scannables should have their name set ok
		IRunnableDevice<ScanModel> scanner = createScanner(linedetector, 1, true, 2, 2);
		scanner.run(null);
	
		checkAveraging(scanner, 2, 2);
	}
	
	@Test 
	public void testLineAveragingDifferentSize() throws Exception {
		
		// All scannables should have their name set ok
		IRunnableDevice<ScanModel> scanner = createScanner(linedetector, 1, true, 3, 1);
		scanner.run(null);
	
		checkAveraging(scanner, 3, 1);
	}	
	
	@Test(expected=AssertionError.class)
	public void testLineAveragingBadSizeCheck() throws Exception {
		
		// All scannables should have their name set ok
		IRunnableDevice<ScanModel> scanner = createScanner(linedetector, 1, true, 2, 2);
		scanner.run(null);
		checkAveraging(scanner, 3, 1);
	}	

	
	@Test 
	public void testImageNoAveraging() throws Exception {
		
		// All scannables should have their name set ok
		IRunnableDevice<ScanModel> scanner = createScanner(imagedetector, 2, false, 2, 2);
		scanner.run(null);
	}

	@Test 
	public void testImageAveraging() throws Exception {
		
		// All scannables should have their name set ok
		IRunnableDevice<ScanModel> scanner = createScanner(imagedetector, 2, true, 2, 2);
		scanner.run(null);
	
		checkAveraging(scanner, 2, 2);
	}

	private IRunnableDevice<ScanModel> createScanner(IRunnableDevice<?> device, int dataRank, boolean doAveraging, int... shape) throws Exception {
		
		ScanModel smodel = createGridScanModel(device, output, true, shape);
		if (doAveraging) {
			SlicingModel model = new SlicingModel();
			model.setName("average");
			model.setDataFile(output.getAbsolutePath());
			model.setDetectorName(device.getName());
			model.setTimeout(1);
			model.setDataRank(dataRank);
			IRunnableDevice<SlicingModel> averager =  dservice.createRunnableDevice(model, null);
			final List<IRunnableDevice<?>> detectors = new ArrayList<>(smodel.getDetectors());
			detectors.add(averager);
			smodel.setDetectors(detectors);
		}
		return dservice.createRunnableDevice(smodel, null);
	}

	private void checkAveraging(IRunnableDevice<ScanModel> scanner, int... scanShape) throws Exception {
		
		SlicingModel model = (SlicingModel)scanner.getModel().getDetectors().get(1).getModel();
		
		ILoaderService lservice = ServiceHolder.getLoaderService();
		IDataHolder    holder   = lservice.getData(model.getDataFile(), new IMonitor.Stub());

		ILazyDataset data = holder.getLazyDataset("/entry/instrument/"+model.getDetectorName()+"/data");
		ILazyDataset av   = holder.getLazyDataset("/entry/instrument/"+model.getDetectorName()+AveragingSlicingDevice.AVERAGE_QUALIFIER+"/data");

		assertTrue(Arrays.equals(scanShape, av.getShape()));
		
		final PositionIterator it = new PositionIterator(scanShape);
		while(it.hasNext()) {
			int[] pos = it.getPos();
			Slice[] islice = new Slice[data.getRank()];   // With two nulls at the end
			Slice[] aslice = new Slice[scanShape.length];
			for (int i = 0; i < pos.length; i++) {
				islice[i] = aslice[i] = new Slice(pos[i], pos[i]+1);
			}
			IDataset image = data.getSlice(islice);
			double mean1 = (Double)image.squeeze().mean();
			double mean2 = av.getSlice(aslice).getDouble(0);
          
			assertEquals(mean1, mean2, 0.00001);
		}
	}

	protected int[] getDataShape(int dataRank, ILazyDataset data) {
		
  		int[] dshape = new int[data.getRank()];
  		Arrays.fill(dshape, 1);
  		
		for (int i = dataRank; i > 0; i--) {
			dshape[dshape.length-i] = data.getShape()[data.getShape().length-i];
		}
		return dshape;
	}

}
