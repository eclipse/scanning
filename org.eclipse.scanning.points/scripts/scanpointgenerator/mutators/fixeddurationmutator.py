###
# Copyright (c) 2016 Diamond Light Source Ltd.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#    Gary Yendell - initial API and implementation and/or initial documentation
#    Charles Mita - initial API and implementation and/or initial documentation
# 
###
from scanpointgenerator.core import Mutator


@Mutator.register_subclass("scanpointgenerator:mutator/FixedDurationMutator:1.0")
class FixedDurationMutator(Mutator):
    """Mutator to apply a fixed duration to points of a ScanPointGenerator"""

    def __init__(self, duration):
        """Args:
            duration(float): Duration to apply
        """
        self.duration = duration

    def mutate(self, iterator):
        """
        Applies duration to points in the given iterator, yielding them

        Args:
            iterator: Iterator to mutate

        Yields:
            Point: Mutated points
        """

        for p in iterator:
            p.duration = self.duration
            yield p

    def to_dict(self):
        return {"typeid": self.typeid, "duration": self.duration}

    @classmethod
    def from_dict(cls, d):
        return cls(d["duration"])
