package org.eclipse.scanning.event.queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.IHasAtomQueue;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.QueueableModel;
import org.eclipse.scanning.api.event.queues.models.SubTaskAtomModel;
import org.eclipse.scanning.api.event.queues.models.TaskBeanModel;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.event.queues.spooler.IBeanAssembler;
import org.eclipse.scanning.event.queues.spooler.PositionerAtomAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueBeanFactory implements IQueueBeanFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(QueueBeanFactory.class);
	
	private List<String> queueAtomShortNameRegistry;
	
	private Map<String, QueueAtom> queueAtomRegistry;
	private Map<String, SubTaskAtomModel> subTaskModelRegistry;
	private Map<String, TaskBeanModel> taskBeanModelRegistry;
	
	private Map<Class<? extends Queueable>, IBeanAssembler<? extends Queueable>> beanAssemblers;
	
	@SuppressWarnings("rawtypes")
	private Map<String, IQueueValue> globalValueRegistry;
	/*
	 * Global value IQueueValues are expected to have a ? type argument.
	 * Having this registry with rawtype IQueueValue means we don't get a 
	 * compile error for the {@link #unregisterOperation}
	 */
	
	private String defaultTaskBeanShortName;
	private boolean explicitDefaultTaskBean = false;
	
	public QueueBeanFactory() {
		queueAtomShortNameRegistry = new ArrayList<>();
		
		queueAtomRegistry = new HashMap<>();
		subTaskModelRegistry = new HashMap<>();
		taskBeanModelRegistry = new HashMap<>();
		globalValueRegistry = new HashMap<>();
		
//		beanAssemblers.put(MonitorAtom.class, new MonitorAtomAssembler());
		beanAssemblers.put(PositionerAtom.class, new PositionerAtomAssembler());
//		beanAssemblers.put(ScanAtom.class, new ScanAtomAssembler());
//		beanAssemblers.put(SubTaskAtom.class, new SubTaskAtomAssembler();
	}
	
	/**
	 * Generic method to register an element in one of the registries.
	 * @param key String reference name
	 * @param value V element to be registered
	 * @param registry Map registry to add element to
	 * @throws QueueModelException if the reference is already registered
	 */
	private <V> void registerOperation(String key, V value, Map<String, V> registry) throws QueueModelException {
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
	private <V> void unregisterOperation(String key, Map<String, V> registry, Class<V> clazz) throws QueueModelException {
		if (registry.containsKey(key)) {
			registry.remove(key);
		}
		logger.error("Cannot unregister "+clazz.getSimpleName()+". The reference '"+key+"' is not registered.");
		throw new QueueModelException("No "+clazz.getSimpleName()+" registered for reference '"+key+"'");
	}

	@Override
	public <Q extends QueueAtom> void registerAtom(Q atom) throws QueueModelException{
		String atomShortName = atom.getShortName();
		registerOperation(atomShortName, atom, queueAtomRegistry);
		queueAtomShortNameRegistry.add(atomShortName);
	}

	@Override
	public void unregisterAtom(String reference) throws QueueModelException {
		//Atom could either be a real QueueAtom or a SubTaskModel...
		if (!queueAtomShortNameRegistry.contains(reference)) {
			logger.error("Cannot unregister atom. No atom registered for reference '"+reference+"'.");
			throw new QueueModelException("No atom registered for reference '"+reference+"'.");
		} else {
			if (queueAtomRegistry.containsKey(reference)) {
				queueAtomRegistry.remove(reference);
				queueAtomShortNameRegistry.remove(reference);
			} else if (subTaskModelRegistry.containsKey(reference)) {
				subTaskModelRegistry.remove(reference);
				queueAtomShortNameRegistry.remove(reference);
			}
		}
	}

	@Override
	public void registerAtom(SubTaskAtomModel subTask) throws QueueModelException {
		String subTaskShortName = subTask.getShortName();
		registerOperation(subTaskShortName, subTask, subTaskModelRegistry);
		queueAtomShortNameRegistry.add(subTaskShortName);
	}

	@Override
	public void registerTask(TaskBeanModel task) throws QueueModelException {
		registerOperation(task.getShortName(), task, taskBeanModelRegistry);
		
		/*
		 * Decide whether we should set the default TaskbeanModel by 
		 * implication or not...
		 */
		if (taskBeanModelRegistry.size() == 1) {
			//Don't change the setting of explicit here, there's no need and this would be a side-effect
			defaultTaskBeanShortName = task.getShortName();
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
		unregisterOperation(reference, taskBeanModelRegistry, TaskBeanModel.class);
		//If we got this far, the class was unregistered (otherwise we get an exception
		if (defaultTaskBeanShortName == reference) defaultTaskBeanShortName = null;
	}

	@Override
	public void registerGlobalValue(IQueueValue<?> value) throws QueueModelException {
		registerOperation(value.getName(), value, globalValueRegistry);
	}

	@Override
	public void unregisterGlobalValue(String valueName) throws QueueModelException {
		unregisterOperation(valueName, globalValueRegistry, IQueueValue.class);
	}
	
	@Override
	public IQueueValue<?> getGlobalValue(String reference) {
		return globalValueRegistry.get(reference);
	}

	@Override
	public List<String> getQueueAtomRegister() {
		return queueAtomShortNameRegistry;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Q extends QueueAtom> Q getQueueAtom(String reference) throws QueueModelException {
		if (queueAtomShortNameRegistry.contains(reference)) {
			if (queueAtomRegistry.containsKey(reference)) {
				Q protoAtom = (Q)queueAtomRegistry.get(reference);
				IBeanAssembler<Q> beanAss = (IBeanAssembler<Q>) beanAssemblers.get(protoAtom.getClass());
				return beanAss.assemble(protoAtom);
			}
			if (subTaskModelRegistry.containsKey(reference)) {
				return (Q)assembleSubTask(reference);
			}
		}
		logger.error("No QueueAtom with the short name "+reference+" found in QueueAtom registry.");
		throw new QueueModelException("No QueueAtom with the short name "+reference+" found in QueueAtom registry.");
	}

	@Override
	public SubTaskAtom assembleSubTask(String reference) throws QueueModelException {
		SubTaskAtomModel stModel = subTaskModelRegistry.get(reference);
		if (stModel == null) {
			logger.error("Failed to assemble SubTaskAtom: No SubTaskAtomModel registered for reference'"+reference+"'");
			throw new QueueModelException("No SubTaskAtomModel registered for reference'"+reference+"'");
		}
		
		SubTaskAtom stAtom = new SubTaskAtom(reference, stModel.getName());
		populateAtomQueue(stModel, stAtom);
		
		return stAtom;
	}

	@Override
	public TaskBean assembleTaskBean(String reference) throws QueueModelException {
		TaskBeanModel tbModel = taskBeanModelRegistry.get(reference);
		if (tbModel == null) {
			logger.error("Failed to assemble TaskBean: No TaskBeanModel registered for reference'"+reference+"'");
			throw new QueueModelException("No TaskBeanModel registered for reference'"+reference+"'");
		}
		
		TaskBean tBean = new TaskBean(reference, tbModel.getName());
		populateAtomQueue(tbModel, tBean);
		
		return tBean;
	}
	
	/**
	 * Used by assembleX methods to get atoms in the queueAtomShortNames Lists 
	 * of a given {@link TaskBeanModel} or {@link SubTaskAtomModel} and put 
	 * them into a new, real atomQueue in an instance of {@link TaskBean} or 
	 * {@link SubTaskAtom} (respectively). 
	 * @param model {@link QueueableModel} instance containing atom list
	 * @param queueHolder {@link IHasAtomQueue} instance to be supplied with 
	 *        atoms
	 * @throws QueueModelException if an atom was not present in the registry
	 */
	private <P extends IHasAtomQueue<T>, Q extends QueueableModel, T extends QueueAtom> void populateAtomQueue(Q model, P queueHolder) throws QueueModelException {
		for (String stShrtNm : model.getQueueAtomShortNames()) {
			try {
				T at = getQueueAtom(stShrtNm);
				queueHolder.addAtom(at);
			} catch (QueueModelException qme) {
				logger.error("Could not assemble SubTaskAtom due to missing child atom: "+qme.getMessage());
				throw new QueueModelException("Could not assemble SubTaskAtom: "+qme.getMessage(), qme);
			}
		}
	}

	@Override
	public void setDefaultTaskBeanModel(String reference) {
		defaultTaskBeanShortName = reference;
		explicitDefaultTaskBean = true;
	}

	@Override
	public String getDefaultTaskBeanModelName() throws QueueModelException {
		if (defaultTaskBeanShortName == null) {
			logger.error("No default TaskBeanModel set");
			throw new QueueModelException("No default TaskBeanModel set");
		}
		return defaultTaskBeanShortName;
	}

}
