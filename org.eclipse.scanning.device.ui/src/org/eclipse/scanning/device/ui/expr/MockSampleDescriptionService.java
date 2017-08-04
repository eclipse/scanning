package org.eclipse.scanning.device.ui.expr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.database.ISampleDescriptionService;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.scan.models.ScanMetadata;

public class MockSampleDescriptionService implements ISampleDescriptionService {
	
	private Map<Long, String> sampleIDNames;
	
	private void makeFakeExperimentIdNames() {
		// TODO: temporary code for populating the ExperimentView until the real service is available.
		Map<Long, String> fakeExperIdNames = new HashMap<>();
		fakeExperIdNames.put(3245L, "XPDF Test1");
		fakeExperIdNames.put(5324L, "XPDF Test2");
		fakeExperIdNames.put(321345L, "XPDF Test5");
		fakeExperIdNames.put(95222L, "XPDF testgg");
		setSampleIdNames(fakeExperIdNames);
	}
	
	@Override
	public Map<Long, String> getSampleIdNames(String proposalCode, long proposalNumber) {
		if (sampleIDNames == null) {
			makeFakeExperimentIdNames();
		}
		
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
