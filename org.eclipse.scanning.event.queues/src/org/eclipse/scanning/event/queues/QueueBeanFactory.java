package org.eclipse.scanning.event.queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	private String defaultTaskBeanShortName;
	private boolean explicitDefaultTaskBean = false;
	
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
		if (taskBeanModelRegistry.size() == 1) {
			//Don't change the setting of explicit here, there's no need and this would be a side-effect
			defaultTaskBeanShortName = task.getShortName();
		} else if (!explicitDefaultTaskBean) {
				defaultTaskBeanShortName = null;
		}
		/*
		 * Otherwise the implication is that an explicit default has been set 
		 * and we should leave the current defaultTaskBeanModel alone
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
				return (Q)assembleSubTask(shortName);
			}
		}
//TODO		logger.error("No QueueAtom with the short name "+shortName+" found in QueueAtom registry.");
		throw new QueueModelException("No QueueAtom with the short name "+shortName+" found in QueueAtom registry.");
	}

	@Override
	public SubTaskAtom assembleSubTask(String modelShortName) throws QueueModelException {
		SubTaskAtomModel stModel = subTaskModelRegistry.get(modelShortName);
		
		SubTaskAtom stAtom = new SubTaskAtom(stModel.getName());
		for (String qaShrtNm : stModel.getQueueAtomShortNames()) {
			try {
				QueueAtom qAtom = getQueueAtom(qaShrtNm);
				stAtom.addAtom(qAtom);
			} catch (QueueModelException qme) {
//TODO				logger.error("Could not assemble SubTaskAtom due to missing child atom: "+qme.getMessage());
				throw new QueueModelException("Could not assemble SubTaskAtom: "+qme.getLocalizedMessage(), qme);
			}
		}
		
		return stAtom;
	}

	@Override
	public TaskBean assembleTaskBean(String modelShortName) throws QueueModelException {
		TaskBeanModel tbModel = taskBeanModelRegistry.get(modelShortName);
		
		TaskBean tBean = new TaskBean(tbModel.getName());
		
		return tBean;
	}

	@Override
	public void setDefaultTaskBeanModel(String modelShortName) {
		defaultTaskBeanShortName = modelShortName;
		explicitDefaultTaskBean = true;
	}

	@Override
	public String getDefaultTaskBeanModelName() {
		return defaultTaskBeanShortName;
	}

}
