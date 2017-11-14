package org.eclipse.scanning.api.database;

import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.scan.models.ScanMetadata;

/**
 *
 * A Service for getting sample information.
 *
 * Not all database services will have sample information,
 * therefore the various methods are default-ed to throw
 * an IllegalArgumentException.
 *
 * @author Matthew Gerring
 * @author Michael Wharmby
 *
 */
public interface ISampleDescriptionService {

	/**
	 * This method wraps 'retrieveSamplesAssignedForProposal' without doing further work.
	 * This method is guaranteed to work over JSON text protocol.
	 *
	 * @param proposalCode
	 * @param proposalNumber
	 * @return
	 */
	default <T> List<T> getSamples(String proposalCode, long proposalNumber) {
		throw new IllegalArgumentException("Method getSamples(...) not implemented!");
	}

	/**
	 * This method returns all the names of the experiments which are ready to
	 * run according to 'retrieveSamplesAssignedForProposal'.
	 * @param proposalCode
	 * @param proposalNumber
	 * @return Map{sampleId -> sampleName}
	 */
	default Map<Long, String> getSampleIdNames(String proposalCode, long proposalNumber) {
		throw new IllegalArgumentException("Method getSamples(...) not implemented!");
	}

	/**
	 *
	 * @param proposalCode
	 * @param proposalNumber
	 * @param sampleId
	 * @return SampleInformation object or other composite representing the sample information
	 */
	default <T> T getSampleInformation(String proposalCode, long proposalNumber, long sampleId) {
		throw new IllegalArgumentException("Method getSampleInformation(...) not implemented!");
	}

	/**
	 *
	 * @param proposalCode
	 * @param proposalNumber
	 * @param sampleIds
	 * @return Map{sampleId->SampleInformation}
	 */
	default <T> Map<Long,T> getSampleInformation(String proposalCode, long proposalNumber, long... sampleIds) {
		throw new IllegalArgumentException("Method getSampleInformation(...) not implemented!");
	}

	/**
	 * For a sample in a particular session (proposal code + proposal number)
	 * produce the configuration necessary to configure the experiment. This
	 * method reprocesses the response of the database into a concrete class
	 * rather than a generic.
	 * @param proposalCode
	 * @param proposalNumber
	 * @param sampleId
	 * @return {@link ExperimentConfiguration} object as an explicit type
	 *         rather than a generic.
	 */
	default ExperimentConfiguration generateExperimentConfiguration(String proposalCode, long proposalNumber, long sampleId) {
		throw new IllegalArgumentException("Method getSampleInformation(...) not implemented!");
	}

	/**
	 * For a series of samples in a particular session (proposal code +
	 * proposal number) produce the configuration necessary to configure the
	 * experiment. This  method reprocesses the response of the database into
	 * a concrete class rather than a generic.
	 * @param proposalCode
	 * @param proposalNumber
	 * @param sampleIds
	 * @return Map{sampleId -> {@link ExperimentConfiguration}} with explicit
	 *         object types rather than a generic.
	 */
	default Map<Long, ExperimentConfiguration> generateAllExperimentConfiguration(String proposalCode, long proposalNumber, long... sampleIds) {
		throw new IllegalArgumentException("Method getSampleInformation(...) not implemented!");
	}

	/**
	 * For a sample in a particular session (proposal code + proposal number)
	 * produce the metadata which should be passed into the NeXus file during
	 * the scan, which the user provided to the database.
	 * @param proposalCode
	 * @param proposalNumber
	 * @param sampleIds
	 * @return List<{@link ScanMetadata}> to be passed into the NeXus file
	 */
	default List<ScanMetadata> generateSampleScanMetadata(String proposalCode, long proposalNumber, long sampleIds) {
		throw new IllegalArgumentException("Method getSampleInformation(...) not implemented!");
	}

	/**
	 * For a series of samples in a particular session (proposal code +
	 * proposal number) produce the metadata which should be passed into the
	 * NeXus file during the scan, which the user provided to the database.
	 * @param proposalCode
	 * @param proposalNumber
	 * @param sampleIds
	 * @return Map{sampleId -> List<{@link ScanMetadata}> to be passed into
	 *         the NeXus file
	 */
	default Map<Long, List<ScanMetadata>> generateAllScanMetadata(String proposalCode, long proposalNumber, long... sampleIds) {
		throw new IllegalArgumentException("Method getSampleInformation(...) not implemented!");
	}

}
