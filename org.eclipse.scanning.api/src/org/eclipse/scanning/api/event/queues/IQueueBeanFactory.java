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
	
	void registerAtom(SubTaskAtomModel subTask) throws QueueModelException;
	
	void registerTask(TaskBeanModel task) throws QueueModelException;
	
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
	 * @param shortName
	 * @return
	 */
	<Q extends QueueAtom> Q getQueueAtom(String shortName) throws QueueModelException;
	
	SubTaskAtom assembleSubTask(String modelShortName)  throws QueueModelException;
	
	TaskBean assembleTaskBean(String modelShortName) throws QueueModelException;
	
	default TaskBean assembleDefaultTaskBean() throws QueueModelException {
		return assembleTaskBean(getDefaultTaskBeanModelName());
	}
	
	void setDefaultTaskBeanModel(String modelShortName);
	
	String getDefaultTaskBeanModelName();

}
