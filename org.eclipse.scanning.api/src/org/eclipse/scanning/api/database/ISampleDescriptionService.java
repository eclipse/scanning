package org.eclipse.scanning.api.database;

import java.util.List;
import java.util.Map;

/**
 * 
 * A Service for getting sample information.
 * 
 * Not all database services will have sample information,
 * therefore the various methods are default-ed to throw
 * an IllegalArgumentException.
 * 
 * @author Matthew Gerring
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

}
