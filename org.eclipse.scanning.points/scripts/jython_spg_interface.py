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

from scanpointgenerator import LineGenerator
from scanpointgenerator import ArrayGenerator
from scanpointgenerator import SpiralGenerator
from scanpointgenerator import LissajousGenerator
from scanpointgenerator import CompoundGenerator
from scanpointgenerator import RandomOffsetMutator
from scanpointgenerator import CircularROI
from scanpointgenerator import EllipticalROI
from scanpointgenerator import PointROI
from scanpointgenerator import PolygonalROI
from scanpointgenerator import RectangularROI
from scanpointgenerator import SectorROI
from scanpointgenerator import Excluder
from scanpointgenerator import ROIExcluder

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

    def toDict(self):
        return self.generator.to_dict()

    def size(self):
        return self.generator.size
    def getShape(self):
        return self.generator.shape
    def getRank(self):
        return len(self.generator.shape)


class JLineGenerator1D(JavaIteratorWrapper):
    """
    Create a 1D LineGenerator and wrap the points into java Scalar objects
    """

    def __init__(self, name, units, start, stop, num_points, alternate_direction=False):
        super(JLineGenerator1D, self).__init__()

        self.name = name
        line_gen = LineGenerator(name, units, start, stop, num_points, alternate_direction)
        self.generator = CompoundGenerator([line_gen], [], [])
        self.generator.prepare()
        logging.debug(self.generator.to_dict())

    def _iterator(self):

        for point in self.generator.iterator():
            index = point.indexes[0]
            position = point.positions[self.name]
            java_point = Scalar(self.name, index, position)

            yield java_point


class JLineGenerator2D(JavaIteratorWrapper):
    """
    Create a 2D LineGenerator and wrap the points into java Point objects
    """

    def __init__(self, names, units, start, stop, num_points, alternate_direction=False):
        super(JLineGenerator2D, self).__init__()

        start = start.tolist()  # Convert from array to list
        stop = stop.tolist()

        self.names = names
        line_gen = LineGenerator(names, units, start, stop, num_points, alternate_direction)
        self.generator = CompoundGenerator([line_gen], [], [])
        self.generator.prepare()
        logging.debug(self.generator.to_dict())

    def _iterator(self):

        for point in self.generator.iterator():
            index = point.indexes[0]
            x_name = self.names[0]
            y_name = self.names[1]
            x_position = point.positions[x_name]
            y_position = point.positions[y_name]
            java_point = Point(x_name, index, x_position,
                               y_name, index, y_position, False)
            # Set is2D=False

            yield java_point


class JArrayGenerator(JavaIteratorWrapper):
    """
    Create an ArrayGenerator and wrap the points into java Scalar objects
    """

    def __init__(self, name, units, points):
        super(JArrayGenerator, self).__init__()

        points = points.tolist()  # Convert from array to list

        self.name = name
        array_gen = ArrayGenerator(name, units, points)
        self.generator = CompoundGenerator([array_gen], [], [])
        self.generator.prepare()
        logging.debug(self.generator.to_dict())

    def _iterator(self):

        for point in self.generator.iterator():
            index = point.indexes[0]
            position = point.positions[self.name]
            java_point = Scalar(self.name, index, position)

            yield java_point


class JSpiralGenerator(JavaIteratorWrapper):
    """
    Create a SpiralGenerator and wrap the points into java Point objects
    """

    def __init__(self, names, units, centre, radius, scale=1.0, alternate_direction=False):
        super(JSpiralGenerator, self).__init__()

        self.names = names
        spiral_gen = SpiralGenerator(names, units, centre, radius, scale, alternate_direction)
        self.generator = CompoundGenerator([spiral_gen], [], [])
        self.generator.prepare()
        logging.debug(self.generator.to_dict())

    def _iterator(self):

        x_name = self.names[0]
        y_name = self.names[1]

        for point in self.generator.iterator():
            index = point.indexes[0]
            x_position = point.positions[x_name]
            y_position = point.positions[y_name]
            java_point = Point(x_name, index, x_position,
                               y_name, index, y_position, False)
            # Set is2D=False

            yield java_point


class JLissajousGenerator(JavaIteratorWrapper):
    """
    Create a LissajousGenerator and wrap the points into java Point objects
    """

    def __init__(self, names, units, box, num_lobes, num_points):
        super(JLissajousGenerator, self).__init__()

        self.names = names
        liss_gen = LissajousGenerator(names, units, box["centre"],
                [box["width"], box["height"]], num_lobes, num_points)
        self.generator = CompoundGenerator([liss_gen], [], [])
        self.generator.prepare()
        logging.debug(self.generator.to_dict())

    def _iterator(self):

        x_name = self.names[0]
        y_name = self.names[1]

        for point in self.generator.iterator():
            index = point.indexes[0]
            x_position = point.positions[x_name]
            y_position = point.positions[y_name]
            java_point = Point(x_name, index, x_position,
                               y_name, index, y_position, False)
            # Set is2D=False

            yield java_point


