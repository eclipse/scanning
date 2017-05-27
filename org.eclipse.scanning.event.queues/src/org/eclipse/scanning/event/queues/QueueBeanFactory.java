package org.eclipse.scanning.event.queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.SubTaskAtomModel;
import org.eclipse.scanning.api.event.queues.models.TaskBeanModel;

public class QueueBeanFactory implements IQueueBeanFactory {
	
//TODO	private static final Logger logger = LoggerFactory.getLogger(QueueBeanFactory.class);
	
	private List<String> queueAtomShortNameRegistry;
	
	private Map<String, QueueAtom> queueAtomRegistry;
	private Map<String, SubTaskAtomModel> subTaskModelRegistry;
	private Map<String, TaskBeanModel> taskBeanModelRegistry;
	
	public QueueBeanFactory() {
		queueAtomShortNameRegistry = new ArrayList<>();
		
		queueAtomRegistry = new HashMap<>();
		subTaskModelRegistry = new HashMap<>();
		taskBeanModelRegistry = new HashMap<>();
	}

	@Override
	public <Q extends QueueAtom> void registerAtom(Q atom) throws QueueModelException{
		String atomShortName = atom.getShortName();
		if (queueAtomShortNameRegistry.contains(atomShortName)) {
//TODO			logger.error("Cannot register atom. An atom or bean with the shortname "+atomShortName+" is already registered.");
			throw new QueueModelException("An atom or bean with the shortname "+atomShortName+" is already registered.");
		}
		
		queueAtomRegistry.put(atomShortName, atom);
		queueAtomShortNameRegistry.add(atomShortName);

	}

	@Override
	public void registerAtom(SubTaskAtomModel subTask) throws QueueModelException {
		String subTaskShortName = subTask.getShortName();
		if (queueAtomShortNameRegistry.contains(subTaskShortName)) {
//TODO			logger.error("Cannot register SubTaskAtomModel. An atom or bean with the shortname "+subTaskShortName+" is already registered.");
			throw new QueueModelException("An atom or bean with the shortname "+subTaskShortName+" is already registered.");
		}
		subTaskModelRegistry.put(subTaskShortName, subTask);
		queueAtomShortNameRegistry.add(subTaskShortName);
	}

	@Override
	public void registerTask(TaskBeanModel task) throws QueueModelException {
		// TODO Auto-generated method stub
		/*
		 * TODO When there is one bean, this should be set as default
		 * When there are two beans, default should be unset (require explicit user input).
		 */

	}

	@Override
	public List<String> getQueueAtomRegister() {
		return queueAtomShortNameRegistry;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Q extends QueueAtom> Q getQueueAtom(String shortName) throws QueueModelException {
		if (queueAtomShortNameRegistry.contains(shortName)) {
			if (queueAtomRegistry.containsKey(shortName)) {
				return (Q)queueAtomRegistry.get(shortName);
			}
			if (subTaskModelRegistry.containsKey(shortName)) {
				return (Q)assembleSubTaskModel(shortName);
			}
		}
//TODO		logger.error("No QueueAtom with the short name "+shortName+" found in QueueAtom registry.");
		throw new QueueModelException("No QueueAtom with the short name "+shortName+" found in QueueAtom registry.");
	}

	@Override
	public SubTaskAtom assembleSubTaskModel(String modelShortName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TaskBean assembleTaskBeanModel(String modelShortName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TaskBean assembleDefaultTaskBean() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDefaultTaskBeanModel(String modelShortName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDefaultTaskBeanModel() {
		// TODO Auto-generated method stub
		return null;
	}

}
