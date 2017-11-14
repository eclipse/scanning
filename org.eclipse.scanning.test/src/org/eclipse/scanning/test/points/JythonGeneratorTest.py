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
from org.eclipse.scanning.api.points import Point
from org.eclipse.scanning.api.points import Scalar
from org.eclipse.scanning.api.points import MapPosition
from org.eclipse.scanning.api.points import ScanPointIterator
from org.eclipse.scanning.points import PySerializable
from java.util import ArrayList

## Logging
import logging
# logging.basicConfig(level=logging.DEBUG)


class JavaIteratorWrapper(ScanPointIterator, PySerializable):
    """
    A wrapper class to give a python iterator the while(hasNext()) next()
    operation required of Java Iterators
    """

    def __init__(self):
        self._iterator = self._iterator()  # Store single instance of _iterator()
        self._has_next = None
        self._next = None

    def _iterator(self):
        raise NotImplementedError("Must be implemented in child class")

    def next(self):

        if self._has_next:
            result = self._next
        else:
            result = self._iterator.next()  # Note: No next() in Py3

        self._has_next = None

        return result

    def hasNext(self):

        if self._has_next is None:

            try:
                self._next = self._iterator.next()  # Note: No next() in Py3
            except StopIteration:
                self._has_next = False
            else:
                self._has_next = True

        return self._has_next

    def size(self):
        return self._size



class FixedValueGenerator(JavaIteratorWrapper):
    """
    Create a fixed series of points
    """
    def __init__(self, scannableName, size, value):
        super(FixedValueGenerator, self).__init__()

        self.scannableName = scannableName
        self._size = size
        self.value = value


    def _iterator(self):

        for index in xrange(self._size):
            java_point = Scalar(self.scannableName, index, self.value)
            yield java_point

class MultipliedValueGenerator(JavaIteratorWrapper):
    """
    Create a fixed series of points
    """
    def __init__(self, scannableName, size, value):
        super(MultipliedValueGenerator, self).__init__()

        self.scannableName = scannableName
        self._size = size
        self.value = value


    def _iterator(self):

        for index in xrange(self._size):
            java_point = Scalar(self.scannableName, index, self.value*index)
            yield java_point

class MappedPositionGenerator(JavaIteratorWrapper):
    """
    Create a fixed series of points
    """
    def __init__(self, scannableName, size, value, numberOfScannables):
        super(MappedPositionGenerator, self).__init__()

        self.scannableName = scannableName
        self._size = size
        self.value = value
        self.numberOfScannables = numberOfScannables


    def _iterator(self):

        for index in xrange(self._size):
            java_point = MapPosition()
            for iscannable in xrange(self.numberOfScannables):
                java_point.put(self.scannableName+str(iscannable), index, self.value*index)

            yield java_point

class ExceptionGenerator(JavaIteratorWrapper):
    """
    Create a fixed series of points
    """
    def __init__(self, scannableName, size, value):
        super(ExceptionGenerator, self).__init__()

        self.scannableName = scannableName
        self._size = size
        self.value = value


    def _iterator(self):

        raise Exception("Cannot iterate ExceptionGenerator!")
