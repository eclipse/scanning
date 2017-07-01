package org.eclipse.scanning.event.queues.spooler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.queues.models.DeviceModel;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.ModelEvaluationException;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.spooler.pathassemblers.ArrayModelAssembler;
import org.eclipse.scanning.event.queues.spooler.pathassemblers.IScanPathModelAssembler;
import org.eclipse.scanning.event.queues.spooler.pathassemblers.StepModelAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScanAtomAssembler extends AbstractBeanAssembler<ScanAtom> {
	
	private static final Logger logger = LoggerFactory.getLogger(ScanAtomAssembler.class);
	
	//We always want to set this value for the detectors
	private static final QueueValue<String> EXPOSURETIME = new QueueValue<>("exposureTime", true);
	
	private Map<String, IScanPathModelAssembler<? extends IScanPathModel>> pathAssemblerRegister;
	
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
		
		pathAssemblerRegister = new HashMap<>();
		pathAssemblerRegister.put("step", new StepModelAssembler());
		pathAssemblerRegister.put("array", new ArrayModelAssembler());
	}
	
	

	@Override
	public void updateBeanModel(ScanAtom model, ExperimentConfiguration config) throws QueueModelException {
		//Where do we have values we may want to update?
		// - detectorModelsModel
		// - pathModelsModel
		//0) Check for duplicates
		//1) We update these against their respective Maps in ExpConfig
		//2) We search the results for values that need replacing from localValues
		
		//Paths
		updateModelMap(model.getPathModelsModel(), config.getPathModelValues(), config);
		updateModelMap(model.getDetectorModelsModel(), config.getDetectorModelValues(), config);
	}
	
	private void updateModelMap(Map<String, DeviceModel> modelMap, Map<String, DeviceModel> configMap, ExperimentConfiguration config) throws QueueModelException {
		Set<String> extraPaths = new HashSet<>(configMap.keySet());
		extraPaths.removeAll(modelMap.keySet());
		extraPaths.stream().forEach(pathName -> modelMap.put(pathName, configMap.get(pathName)));
		//Check for duplicates
		if (extraPaths.size() != configMap.keySet().size()) {
			Optional<String> deviceName = configMap.keySet().stream().filter(confName -> modelMap.containsKey(confName)).findFirst();
			logger.error("Both stored and experiment models configure '"+deviceName.get()+"'. Cannot specify multiple configurations for same device");
			throw new QueueModelException("Cannot specify multiple configurations for same device ('"+deviceName.get()+"')");
		}
		
		for (Map.Entry<String, DeviceModel> pathModel : modelMap.entrySet()) {
			List<IQueueValue<?>> modelDevConf = pathModel.getValue().getDeviceConfiguration();
			modelDevConf = modelDevConf.stream().map(option -> updateValue(option, config)).collect(Collectors.toList());
			pathModel.getValue().setDeviceConfiguration(modelDevConf);
		}
	}
	
	@SuppressWarnings("unchecked")//We check value is safe to cast before getting near the cast - this is fine 
	private IQueueValue<?> updateValue(IQueueValue<?> value, ExperimentConfiguration config) {
		if (value.isReference() && value instanceof QueueValue && value.getValueType().equals(String.class)) {
			try {
				String argName = value.getName();
				value = getRealValue((QueueValue<String>)value, config);
				value.setName(argName);
			} catch (QueueModelException qmEx) {
				throw new ModelEvaluationException(qmEx);
			}
		}
		return value;
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
	
	private <R> CompoundModel<R> prepareScanPaths(Map<String, DeviceModel> pathModels) throws QueueModelException {
		CompoundModel<R> paths = new CompoundModel<>();
		
		for (String deviceName : pathModels.keySet()) {
			DeviceModel devModel = pathModels.get(deviceName);
			
			//Create the IScanPathModel
			IScanPathModelAssembler<? extends IScanPathModel> pathAssembler = pathAssemblerRegister.get(devModel.getType().toLowerCase());
			Object path = pathAssembler.assemble(deviceName, devModel);
			configureObject(path, devModel, pathAssembler.getRequiredArgReferences());
			
			//Create the ROIs, if there 
			List<R> rois = null;
			if (devModel.getRoiConfiguration().size() > 0 ) {
				//TODO
			}
			
			paths.addData(path, rois);
		}
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
	 * @throws QueueModelException if the detector could not be configured or 
	 *         no value for EXPOSURETIME is set
	 */
	private Map<String, Object> prepareDetectors(Map<String, DeviceModel> detectorModels) throws QueueModelException {
		Map<String, Object> detectors = new HashMap<>();
		for (Map.Entry<String, DeviceModel> detectorEntry : detectorModels.entrySet()) {
			String detName = detectorEntry.getKey();
			DeviceModel detConfig = detectorEntry.getValue();
			IDetectorModel detModel;
			try {
				detModel = (IDetectorModel) ServicesHolder.getDeviceService().getRunnableDevice(detName).getModel();
			} catch (ClassCastException | NullPointerException ex) {
				logger.error("Device model returned for detector '"+detName+"' was not a detector model");
				throw new QueueModelException("Failed to cast '"+detName+"' model to IDetectorModel", ex);
			} catch (ScanningException ex) {
				//getRunnableDevice isn't actually able to throw a ScanningException (as of 27.06.2017)
				logger.error("No detector returned by RunnableDeviceService for the name '"+detName+"'");
				throw new QueueModelException("No detector for name '"+detName+"'");
			}
			//We always want to set exposure time, so check it's set, set it and to save time don't set it again
			if (!detConfig.getDeviceConfiguration().stream().anyMatch(option -> EXPOSURETIME.isReference())) {
				logger.error("No '"+EXPOSURETIME.evaluate()+"' value in input model of detector '"+detName+"'. "+EXPOSURETIME.evaluate()+" is required to configure each detector");
				throw new QueueModelException("No '"+EXPOSURETIME.evaluate()+"' value in input model of detector '"+detName+"'");
			}
			detModel.setExposureTime((Double)detConfig.getDeviceModelValue(EXPOSURETIME).evaluate());
			detModel = configureObject(detModel, detConfig, Arrays.asList(EXPOSURETIME));
			detectors.put(detName, detModel);
		}
		
		return detectors;
	}
	
	private Collection<String> prepareMonitors(Collection<IQueueValue<?>> monitorsModel) {
		List<String> monitors = new ArrayList<>();
		
		return monitors;
	}
	
	/**
	 * For a given object and List of {@link IQueueValues}, determine which 
	 * items in the list represent values which can be set on the object and 
	 * set them. If an item in the configuration appears in the ignoreArray, it 
	 * will not be set by this method. The method only considers methods on the 
	 * target obj with names that start with set.
	 * @param obj T to be configured
	 * @param configuration List of {@link IQueueValues} representing 
	 *        configuration
	 * @param ignoreArray array of {@link IQueueValue} parameters which should 
	 *        not be set by this method
	 * @return T obj which has been fully configured
	 */
	private <T> T configureObject(T obj, DeviceModel configuration, List<IQueueValue<String>> ignoreList) throws QueueModelException {
		List<Method> allMethods = Arrays.asList(obj.getClass().getMethods());
		
		configuration.getDeviceConfiguration().stream()
			.filter(option -> ignoreList.stream().anyMatch(ignore -> !ignore.isReference())) //Is this option in the ignore list?
				.forEach(option -> allMethods.stream().filter(method -> method.getName().startsWith("set")) //Does this method start with set?
													  .filter(method -> option.isSetMethodForName(method)) //Is this the set method for this option?
													  .forEach(method -> setField(method, obj, option)));
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
