package org.eclipse.scanning.api.event.queues;

import java.util.List;

import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.SubTaskAtomModel;
import org.eclipse.scanning.api.event.queues.models.TaskBeanModel;

public interface IQueueBeanFactory {
	
	<Q extends QueueAtom> void registerAtom(Q atom) throws QueueModelException;
	
	default <Q extends QueueAtom> void replaceAtom(Q atom) throws QueueModelException {
		unregisterAtom(atom.getShortName());
		registerAtom(atom);
	}
	
	void registerAtom(SubTaskAtomModel subTask) throws QueueModelException;
	
	default void replaceAtom(SubTaskAtomModel subTask) throws QueueModelException {
		unregisterAtom(subTask.getShortName());
		registerAtom(subTask);
	}
	
	void unregisterAtom(String reference) throws QueueModelException;
	
	void registerTask(TaskBeanModel task) throws QueueModelException;
	
	default void replaceTask(TaskBeanModel task) throws QueueModelException {
		unregisterTask(task.getShortName());
		registerTask(task);
	}
	
	void unregisterTask(String reference) throws QueueModelException;
	
	/**
	 * Return a list of all the shortNames of the registered queue atoms (both 
	 * {@link QueueAtom} instances such as {@link PositionerAtom} and 
	 * {@link SubTaskAtomModel}). This provides a common way to access any of 
	 * the objects which can create {@link QueueAtom}s.
	 * 
	 * @return List<String> shortNames of all registered atoms
	 */
	List<String> getQueueAtomRegister();
	
	/**
	 * 
	 * @param reference
	 * @return
	 */
	<Q extends QueueAtom> Q getQueueAtom(String reference) throws QueueModelException;
	
	SubTaskAtom assembleSubTask(String reference)  throws QueueModelException;
	
	TaskBean assembleTaskBean(String reference) throws QueueModelException;
	
	default TaskBean assembleDefaultTaskBean() throws QueueModelException {
		return assembleTaskBean(getDefaultTaskBeanModelName());
	}
	
	void setDefaultTaskBeanModel(String reference);
	
	String getDefaultTaskBeanModelName();

}