class JCompoundGenerator(JavaIteratorWrapper):
    """
    Create a CompoundGenerator and wrap the points into java Point objects
    """

    def __init__(self, iterators, excluders, mutators, duration=-1):
        super(JCompoundGenerator, self).__init__()
        try:  # If JavaIteratorWrapper
            generators = [g for t in iterators for g in t.generator.generators]
        except AttributeError:  # Else call get*() of Java iterator
            generators = [iterator.getPyIterator().generator for iterator in iterators]
        logging.debug("Generators passed to JCompoundGenerator:")
        logging.debug([generator.to_dict() for generator in generators])

        excluders = [excluder.py_excluder for excluder in excluders]
        mutators = [mutator.py_mutator for mutator in mutators]
        # used to detect duplicated excluders/mutators
        mutator_dicts = [m.to_dict() for m in mutators]
        excluder_dicts = [e.to_dict() for e in excluders]
        extracted_generators = []

        for generator in generators:
            if generator.__class__.__name__ == "CompoundGenerator":
                extracted_generators.extend(generator.generators)
                # extract mutators/excluders we haven't already seen
                # it's possible a mutator/excluder was attached to both us and
                # a compound generator we were given
                extracted_mutators = [m for m in generator.mutators if m.to_dict() not in mutator_dicts]
                mutators.extend(extracted_mutators)
                mutator_dicts.extend([m.to_dict() for m in extracted_mutators])
                extracted_excluders = [e for e in generator.excluders if e.to_dict() not in excluder_dicts]
                excluders.extend(extracted_excluders)
                excluder_dicts.extend([e.to_dict() for e in extracted_excluders])
            else:
                extracted_generators.append(generator)
        generators = extracted_generators

        self.generator = CompoundGenerator(generators, excluders, mutators, duration=duration)
        self.generator.prepare()

        self.dimension_names = [reduce(lambda x,y:x+y, (g.axes for g in d.generators))
                for d in self.generator.dimensions]
        self.axes_ordering = sum(self.dimension_names, [])
        self.index_locations = {axis:[axis in names for names in self.dimension_names].index(True)
                for axis in self.axes_ordering}

        logging.debug("Dimension names:")
        logging.debug(self.dimension_names)
        logging.debug("Index Locations:")
        logging.debug(self.index_locations)
        logging.debug("Axes Ordering:")
        logging.debug(self.axes_ordering)

        logging.debug("CompoundGenerator:")
        logging.debug(self.generator.to_dict())

    def _iterator(self):

        for point in self.generator.iterator():

            if len(point.positions.keys()) == 1:
                name = point.positions.keys()[0]
                index = point.indexes[0]
                position = point.positions[name]
                java_point = Scalar(name, index, position)

            elif len(point.positions.keys()) == 2:
                logging.debug([point.indexes, point.positions])

                names = []
                indexes = []
                values = []
                for axis in self.axes_ordering:
                    index = self.index_locations[axis]
                    indexes.append(point.indexes[index])
                    logging.debug([axis, index])
                    values.append(point.positions[axis])
                    names.append(axis)

                java_point = Point(names[1], indexes[1], values[1],
                                   names[0], indexes[0], values[0])
                java_point.setDimensionNames(self.dimension_names)
            else:
                java_point = MapPosition()

                for axis in self.axes_ordering:
                    index = self.index_locations[axis]
                    logging.debug([axis, index])
                    value = point.positions[axis]
                    java_point.put(axis, value)
                    java_point.putIndex(axis, point.indexes[index])

                java_point.setDimensionNames(self.dimension_names)

            yield java_point


class JRandomOffsetMutator(object):

    def __init__(self, seed, axes, max_offset):
        self.py_mutator = RandomOffsetMutator(seed, axes, max_offset)
        logging.debug(self.py_mutator.to_dict())

class JExcluder(object):

    def __init__(self, rois, scannables):
        py_rois = [roi.py_roi for roi in rois]
        self.py_excluder = ROIExcluder(py_rois, scannables)
        logging.debug(self.py_excluder.to_dict())

class JCircularROI(object):

    def __init__(self, centre, radius):
        self.py_roi = CircularROI(centre, radius)
        logging.debug(self.py_roi.to_dict())

class JEllipticalROI(object):

    def __init__(self, centre, semiaxes, angle=0):
        self.py_roi = EllipticalROI(centre, semiaxes, angle)
        logging.debug(self.py_roi.to_dict())

class JPointROI(object):

    def __init__(self, point):
        self.py_roi = PointROI(point)
        logging.debug(self.py_roi.to_dict())

class JPolygonalROI(object):

    def __init__(self, points_x, points_y):
        self.py_roi = PolygonalROI(points_x, points_y)
        logging.debug(self.py_roi.to_dict())

class JRectangularROI(object):

    def __init__(self, start, width, height, angle=0):
        self.py_roi = RectangularROI(start, width, height, angle)
        logging.debug(self.py_roi.to_dict())

class JSectorROI(object):

    def __init__(self, centre, radii, angles):
        self.py_roi = SectorROI(centre, radii, angles)
        logging.debug(self.py_roi.to_dict())
