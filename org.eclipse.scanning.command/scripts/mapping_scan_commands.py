###
# Copyright (c) 2016 Diamond Light Source Ltd.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#    Nic Bricknell - initial API and implementation and/or initial documentation
# 
###
"""A friendly interface to mapping scans.

The most important function here is mscan(), which collates information into a
ScanRequest and submits it to a queue for attention of the GDA server.

Users who want to create a ScanRequest without submitting it to GDA can use the
pure function scan_request(), whose signature is similar to mscan(). The scan
request object can later be submitted to the GDA server with submit().

The following pure functions create scan paths which may be passed to mscan():
grid(), step(), line(), array(), point(), val().

There are also some pure functions which may be used to narrow the region of
interest (ROI) when using grid(). They are: circ(), rect(), poly().
"""

# To use these commands in the GDA Jython REPL:
#
# - To server/main/_common/jython_server_facade.xml in your <beamline>-config,
#   add a gda.jython.ScriptProject with path:
#   ${gda.install.git.loc}/daq-eclipse.git/org.eclipse.scanning.command/scripts
#
# - To localStation.py in your <beamline>-config, add the following line:
#   from mapping_scan_commands import *
#
# - For each detector, you amy use keyword arguments to specific arbitrary fields
#   in their models.
#   e.g. detector('mandelbrot', 0.1, columns=200)
#
#   mscan() will send your updated detector model as part of the ScanRequest.

import sys

from java.lang import System
from java.net import URI
from java.util import HashMap, ArrayList
from org.eclipse.dawnsci.analysis.dataset.roi import (
    CircularROI, RectangularROI, PolygonalROI, PolylineROI, PointROI)
from org.eclipse.scanning.api.event.IEventService import (
    SUBMISSION_QUEUE, STATUS_TOPIC)
from org.eclipse.scanning.api.event.scan import (ScanBean, ScanRequest)
from org.eclipse.scanning.api.points.models import (
    StepModel, CollatedStepModel, GridModel, RasterModel, SinglePointModel,
    OneDEqualSpacingModel, OneDStepModel, ArrayModel,
    BoundingBox, BoundingLine, CompoundModel, RepeatedPointModel)
from org.eclipse.scanning.command.Services import (
    getEventService, getRunnableDeviceService)


# Grepping for 'mscan' in a GDA workspace shows up nothing, so it seems that
# mscan is a free name.
def mscan(path=None, mon=None, det=None, now=False, block=True,
          allow_preprocess=False, broker_uri=None):
    """Create a ScanRequest and submit it to the GDA server.

    A simple usage of this function is as follows:
    >>> mscan(step(my_scannable, 0, 10, 1), det=detector('mandelbrot', 0.1))

    The above invokation says "please perform a mapping scan over my scannable
    from 0 to 10 with step size 1, collecting data from the 'Mandelbrot'
    detector with an exposure time of 0.1 seconds at each step".

    You can specify detector and arbitrary fields in their model with keyword arguments
    >>> mscan(..., det=[detector('mandelbrot', 0.1), detector('another_detector', 0.4, param1='foo', param2='bar')])

    You can specify a scannable or list of scannables to monitor:
    >>> mscan(..., mon=my_scannable, ...)  # or:
    >>> mscan(..., mon=[my_scannable, another_scannable], ...)

    You can embed one scan path inside another to create a compound scan path:
    >>> mscan([step(s, 0, 10, 1), step(f, 1, 5, 1)], ...)

    The above invocation says "for each point from 0 to 10 on my slow axis, do
    a scan from 1 to 5 on my fast axis". In fact, for the above case, a grid-
    type scan would be more idiomatic:
    >>> mscan(grid((f, s), (1, 0), (5, 10), (1, 1)), ...)

    By default, this function will submit the scan request to a queue and
    return only once the scan is complete. You may override this behaviour with
    the "now" and "block" keywords:
    >>> # Return as soon as the scan request is submitted.
    >>> mscan(..., ..., block=False)

    >>> # Skip the queue and wait for scan completion.
    >>> mscan(..., ..., now=True)

    >>> # Skip the queue and return straight after submission.
    >>> mscan(..., ..., now=True, block=False)
    """
    if (broker_uri is None):
        broker_uri = getScanningBrokerUri()
        
    submit(scan_request(path, mon, det, allow_preprocess),
           now, block, broker_uri)


