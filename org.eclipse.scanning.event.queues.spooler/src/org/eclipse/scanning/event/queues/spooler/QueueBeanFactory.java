package org.eclipse.scanning.event.queues.spooler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.MonitorAtom;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.event.queues.spooler.beanassemblers.IBeanAssembler;
import org.eclipse.scanning.event.queues.spooler.beanassemblers.MonitorAtomAssembler;
import org.eclipse.scanning.event.queues.spooler.beanassemblers.PositionerAtomAssembler;
import org.eclipse.scanning.event.queues.spooler.beanassemblers.ScanAtomAssembler;
import org.eclipse.scanning.event.queues.spooler.beanassemblers.SubTaskAtomAssembler;
import org.eclipse.scanning.event.queues.spooler.beanassemblers.TaskBeanAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueBeanFactory implements IQueueBeanFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(QueueBeanFactory.class);
	
	private Map<QueueValue<String>, QueueAtom> queueAtomModelRegistry;
	private Map<QueueValue<String>, TaskBean> taskBeanModelRegistry;
	
	private Map<Class<? extends Queueable>, IBeanAssembler<? extends Queueable>> beanAssemblers;
	
	@SuppressWarnings("rawtypes")
	private Map<QueueValue<String>, IQueueValue> globalValueRegistry;
	/*
	 * Global value IQueueValues are expected to have a ? type argument.
	 * Having this registry with rawtype IQueueValue means we don't get a 
	 * compile error for the {@link #unregisterOperation}
	 */
	
	private QueueValue<String> defaultTaskBeanShortName;
	private boolean explicitDefaultTaskBean = false;
	
	public QueueBeanFactory() {
		queueAtomModelRegistry = new HashMap<>();
		taskBeanModelRegistry = new HashMap<>();
		globalValueRegistry = new HashMap<>();
		
		beanAssemblers = new HashMap<>();
		beanAssemblers.put(MonitorAtom.class, new MonitorAtomAssembler(this));
		beanAssemblers.put(PositionerAtom.class, new PositionerAtomAssembler(this));
		beanAssemblers.put(ScanAtom.class, new ScanAtomAssembler(this));
		beanAssemblers.put(SubTaskAtom.class, new SubTaskAtomAssembler(this));
		beanAssemblers.put(TaskBean.class, new TaskBeanAssembler(this));
	}
	
	/**
	 * Generic method to register an element in one of the registries.
	 * @param key String reference name
	 * @param value V element to be registered
	 * @param registry Map registry to add element to
	 * @throws QueueModelException if the reference is already registered
	 */
	private <V> void registerOperation(QueueValue<String> key, V value, Map<QueueValue<String>, V> registry) throws QueueModelException {
		if (registry.containsKey(key)) {
			logger.error("Cannot register "+value.getClass().getSimpleName()+". The reference '"+key+"' is already registered.");
			throw new QueueModelException("A "+value.getClass().getSimpleName()+" with reference '"+key+"' is already registered.");
		}
		registry.put(key, value);
	}
	
	/**
	 * Generic method to remove an element from one of the registries.
	 * @param key String reference name
	 * @param registry Map registry to remove element from
	 * @param clazz Class type of the element to be removed
	 * @throws QueueModelException if the reference is not registered 
	 */
	private <V> void unregisterOperation(QueueValue<String> key, Map<QueueValue<String>, V> registry, Class<V> clazz) throws QueueModelException {
		if (registry.containsKey(key)) {
			registry.remove(key);
		} else{
			logger.error("Cannot unregister "+clazz.getSimpleName()+". Reference for '"+key+"' is not present.");
			throw new QueueModelException("No "+clazz.getSimpleName()+" registered for reference '"+key+"'");
		}
	}

	@Override
	public <Q extends QueueAtom> void registerAtom(Q atom) throws QueueModelException{
		//Variable=true because we're going to look for QueueValues with variable=true in other code
		registerOperation(new QueueValue<>(atom.getShortName(), true), atom, queueAtomModelRegistry);
	}

	@Override
	public void unregisterAtom(String reference) throws QueueModelException {
		//Variable=true because we're going to look for QueueValues with variable=true in other code
		unregisterOperation(new QueueValue<>(reference, true), queueAtomModelRegistry, QueueAtom.class);
	}

	@Override
	public void registerTask(TaskBean task) throws QueueModelException {
		//Variable=true because we're going to look for QueueValues with variable=true in other code
		QueueValue<String> taskShortName = new QueueValue<>(task.getShortName(), true);
		registerOperation(taskShortName, task, taskBeanModelRegistry);
		
		/*
		 * Decide whether we should set the default TaskbeanModel by 
		 * implication or not...
		 */
		if (taskBeanModelRegistry.size() == 1) {
			//Don't change the setting of explicit here, there's no need and this would be a side-effect
			defaultTaskBeanShortName = taskShortName;
		} else if (!explicitDefaultTaskBean) {
				defaultTaskBeanShortName = null;
		}
		/*
		 * Otherwise an explicit default has been set and we should leave the 
		 * current default TaskBean model alone
		 */
	}

	@Override
	public void unregisterTask(String reference) throws QueueModelException {
		//Variable=true because we're going to look for QueueValues with variable=true in other code
		QueueValue<String> unRegRef = new QueueValue<>(reference, true);
		unregisterOperation(unRegRef, taskBeanModelRegistry, TaskBean.class);
		//If we got this far, the class was unregistered (otherwise we get an exception
		if (defaultTaskBeanShortName == unRegRef) defaultTaskBeanShortName = null;
	}

	@Override
	public void registerGlobalValue(IQueueValue<?> value) throws QueueModelException {
		//Variable=true because we're going to look for QueueValues with variable=true in other code
		registerOperation(new QueueValue<String>(value.getName(), true), value, globalValueRegistry);
	}

	@Override
	public void unregisterGlobalValue(String valueName) throws QueueModelException {
		//Variable=true because we're going to look for QueueValues with variable=true in other code
		unregisterOperation(new QueueValue<String>(valueName, true), globalValueRegistry, IQueueValue.class);
	}
	
	@Override
	public IQueueValue<?> getGlobalValue(QueueValue<String> reference) throws QueueModelException {
		if (globalValueRegistry.containsKey(reference)) {
			return globalValueRegistry.get(reference);
		}
		logger.error("No global value with the short name "+reference+" found in registry.");
		throw new QueueModelException("No global value with the short name "+reference+" found in registry.");
	}

	@Override
	public List<QueueValue<String>> getGlobalValuesRegister() {
		return new ArrayList<>(globalValueRegistry.keySet());
	}

	@Override
	public List<QueueValue<String>> getQueueAtomRegister() {
		return new ArrayList<>(queueAtomModelRegistry.keySet());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Q extends QueueAtom> Q assembleQueueAtom(QueueValue<String> reference, ExperimentConfiguration config) throws QueueModelException {
		if (queueAtomModelRegistry.containsKey(reference)) {
			Q protoAtom = (Q)queueAtomModelRegistry.get(reference);
			IBeanAssembler<Q> beanAss = (IBeanAssembler<Q>) beanAssemblers.get(protoAtom.getClass());
			return beanAss.assemble(protoAtom, config);
		}
		logger.error("No QueueAtom with the short name "+reference+" found in registry.");
		throw new QueueModelException("No QueueAtom with the short name "+reference+" found in registry.");
	}

	@Override
	public TaskBean assembleTaskBean(QueueValue<String> reference, ExperimentConfiguration config) throws QueueModelException {
		TaskBean tbModel = taskBeanModelRegistry.get(reference);
		if (tbModel == null) {
			logger.error("Failed to assemble TaskBean: No TaskBeanModel registered for reference'"+reference+"'");
			throw new QueueModelException("No TaskBeanModel registered for reference'"+reference+"'");
		}
		@SuppressWarnings("unchecked")
		IBeanAssembler<TaskBean> beanAss = (IBeanAssembler<TaskBean>) beanAssemblers.get(TaskBean.class);
		return beanAss.assemble(tbModel, config);
	}
	
	@Override
	public void setDefaultTaskBeanModel(String reference) {
		defaultTaskBeanShortName = new QueueValue<String>(reference, true);
		explicitDefaultTaskBean = true;
	}

	@Override
	public String getDefaultTaskBeanModelName() throws QueueModelException {
		if (defaultTaskBeanShortName == null) {
			logger.error("No default TaskBeanModel set");
			throw new QueueModelException("No default TaskBeanModel set");
		}
		return defaultTaskBeanShortName.evaluate();
	}

	@Override
	public List<QueueValue<String>> getTaskBeanRegister() {
		return new ArrayList<>(taskBeanModelRegistry.keySet());
	}
	
}
