package org.eclipse.scanning.api.event.queues;

import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.database.ISampleDescriptionService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.scan.models.ScanMetadata;

public interface IQueueSpoolerService {
	
	/**
	 * Generate a {@link TaskBean} from data supplied from the 
	 * {@link ISampleDescriptionService} and submit it to the job-queue of the 
	 * {@link IQueueService}. This method also checks that the bean is ready 
	 * to be submitted before hand.
	 * @param proposalCode String group code of proposal (e.g. CM)
	 * @param proposalNumber long ID number of proposal
	 * @param sampleIdsList List<Long> of sampleIds
	 * @throws QueueModelException if a sampleID is not ready or could not be 
	 *         assembled into a bean
	 * @throws EventException if submission of the bean for a sampleID failed
	 */
	default void submitExperiments(String proposalCode, long proposalNumber, List<Long> sampleIdsList) throws QueueModelException, EventException {
		long[] sampleIds = new long[sampleIdsList.size()];
		for (int i = 0; i < sampleIdsList.size(); i++) {
			long id = sampleIdsList.get(i);
			if (!isExperimentReady(proposalCode, proposalNumber, id)) {
				String missingExpName = getSampleService().getSampleIdNames(proposalCode, proposalNumber).get(id);
				throw new QueueModelException("Cannot submit experiment "+missingExpName+" (ID="+id+"). Database indicates it is not ready");
			}
			sampleIds[i] = id;
		}
		
		Map<Long, ExperimentConfiguration> allConfigs = getSampleService().generateAllExperimentConfiguration(proposalCode, proposalNumber, sampleIds);
		Map<Long, List<ScanMetadata>> allMetadata = getSampleService().generateAllScanMetadata(proposalCode, proposalNumber, sampleIds);
		for (long id : sampleIds) {
			TaskBean task = createBeansForExperiment(allConfigs.get(id), allMetadata.get(id));
			doSubmission(task);
		}
		
	}
	
	/**
	 * Determine whether an experiment with a given ID is marked ready to 
	 * submit according to, for example, the {@link ISampleDescriptionService}.
	 * @param proposalCode
	 * @param proposalNumber
	 * @param id
	 * @return true if the experiment is ready
	 */
	boolean isExperimentReady(String proposalCode, long proposalNumber, long sampleId);
	
	/**
	 * Use the {@link IQueueBeanFactory} to generate a new {@link TaskBean} 
	 * which contains a full description of the planned experiment.
	 * @param config {@link ExperimentConfiguration} to configure 
	 *        {@link ScanRequest}
	 * @param metadata List<{@link ScanMetadata}> to be included in the NeXus 
	 *        file from the scan
	 * @return {@link TaskBean} to be submitted
	 * @throws QueueModelException if construction of the bean failed
	 */
	TaskBean createBeansForExperiment(ExperimentConfiguration config, List<ScanMetadata> metadata) throws QueueModelException;
	
	/**
	 * Submit a {@link TaskBean} to the job-queue of {@link IQueueService}. 
	 * The {@link IQueueControllerService} provides methods to do this.
	 * @param bean {@link TaskBean} to be submitted
	 * @throws EventException if submission of the bean failed
	 */
	void doSubmission(TaskBean bean) throws EventException;
	
	/**
	 * Return the {@link IQueueBeanFactory} used by this service to assemble 
	 * beans.
	 * @return {@link IQueueBeanFactory}
	 */
	IQueueBeanFactory getQueueBeanFactory();
	
	/**
	 * Return the {@link ISampleDescriptionService} used by this service to 
	 * get information about experiments from the database.
	 * @return {@link ISampleDescriptionService}
	 */
	ISampleDescriptionService getSampleService();

}