def submit(request, now=False, block=True,
           broker_uri=None):
    
    if (broker_uri is None):
        broker_uri = getScanningBrokerUri()

    """Submit an existing ScanRequest to the GDA server.

    See the mscan() docstring for details of `now` and `block`.
    """
    scan_bean = ScanBean(request) # Generates a sensible name for the scan from the request.
   
    # Throws an exception if we made a bad bean
    json = getEventService().getEventConnectorService().marshal(scan_bean)

    if now:
        raise NotImplementedError()  # TODO: Raise priority.


    submitter = getEventService().createSubmitter(URI(broker_uri), SUBMISSION_QUEUE)
    
    if block:
        submitter.setTopicName(STATUS_TOPIC)
        submitter.blockingSubmit(scan_bean)
    else:
        submitter.submit(scan_bean)

def getScanningBrokerUri():
    
    uri = System.getProperty("org.eclipse.scanning.broker.uri")

    if (uri is None):
        uri = System.getProperty("GDA/gda.activemq.broker.uri")

    if (uri is None):
        uri = System.getProperty("gda.activemq.broker.uri")

    return uri;


def scan_request(path=None, mon=None, det=None, file=None, allow_preprocess=False):
    """Create a ScanRequest object with the given configuration.

    See the mscan() docstring for usage.
    """
    try:
        assert path is not None
    except AssertionError:
        raise ValueError('Scan request must have a scan path.')

    # The effect of the following three lines is to make square brackets
    # optional when calling this function with length-1 lists. I.e. we can do
    # either scan([grid(...)], ...) or scan(grid(...), ...). Also _stringify
    # the monitors so users can pass either a monitor name in quotes or a
    # scannable object from the Jython namespace.
    scan_paths = _listify(path)
    monitors = ArrayList(map(_stringify, _listify(mon)))
    detectors = _listify(det)

    (scan_path_models, _) = zip(*scan_paths)  # zip(* == unzip(

    # ScanRequest expects CompoundModel 
    cmodel = CompoundModel()
    for (model, rois) in scan_paths:
        cmodel.addData(model, rois)

    # (Again, use a HashMap, not a Python dict.)
    detector_map = HashMap()
    for (name, model) in detectors:
        detector_map[name] = model

    return _instantiate(ScanRequest,
                        {'compoundModel': cmodel,
                         'filePath' : file,
                         'monitorNames': monitors,
                         'detectors': detector_map,
                         'ignorePreprocess': not allow_preprocess})


"""
The detector method returns a dictionary of detector name to the detector model.
You must specific the detector name and exposure time
You may optionally add keyword arguments which if set will 
call the appropriate setter methods on the detector model. For instance
if you want to set 'enableNoise' in the model you would have a keyword argument
enableNoise=True so the detector function would be detector("mandelbrot", 0.1, enableNoise=True)
"""
def detector(name, exposure, **kwargs):
    
    detector = getRunnableDeviceService().getRunnableDevice(name)

    assert detector is not None, "Detector '"+name+"' not found."
    
    model = detector.getModel()
    assert model is not None, "The model of detector '"+name+"' appears to be None."
    
    if (exposure > 0):
        model.setExposureTime(exposure)
    
    for key, value in kwargs.iteritems():
        setattr(model, key, value)

    return (name, model)

# Scan paths
# ----------

