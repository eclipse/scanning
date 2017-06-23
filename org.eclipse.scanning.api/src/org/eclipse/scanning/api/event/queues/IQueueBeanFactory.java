package org.eclipse.scanning.api.event.queues;

import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.SubTaskAtomModel;
import org.eclipse.scanning.api.event.queues.models.TaskBeanModel;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

/**
 * The IQueueBeanFactory stores models for atoms and beans which can be 
 * created and run in the {@link IQueueService}.
 * 
 * The {@link IQueueService} has two tiers of queueable objects: beans and 
 * atoms. {@link QueueAtom}s are stored in the IQueueBeanFactory within a 
 * registry. In both tiers, there is a class which can contain a queue: 
 * {@link TaskBean}s and {@link SubTaskAtom}s, respectively. Each of these 
 * classes have their own model classes {@link TaskBeanModel} and 
 * {@link SubTaskAtomModel} which are stored in two further registries.
 * 
 * The IQueueBeanFactory provides methods to interact with these registries so 
 * that new beans or models can be added by the user for later creation. More 
 * importantly, it provides methods to instantiate {@link TaskBean}s and 
 * {@link SubTaskAtom}s, allowing the construction of hierarchical queues which
 * are used within the {@link IQueueService} to described an experimental 
 * procedure. These models may contain variables which are to be evaluated on 
 * construction; this evaluation step is initiated by the IQueueBeanFactory. //TODO !!!
 * 
 * The IQueueBeanFactory also has the concept of a default 
 * {@link TaskBeanModel} which is the model used to construct a 
 * {@link TaskBean} when no model in particular is requested by the user 
 * (through the {@link IQueueSpoolerService}). //TODO Is this right?
 *  
 * @author Michael Wharmby
 *
 */
public interface IQueueBeanFactory {
	
	/**
	 * Add a new {@link QueueAtom} to the queueAtomRegistry. 
	 * @param atom {@link QueueAtom} to be registered
	 * @throws QueueModelException if an atom with the same 
	 *         shortname/reference already exists
	 */
	<Q extends QueueAtom> void registerAtom(Q atom) throws QueueModelException;
	
	/**
	 * Change the {@link QueueAtom} registered with a particular 
	 * shortname/reference to a different one.
	 * @param atom {@link QueueAtom} with a shortname set that will replace an 
	 *        already registered atom
	 * @throws QueueModelException if no atom is registered with the shortname 
	 *         set on the given atom
	 */
	default <Q extends QueueAtom> void replaceAtom(Q atom) throws QueueModelException {
		unregisterAtom(atom.getShortName());
		registerAtom(atom);
	}
	
//	/**
//	 * Add a new {@link SubTaskAtomMode} to the subTaskAtomRegistry.
//	 * @param subTask {@link SubTaskAtomMode} to be registered
//	 * @throws QueueModelException if an atom with the same 
//	 *         shortname/reference already exists
//	 */
//	void registerAtom(SubTaskAtomModel subTask) throws QueueModelException;
	
//	/**
//	 * Change the {@link SubTaskAtomModel} registered with a particular 
//	 * shortname/reference to a different one.
//	 * @param atom {@link SubTaskAtomModel} with a shortname set that will 
//	 *        replace an already registered atom
//	 * @throws QueueModelException if no atom is registered with the shortname 
//	 *         set on the given atom
//	 */
//	default void replaceAtom(SubTaskAtomModel subTask) throws QueueModelException {
//		unregisterAtom(subTask.getShortName());
//		registerAtom(subTask);
//	}
	
	/**
	 * Remove a {@link QueueAtom} by its reference from the 
	 * queueAtomModelRegistry. 
	 * @param reference String shortname/reference
	 * @throws QueueModelException if no atom is registered with the shortname
	 */
	void unregisterAtom(String reference) throws QueueModelException;
	
	/**
	 * Add a new {@link TaskBean} to the taskBeanModelRegistry. If only one 
	 * {@link TaskBean} is present, this becomes the default. If more 
	 * than one is included, unless {@link #setDefaultTaskBeanModel(String)} 
	 * has been called, the default is unset and the user must explicitly 
	 * choose which {@link TaskBean} should be assembled by default.
	 * @param task {@link TaskBean} to add to the registry
	 * @throws QueueModelException if a task is registered with the shortname 
	 *         set on the given task
	 */
	void registerTask(TaskBean task) throws QueueModelException;
	
	/**
	 * Change the {@link TaskBean} registered with a particular 
	 * shortname/reference to a different one.
	 * @param task {@link TaskBean} with a shortname set that will 
	 *        replace an already registered atom
	 * @throws QueueModelException if no atom is registered with the shortname 
	 *         set on the given atom
	 */
	default void replaceTask(TaskBean task) throws QueueModelException {
		unregisterTask(task.getShortName());
		registerTask(task);
	}
	
	/**
	 * Remove a {@link TaskBean} by its reference/shortname from the 
	 * taskBeanModelRegistry. 
	 * @param reference String shortname/reference
	 * @throws QueueModelException if no task is registered with the shortname 
	 */
	void unregisterTask(String reference) throws QueueModelException;
	
