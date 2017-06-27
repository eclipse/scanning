package org.eclipse.scanning.event.queues.spooler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.queues.models.ModelEvaluationException;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScanAtomAssembler extends AbstractBeanAssembler<ScanAtom> {
	
	private static final Logger logger = LoggerFactory.getLogger(ScanAtomAssembler.class);
	
	//We always want to set this value for the detectors
	private static final QueueValue<String> EXPOSURETIME = new QueueValue<>("exposureTime", true);
	
	/*
	 * 
	 * 
	 * def mscan(path=None, mon=None, det=None, now=False, block=True,
     *    allow_preprocess=False, broker_uri=None):
     *    
     *    submit(request=scan_request(path=path, mon=mon, det=det, allow_preprocess=allow_preprocess),
     *     now=now, block=block, broker_uri=broker_uri)
     * 
     * def scan_request(path=None, mon=None, det=None, file=None, allow_preprocess=False):
     *    cmodel = CompoundModel()
     *    for (model, rois) in scan_paths:
     *       cmodel.addData(model, rois)
     *    detector_map = HashMap()
     *    for (name, model) in detectors:
     *       detector_map[name] = model
	 */

	public ScanAtomAssembler(IQueueBeanFactory queueBeanFactory) {
		super(queueBeanFactory);
	}

	@Override
	public ScanAtom buildNewBean(ScanAtom model) throws QueueModelException {
		ScanAtom atom = new ScanAtom(model.getShortName(), false);
		atom.setBeamline(model.getBeamline());
		atom.setRunTime(model.getRunTime());
		atom.setScanBrokerURI(model.getScanBrokerURI());
		atom.setScanStatusTopicName(model.getScanStatusTopicName());
		atom.setScanSubmitQueueName(model.getScanSubmitQueueName());
		
		ScanRequest<?> scanReq = new ScanRequest<>();
		scanReq.setCompoundModel(prepareScanPaths(model.getPathModelsModel()));
		scanReq.setDetectors(prepareDetectors(model.getDetectorModelsModel()));
		scanReq.setMonitorNames(prepareMonitors(model.getMonitorsModel()));
		atom.setScanReq(scanReq);
		
		return atom;
	}

	@Override
	public ScanAtom setBeanName(ScanAtom bean) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private <R> CompoundModel<R> prepareScanPaths(Map<String, List<IQueueValue<?>>> pathModels) {
		CompoundModel<R> paths = new CompoundModel<>();
		
		return paths;
	}
	
	/**
	 * For each detector in the supplied map, get the model from the 
	 * {@link IRunnableDeviceService} and attempt to set all the fields that 
	 * have been passed as {@link IQueueValue}s as part of the map to their 
	 * evaluated values.
	 * @param detectorModels Map of String names of detectors with a List of 
	 *        {@link IQueueValue}s which provides the configuration 
	 * @return Map of String names of detectors against Object configured 
	 *         detector models
	 * @throws QueueModelException if the detector could not be configured
	 */
	private Map<String, Object> prepareDetectors(Map<String, List<IQueueValue<?>>> detectorModels) throws QueueModelException {
		Map<String, Object> detectors = new HashMap<>();
		for (String detName : detectorModels.keySet()) {
			IRunnableDevice<Object> detector;
			IDetectorModel detModel;
			try {
				detector = ServicesHolder.getDeviceService().getRunnableDevice(detName);
				detModel = (IDetectorModel) detector.getModel();
			} catch (ClassCastException | NullPointerException ex) {
				logger.error("Device model returned for detector '"+detName+"' was not a detector model");
				throw new QueueModelException("Failed to cast '"+detName+"' model to IDetectorModel", ex);
			} catch (ScanningException ex) {
				//getRunnableDevice isn't actually able to throw a ScanningException (as of 27.06.2017)
				logger.error("No detector returned by RunnableDeviceService for the name '"+detName+"'");
				throw new QueueModelException("No detector for name '"+detName+"'");
			}
			detModel.setExposureTime((Double)getQueueValue(EXPOSURETIME).evaluate());
			detModel = configureObject(detModel, detectorModels.get(detName));
			detectors.put(detName, detModel);
		}
		
		return detectors;
	}
	
	private Collection<String> prepareMonitors(Collection<IQueueValue<?>> monitorsModel) {
		List<String> monitors = new ArrayList<>();
		
		return monitors;
	}
	
	/**
	 * For a given object and List of {@link IQueueValues}, determine which items in the list represent values which can be set on the object and set them.
	 * @param obj T to be configured
	 * @param configuration List of {@link IQueueValues} representing 
	 *        configuration
	 * @return T obj which has been fully configured
	 */
	private <T> T configureObject(T obj, List<IQueueValue<?>> configuration) throws QueueModelException {
		List<Method> allMethods = Arrays.asList(obj.getClass().getMethods());
		
		configuration.stream().forEach(
				option -> allMethods.stream().filter(method -> option.isSetMethodForName(method))
				.forEach(method -> setField(method, obj, option)));//TODO By this point all values need to be current

		return obj;
	}
	
	/**
	 * Use the supplied set method to configure a field on the given object to 
	 * the value obtained by evaluating {@link IQueueValue} value.
	 * @param setter Method object, should be a setter
	 * @param obj T on which the setter method will be called
	 * @param value {@link IQueueValue} defining setter argument
	 */
	private <T> void setField(Method setter, T obj, IQueueValue<?> value) {
		try {
			setter.invoke(obj, value.evaluate());
		} catch (Exception ex) {
			Object evaluated = null;
			try {
				evaluated = value.evaluate();
			} catch (ModelEvaluationException meEx) {
				//If this is the failure, we leave evaluated at null and the problem should be clear in the log...
			}
			logger.error("Configuring "+obj.getClass().getSimpleName()+" failed. Could not set value of '"+setter.getName()+"' to '"+evaluated+"'");
			throw new ModelEvaluationException("Failed configuring "+obj.getClass().getSimpleName()+" with "+setter.getName()+" -> "+evaluated);
		}
	}

}
