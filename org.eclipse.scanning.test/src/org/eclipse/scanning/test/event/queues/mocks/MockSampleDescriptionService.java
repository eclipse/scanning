package org.eclipse.scanning.test.event.queues.mocks;

import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.database.ISampleDescriptionService;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.scan.models.ScanMetadata;

public class MockSampleDescriptionService implements ISampleDescriptionService {
	
	private Map<Long, String> sampleIDNames;

	@Override
	public Map<Long, String> getSampleIdNames(String proposalCode, long proposalNumber) {
		return sampleIDNames;
	}
	
	public void setSampleIdNames(Map<Long, String> sampleIdNames) {
		this.sampleIDNames = sampleIdNames;
	}

	@Override
	public ExperimentConfiguration generateExperimentConfiguration(String proposalCode, long proposalNumber,
			long sampleId) {
		// TODO Auto-generated method stub
		return ISampleDescriptionService.super.generateExperimentConfiguration(proposalCode, proposalNumber, sampleId);
	}

	@Override
	public List<ScanMetadata> generateSampleScanMetadata(String proposalCode, long proposalNumber, long sampleIds) {
		// TODO Auto-generated method stub
		return ISampleDescriptionService.super.generateSampleScanMetadata(proposalCode, proposalNumber, sampleIds);
	}

	@Override
	public Map<Long, ExperimentConfiguration> generateAllExperimentConfiguration(String proposalCode,
			long proposalNumber, long... sampleIds) {
		// TODO Auto-generated method stub
		return ISampleDescriptionService.super.generateAllExperimentConfiguration(proposalCode, proposalNumber, sampleIds);
	}

	@Override
	public Map<Long, List<ScanMetadata>> generateAllScanMetadata(String proposalCode, long proposalNumber,
			long... sampleIds) {
		// TODO Auto-generated method stub
		return ISampleDescriptionService.super.generateAllScanMetadata(proposalCode, proposalNumber, sampleIds);
	}

}