def step(axis=None, start=None, stop=None, step=None):
    """Define a step scan path to be passed to mscan().

    Note that this function may be called with or without keyword syntax. That
    is, the following are mutually equivalent:
    >>> step(axis=my_scannable, start=0, stop=10, step=1)
    >>> step(my_scannable, 0, 10, 1)
    """
    try:
        assert None not in (axis, start, stop, step)
    except (TypeError, ValueError):
        raise ValueError(
            '`axis`, `start`, `stop` and `step` must be provided.')

    # For the first argument, users can pass either a Scannable object
    # or a string. IScanPathModels are only interested in the string (i.e.
    # the Scannable's name).
    axis = _stringify(axis)

    # No such thing as ROIs for StepModels.
    roi = None

    model = _instantiate(
                StepModel,
                {'name': axis,
                 'start': start,
                 'stop': stop,
                 'step': step})

    return model, _listify(roi)

def cstep(names=None, start=None, stop=None, step=None):
    """Define a step scan path to be passed to mscan().

    Note that this function may be called with or without keyword syntax. That
    is, the following are mutually equivalent:
    >>> step(axis=my_scannable, start=0, stop=10, step=1)
    >>> step(['x','y'], 0, 10, 1)
    
    TODO This command is untested and no scan point generator exists than can do
    this on the Jython side.
    
    """
    try:
        assert None not in (names, start, stop, step)
    except (TypeError, ValueError):
        raise ValueError(
            '`axis`, `start`, `stop` and `step` must be provided.')

    # No such thing as ROIs for StepModels.
    roi = None

    model = _instantiate(
                CollatedStepModel,
                {'start': start,
                 'stop': stop,
                 'step': step,
                 'names': names})

    return model, _listify(roi)


def repeat(axis=None, count=None, value=None, sleep=None):
    """Define a repeat scan path to be passed to mscan().

    Note that this function may be called with or without keyword syntax. That
    is, the following are mutually equivalent:
    >>> step(axis=my_scannable, count=10, value=2.2, sleep=0)
    >>> repeat(my_scannable, 10, 2.2, 0)
    """
    try:
        assert None not in (axis, count, value)
    except (TypeError, ValueError):
        raise ValueError(
            '`axis`, `start`, `stop` and `step` must be provided.')

    # For the first argument, users can pass either a Scannable object
    # or a string. IScanPathModels are only interested in the string (i.e.
    # the Scannable's name).
    axis = _stringify(axis)

    # No such thing as ROIs for StepModels.
    roi = None

    model = _instantiate(
                RepeatedPointModel,
                {'name': axis,
                 'count': count,
                 'value': value,
                 'sleep': sleep})

    return model, _listify(roi)