	/**
	 * Add an {@link IQueueValue} to the globalValues registry for inclusion 
	 * in beans being assembled.
	 * @param value {@link IQueueValue}
	 * @throws QueueModelException if a value is already registered with this 
	 *         reference
	 */
	void registerGlobalValue(IQueueValue<?> value) throws QueueModelException;
	
	/**
	 * Exchange the current {@link IQueueValue} associated with the name of 
	 * the new value for the new value.
	 * @param value {@link IQueueValue} to replace with name in registry
	 * @throws QueueModelException if no reference to the name of this value 
	 *         is recorded
	 */
	default void replaceGlobalValue(IQueueValue<?> value) throws QueueModelException{
		unregisterGlobalValue(value.getName());
		registerGlobalValue(value);
	}
	
	/**
	 * Remove a given {@link IQueueValue} from the registry by the reference 
	 * name given.
	 * @param reference String name of {@link IQueueValue} to remove
	 * @throws QueueModelException if no {@link IQueueValue is registered to 
	 *         this reference
	 */
	void unregisterGlobalValue(String reference) throws QueueModelException;
	
	/**
	 * Return the {@link IQueueValue} associated with the reference name. 
	 * @param reference String name of {@link IQueueValue}
	 * @return {@link IQueueValue}
	 */
	IQueueValue<?> getGlobalValue(QueueValue<String> reference);
	
	/**
	 * Return a list of all the shortNames of the registered queue atoms (both 
	 * {@link QueueAtom} instances such as {@link PositionerAtom} and 
	 * {@link SubTaskAtomModel}). This provides a common way to access any of 
	 * the objects which can create {@link QueueAtom}s.
	 * 
	 * @return List<String> shortNames of all registered atoms
	 */
	List<QueueValue<String>> getQueueAtomRegister();
	
	/**
	 * Return an instance of a {@link QueueAtom} template stored in the 
	 * registries by the shortname (reference) given. If the given reference 
	 * resolves to a {@link SubTaskAtomModel}, the 
	 * {@link #assembleSubTask(String)} method is called and the result 
	 * returned.
	 * @param reference String name of atom to return
	 * @param localValues Map containing localValues to be used during assembly
	 *        (can safely be null)
	 * @return Q extends {@link QueueAtom} associated with reference/shortname
	 * @throws QueueModelException if no atom is registered with the reference
	 */
	<Q extends QueueAtom> Q assembleQueueAtom(QueueValue<String> reference, Map<QueueValue<String>, IQueueValue<?>> localValues) throws QueueModelException;
	
//	/**
//	 * Construct a {@link SubTaskAtom} based on the {@link SubTaskAtomModel} 
//	 * registered to the given reference.
//	 * @param reference String shortname of {@link SubTaskAtomModel}
//	 * @param localValues Map containing localValues to be used during assembly
//	 *        (can safely be null)
//	 * @return {@link SubTaskAtom} derived from {@link SubTaskAtomModel}
//	 * @throws QueueModelException if no atom is registered with the reference
//	 */
//	SubTaskAtom assembleSubTask(QueueValue<String> reference, Map<String, IQueueValue<?>> localValues)  throws QueueModelException;
//	
	/**
	 * Construct a {@link TaskBean} based on the {@link TaskBeanModel} 
	 * registered to the given reference.
	 * @param reference String shortname of {@link TaskBeanModel}
	 * @param localValues Map containing localValues to be used during assembly
	 *        (can safely be null)
	 * @return {@link TaskBean} derived from {@link TaskBeanModel}
	 * @throws QueueModelException if no atom is registered with the reference
	 */
	TaskBean assembleTaskBean(QueueValue<String> reference, Map<QueueValue<String>, IQueueValue<?>> localValues) throws QueueModelException;
	
	/**
	 * Constructs a {@link TaskBean} from the current default 
	 * {@link TaskBeanModel}. 
	 * @param localValues Map containing localValues to be used during assembly
	 *        (can safely be null)
	 * @return {@link TaskBean} derived from default {@link TaskBeanModel}
	 * @throws QueueModelException if there is no default model or the 
	 *         currently set default {@link TaskBeanModel} reference could not 
	 *         be found in the taskBeanModelRegistry. 
	 */
	default TaskBean assembleDefaultTaskBean(Map<QueueValue<String>, IQueueValue<?>> localValues) throws QueueModelException {
		return assembleTaskBean(getDefaultTaskBeanModelName(), localValues);
	}
	
	/**
	 * Set an explicit default {@link TaskBeanModel}. This is the model which 
	 * will be used to create {@link TaskBean}s from this factory when no 
	 * other model is specified.
	 * @param reference String shortname of model
	 */
	void setDefaultTaskBeanModel(QueueValue<String> reference);
	
	/**
	 * Return the currently set shortname of the default {@link TaskBeanModel}.
	 * 
	 * @return String shortname of default {@link TaskBeanModel}
	 * @throws QueueModelException if there is no value set for the shortname 
	 *         of the default model
	 */
	QueueValue<String> getDefaultTaskBeanModelName() throws QueueModelException;

}
