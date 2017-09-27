package org.eclipse.scanning.sequencer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.LevelRole;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * 
 * This is an exposure time changing level runner. If exposure time
 * on its list of devices is different to the exposure time at a given
 * point it will attempt to reconfigure the devices with their models
 * and a new exposure time. Only devices which have a model of type
 * IDetectorModel will be considered.
 * 
 * There is a list of levels which can be used to filter devices to only
 * move those on a certain list of levels. 
 * 
 * There is a tolerance to only change exposure time if its value is 
 * outside the required value+/- the tolerance.
 * 
 * @author Matthew Gerring
 *
 */
class ExposureTimeManager extends LevelRunner<IRunnableDevice<?>> {
	
	private List<IRunnableDevice<?>> devices;
	private List<Integer>            levels;
	private double                   tolerance = 0.0001; // 10% of a microsecond 

	public ExposureTimeManager() {
		super();
		devices = new ArrayList<>(7);
	}

	/**
	 * Adds all devices passed in. If levels are set only those devices corresponding
	 * to the required level set are 
	 * @param toAdd
	 */
	public void addDevices(List<IRunnableDevice<?>> toAdd) {
		
		List<IRunnableDevice<?>> filtered = Optional.of(toAdd).orElse(Collections.emptyList());
		filtered = filtered.stream().filter(this::isApplicable).collect(Collectors.toList());
		devices.addAll(filtered);
	}
	
	private boolean isApplicable(IRunnableDevice<?> device) {
		
		// Check model
		if (device.getModel()==null) 
			return false;
		if (!(device.getModel() instanceof IDetectorModel)) 
			return false;
		
		// Check levels
		if (levels==null) 
			return true;
		return levels.contains(device.getLevel());
	}

	public void setExposureTime(IPosition location) throws ScanningException, InterruptedException {
		
		// Check if a different time was ask for
		if (location.getExposureTime()<=0) return;
		
		// Check if there are devices to set.
		if (devices.isEmpty())             return;
		
		// Check if any of those devices 
		double time = location.getExposureTime();
		
		// Get devices with different time
		List<IRunnableDevice<?>> differentTime = devices.stream().filter(device->isTimeDifferent(device, time)).collect(Collectors.toList());
		if (differentTime.isEmpty()) return;
		
		// Change the time, this is normally fast but 
		// Mark Booth says that sometimes the area detector
		// pipeline has to be stopped and restarted. 99%
		// of the time however 
		run(location);
	}

	private boolean isTimeDifferent(IRunnableDevice<?> device, double val) {
		double cur = ((IDetectorModel)device.getModel()).getExposureTime();
		boolean ok = cur>(val-tolerance) && cur<(val+tolerance);
		return !ok;
	}

	@Override
	protected Collection<IRunnableDevice<?>> getDevices() throws ScanningException {
		return devices;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Callable<IPosition> create(IRunnableDevice<?> device, IPosition position) throws ScanningException {
		return new ExposureTimeTask((IRunnableDevice<IDetectorModel>)device, position);
	}
	
	private final class ExposureTimeTask implements Callable<IPosition> {

		private IRunnableDevice<IDetectorModel> device;
		private IPosition          position;

		public ExposureTimeTask(IRunnableDevice<IDetectorModel> device, IPosition position) {
			this.device   = device;
			this.position = position;
		}

		@Override
		public IPosition call() throws Exception {
			IDetectorModel model = device.getModel();
			model.setExposureTime(position.getExposureTime());
			device.configure(model);
			return null; // Faster
		}

	}
	
	@Override
	protected LevelRole getLevelRole() {
		return LevelRole.MOVE;
	}

	/**
	 * The levels of devices we should include in setting the exp time.
	 * @return
	 */
	public List<Integer> getLevels() {
		return levels;
	}

	/**
	 * Set the levels of the devices which we should include in setting the time.
	 * Any device which is not in this list of levels is ignored. If a levels are
	 * null (unset) then all devices are added when the addDevices(...) cal is made.
	 * 
	 * @param levels
	 */
	public void setLevels(int... levels) {
		this.levels = levels!=null ? IntStream.of(levels).boxed().collect(Collectors.toList()) : null;
	}
}