def grid(axes=None, start=None, stop=None, step=None, count=None, snake=True,
         roi=None):
    """Define a grid scan path to be passed to mscan().

    Required keyword arguments:
    * axes: a pair of scannables (x, y) with the former taken as the fast axis
    * origin: a pair of numbers (x0, y0) specifying a grid corner
    * size: a pair of numbers (w, h) specifying the absolute grid dimensions
    - One of:
      * step: a pair of numbers (dx, dy) specifying spacing of grid divisions
      * count: a pair of integers (r, c) specifying number of grid divisions

    Optional keyword arguments:
    * snake: point order should "snake" through the grid? default True
    * roi: use only grid points from given region(s) of interest. default None

    If no ROIs are given, all grid points are used.

    If multiple regions of interest (ROI) are given, the composite ROI is taken
    as the union of the individual ROIs. E.g.:
    >>> grid(..., roi=[circ((0, 1), 1.5), rect((-1, -1), (0, 0), 0)])
    """
    try:
        assert None not in (axes, start, stop)
    except AssertionError:
        raise ValueError(
            '`axes`, `start` and `stop` must be provided to grid().')

    try:
        assert len(filter(lambda arg: arg is None, (count, step))) == 1
    except AssertionError:
        raise ValueError(
            'Either `step` or `count` must be provided to to grid().')

    try:
        (xName, yName) = map(_stringify, axes)
    except (TypeError, ValueError):
        raise ValueError('`axes` must be a pair of scannables (x, y).')

    try:
        (xStart, yStart) = start
    except (TypeError, ValueError):
        raise ValueError('`start` must be a pair of values (x0, y0).')

    try:
        (xStop, yStop) = stop
    except (TypeError, ValueError):
        raise ValueError('`stop` must be a pair of values (w, h).')

    bbox = _instantiate(BoundingBox,
                        {'fastAxisStart': xStart,
                         'slowAxisStart': yStart,
                         'fastAxisLength': xStop - xStart,
                         'slowAxisLength': yStop - yStart})

    if count is not None:
        try:
            (rows, cols) = count
        except (TypeError, ValueError):
            raise ValueError('`count` must be a pair of integers (r, c).')

        model = _instantiate(
                    GridModel,
                    {'fastAxisName': xName,
                     'slowAxisName': yName,
                     'fastAxisPoints': rows,
                     'slowAxisPoints': cols,
                     'snake': snake,
                     'boundingBox': bbox})

    else:
        try:
            (xStep, yStep) = step
        except (TypeError, ValueError):
            raise ValueError('`step` must be a pair of numbers (dx, dy).')

        model = _instantiate(
                    RasterModel,
                    {'fastAxisName': xName,
                     'slowAxisName': yName,
                     'fastAxisStep': xStep,
                     'slowAxisStep': yStep,
                     'snake': snake,
                     'boundingBox': bbox})

    # We _listify() the ROI inputs, so users can type either
    # roi=circ(x, y, r) or roi=[circ(x, y, r), rect(x, y, w, h, angle)].
    return model, _listify(roi)


# TODO: Add axes=None?
def line(origin=None, length=None, angle=None, count=None, step=None):
    """Define a line segment scan path to be passed to mscan().

    Required keyword arguments:
    * origin: origin of line segment (x0, y0)
    * length: length of line segment
    * angle: angle of line segment, CCW from vec(1, 0), specified in radians
    - One of:
      * count: a number of points, equally spaced, along the line segment
      * step: a distance between points along the line segment
    """
    try:
        assert None not in (origin, length, angle)
    except (TypeError, ValueError):
        raise ValueError(
            '`origin`, `length` and `angle` must be provided to line().')
    try:
        assert len(filter(lambda arg: arg is None, (count, step))) == 1
    except AssertionError:
        raise ValueError(
            'Either `count` or `step` must be provided to line().')

    try:
        (xStart, yStart) = origin
    except (TypeError, ValueError):
        raise ValueError('`origin` must be a pair of values (x0, y0).')

    roi = None

    bline = _instantiate(BoundingLine,
                         {'xStart': xStart,
                          'yStart': yStart,
                          'length': length,
                          'angle': angle})

    if step is not None:
        model = _instantiate(
                    OneDStepModel,
                    {'step': step,
                     'boundingLine': _instantiate(BoundingLine,
                                                  {'xStart': xStart,
                                                   'yStart': yStart,
                                                   'length': length,
                                                   'angle': angle})})

    else:
        model = _instantiate(
                    OneDEqualSpacingModel,
                    {'points': count,
                     'boundingLine': _instantiate(BoundingLine,
                                                  {'xStart': xStart,
                                                   'yStart': yStart,
                                                   'length': length,
                                                   'angle': angle})})

    return model, _listify(roi)


def array(axis=None, values=None):
    """Define an array scan path to be passed to mscan().

    Required keyword arguments:
    * axis: a scannable
    * values: a list of numerical values for the scannable to take
    """
    try:
        assert None not in (axis, values)
    except AssertionError:
        raise ValueError('`axis` and `values` must be provided to array().')

    axis = _stringify(axis)

    roi = None

    # We have to manually call ArrayModel.setPositions,
    # as it takes a (Double... positions) argument.
    amodel = ArrayModel()
    amodel.setName(axis)
    amodel.setPositions(*values)

    return amodel, _listify(roi)


