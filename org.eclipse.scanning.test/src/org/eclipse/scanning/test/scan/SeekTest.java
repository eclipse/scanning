package org.eclipse.scanning.test.scan;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IDeviceController;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.server.servlet.Services;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SeekTest extends AbstractAcquisitionTest {

	private File temp;
	private static INexusFileFactory factory;
	
	@BeforeClass
	public static void createFactory() {
		factory = new NexusFileFactoryHDF5();
	}
	
	@Before
	public void setupServices() throws Exception {
		super.setupServices();
		temp = File.createTempFile("test_seek", ".nxs");
		temp.deleteOnExit();
	}
	
	@After
	public void removeFile() throws Exception {
		temp.delete();
	}

	@Test
	public void seekFirst() throws Exception {
		
		IDeviceController controller = createTestScanner(null);
		AbstractRunnableDevice<ScanModel> scanner = (AbstractRunnableDevice<ScanModel>)controller.getDevice();
    
		try {
			scanner.start(null);		
			scanner.latch(100, TimeUnit.MILLISECONDS); // Latch onto the scan, breaking before it is finished.
			scanner.pause();
			
			IPosition first   = scanner.getModel().getPositionIterable().iterator().next();
			IPosition current = scanner.getPositioner().getPosition();
			assertNotNull(current);
			assertNotEquals(first, current);
			
			scanner.seek(0);
			current = scanner.getPositioner().getPosition();
			assertNotNull(current);
			assertEquals(first.getStepIndex(), current.getStepIndex());
			
		} finally {
			scanner.abort();
		}
	}
	
	@Test
	public void seekFirstNoChangeDatasetShape() throws Exception {
		checkSeekDataset(0);
	}
	
	@Test
	public void seekSecondNoChangeDatasetShape() throws Exception {
		checkSeekDataset(2);
	}
	
	private void checkSeekDataset(int seekPosition)  throws Exception {
		
		final String detectorName = "mandelbrot";
		IDeviceController controller = createTestScanner(sservice.getRunnableDevice(detectorName),
				                                         Arrays.asList("xNex", "yNex"),
				                                         temp.getAbsolutePath());
		
		AbstractRunnableDevice<ScanModel> scanner = (AbstractRunnableDevice<ScanModel>)controller.getDevice();
    
		try {
			scanner.start(null);	
			
			scanner.latch(100, TimeUnit.MILLISECONDS); // Latch onto the scan, breaking before it is finished.
			scanner.pause();
			
			IPosition current = scanner.getPositioner().getPosition();
			assertNotNull(current);
			
			scanner.seek(seekPosition);
			current = scanner.getPositioner().getPosition();
			assertNotNull(current);
			
			IPosition check   = seek(seekPosition, scanner.getModel().getPositionIterable().iterator());
			assertEquals(check.getStepIndex(), current.getStepIndex());
			
			// Run to end
			scanner.resume();
			scanner.latch(10, TimeUnit.SECONDS);

			NexusFile nf = factory.newNexusFile(temp.getAbsolutePath());
			nf.openToRead();
			
			TreeFile nexusTree = NexusUtils.loadNexusTree(nf);
			NXroot rootNode = (NXroot) nexusTree.getGroupNode();
			NXentry entry = rootNode.getEntry();
			NXinstrument instrument = entry.getInstrument();
			
			NXdetector detector = instrument.getDetector(detectorName);
			assertNotNull(detector);

			DataNode dataNode = detector.getDataNode(NXdetector.NX_DATA);
			IDataset dataset = dataNode.getDataset().getSlice();
			assertNotNull(dataset);
			assertArrayEquals(new int[]{5, 5, 241, 301}, dataset.getShape());

			nf.close();
			
		} finally {
			scanner.abort();
		}
	}

	private IPosition seek(int location, Iterator<IPosition> iterator) {
		int stepNumber=0;
		/*
		 * IMPORTANT We do not keep the positions in memory because there can be millions.
		 * Running over them is fast however.
		 */
		while(iterator.hasNext()) {
			IPosition pos = iterator.next();
        	pos.setStepIndex(stepNumber);
			if (stepNumber == location) return pos;
			stepNumber++;
		}
		return null;
	}

	@Test
	public void seekFirstRestartsInCorrectLocation() throws Exception {
		
		IDeviceController controller = createTestScanner(null);
		AbstractRunnableDevice<ScanModel> scanner = (AbstractRunnableDevice<ScanModel>)controller.getDevice();
		
		final List<Integer> steps = new ArrayList<Integer>();
		scanner.addPositionListener(new IPositionListener() {
			public void positionPerformed(PositionEvent evt) throws ScanningException {
				steps.add(evt.getPosition().getStepIndex());
			}
		});
		try {
			scanner.start(null);		
			scanner.latch(100, TimeUnit.MILLISECONDS); // Latch onto the scan, breaking before it is finished.
			scanner.pause();
			
			IPosition first   = scanner.getModel().getPositionIterable().iterator().next();
			IPosition stopped = scanner.getPositioner().getPosition();
			assertNotNull(stopped);
			assertNotEquals(first, stopped);
			assertTrue(stopped.getStepIndex()>0);
			assertTrue(stopped.getStepIndex()<24);
			
			scanner.seek(0);
			IPosition current = scanner.getPositioner().getPosition();
			assertNotNull(current);
			assertEquals(first.getStepIndex(), current.getStepIndex());
			scanner.resume();
			scanner.latch(10, TimeUnit.SECONDS);
			
			// The scan should restart from where it is seeked to.
			// Therefore the steps should be size (25) + stopped.getStepIndex()
			assertEquals("The scan should restart from where it is seeked to.",
					     25+stopped.getStepIndex()+1, steps.size());
			
		} finally {
			scanner.abort();
		}
	}

	@Test
	public void staticScannerAvaileForJython() throws Exception {

		IDeviceController controller = createTestScanner(null);
		IRunnableDevice<ScanModel> scanner = (IRunnableDevice<ScanModel>)controller.getDevice();
    
		try {
			scanner.start(null);		
			scanner.latch(100, TimeUnit.MILLISECONDS); // Latch onto the scan, breaking before it is finished.

            assertTrue(Services.getRunnableDeviceService().getActiveScanner()!=null);
			scanner.latch(10, TimeUnit.SECONDS);
		
		} finally {
			scanner.abort();
		}
        assertTrue(Services.getRunnableDeviceService().getActiveScanner()==null);

	}

	@Test(expected=ScanningException.class)
	public void seekTooLarge() throws Exception {

		IDeviceController controller = createTestScanner(null);
		AbstractRunnableDevice<ScanModel> scanner = (AbstractRunnableDevice<ScanModel>)controller.getDevice();
    
		try {
			scanner.start(null);		
			scanner.latch(100, TimeUnit.MILLISECONDS); // Latch onto the scan, breaking before it is finished.
			scanner.pause();
			
			IPosition first   = scanner.getModel().getPositionIterable().iterator().next();
			IPosition current = scanner.getPositioner().getPosition();
			assertNotNull(current);
			assertNotEquals(first, current);
			
			scanner.seek(100); // Too large
			
		} finally {
			scanner.abort();
		}

	}
	
	@Test(expected=ScanningException.class)
	public void seekTooSmall() throws Exception {

		IDeviceController controller = createTestScanner(null);
		AbstractRunnableDevice<ScanModel> scanner = (AbstractRunnableDevice<ScanModel>)controller.getDevice();
    
		try {
			scanner.start(null);		
			scanner.latch(100, TimeUnit.MILLISECONDS); // Latch onto the scan, breaking before it is finished.
			scanner.pause();
			
			IPosition first   = scanner.getModel().getPositionIterable().iterator().next();
			IPosition current = scanner.getPositioner().getPosition();
			assertNotNull(current);
			assertNotEquals(first, current);
			
			scanner.seek(-1); // Too large
			
		} finally {
			scanner.abort();
		}

	}

}
