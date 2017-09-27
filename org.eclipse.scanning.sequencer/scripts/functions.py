from org.eclipse.scanning.sequencer.analysis import IJythonFunction
from org.eclipse.january.dataset import DatasetFactory

class MeanFunction(IJythonFunction):
    
    def process(self, dataset):
        
        mean = dataset.mean()
        return DatasetFactory.createFromObject(mean)


class MaxFunction(IJythonFunction):
    
    def process(self, dataset):
        
        max = dataset.max(False, False)
        return DatasetFactory.createFromObject(max)


class SumFunction(IJythonFunction):
    
    def process(self, dataset):
        
        sum = dataset.sum()
        return DatasetFactory.createFromObject(sum)
       