def val(axis=None, value=None):
    """Define a single axis position to be passed to mscan().

    This single-point scan "path" can be used as the innermost scan path in a
    compound scan to ensure a particular scannable is always set to a given
    value when exposures are taken. (A.k.a. "move to keep still".)

    For instance:
    >>> # Step x from 0 to 10, moving y to 5 after each x movement.
    >>> mscan([step(x, 0, 10, 1), val(y, 5)], ...)
    """
    try:
        assert None not in (axis, value)
    except AssertionError:
        raise ValueError('`axis` and `value` must be provided to val().')

    return array(axis, [value])


def point(x, y):
    """Define a point scan path to be passed to mscan().
    """
    roi = None
    return _instantiate(SinglePointModel, {'x': x, 'y': y}), _listify(roi)


# ROIs
# ----

def circ(origin=None, radius=None):
    """Define a circular region of interest (ROI) to be passed to grid().

    For instance:
    >>> circ(origin=(0, 1), radius=3)
    """
    try:
        assert None not in (origin, radius)
    except AssertionError:
        raise ValueError(
            '`origin` and `radius` must be provided to circ().')

    try:
        (x, y) = origin
    except (TypeError, ValueError):
        raise ValueError('`origin` must be a pair of values (x0, y0).')

    return CircularROI(radius, x, y)


def poly(*vertices):
    """Define a polygonal region of interest (ROI) to be passed to grid().

    For instance, a triangle:
    >>> poly((5, 5), (5, 10), (10, 5))

    A square:
    >>> poly((0, 0), (0, 3), (3, 3), (3, 0))
    """
    point_rois = map(lambda (x, y): PointROI(x, y), vertices)

    # PolygonalROI closes the polygon for us.
    return PolygonalROI(PolylineROI(*point_rois))


def rect(origin=None, size=None, angle=0):
    """Define a rectangular region of interest (ROI) to be passed to grid().

    For instance:
    >>> rect((1, 2), (5, 4), 0.1)

    Angles are specified in radians here. If no angle is passed, the angle is
    taken as 0.
    """
    try:
        assert None not in (origin, size)
    except AssertionError:
        raise ValueError(
            '`origin` and `size` must be provided to rect().')

    try:
        (xStart, yStart) = origin
    except (TypeError, ValueError):
        raise ValueError('`origin` must be a pair of values (x0, y0).')

    try:
        (width, height) = size
    except (TypeError, ValueError):
        raise ValueError('`size` must be a pair of values (w, h).')

    return RectangularROI(xStart, yStart, width, height, angle)


# Bean construction
# -----------------

def _instantiate(Bean, params):
    """Instantiate a JavaBean class with the given params.

    `params` is a dictionary containing attributes to call bean setters with.
    For instance, if params contains {'length': 10}, the instantiated bean will
    have its setLength method called with the value 10. If no such method
    exists, a ValueError is thrown.
    """
    bean = Bean()
    setters = filter(lambda x: x.startswith('set'), dir(bean))

    # For each param, call one of the setters.
    for p in params.keys():
        try:
            [setter] = filter(lambda s: p == s[3].lower()+s[4:], setters)
            getattr(bean, setter)(params[p])
        except ValueError:
            raise ValueError(
                "No setter for param '"+p+"' in "+Bean.__name__+".")

    return bean


# Miscellaneous functions
# -----------------------

def _listify(sheep):  # Idempotent.
    # The argument is called "sheep" because it may be either
    # plural ("[a list of] sheep") or singular ("[a] sheep").
    if type(sheep) is list:
        return sheep
    elif sheep is None:
        return []
    else:
        return [sheep]


def _stringify(scannable):
    if isinstance(scannable, basestring):
        return scannable
    else:
        try:
            return scannable.getName()
        except AttributeError:
            raise ValueError(
                str(scannable)+' has no getName() method and is not a string.')
            # TODO: Test for this exception.
