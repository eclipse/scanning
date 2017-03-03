This bundle requires Jython to create the point generators. 

The point generators are cpython and jython runnable classes which define the order of points generation. 
This approach is used so that point generators may be passed down to hardware devices such as Malcolm and
entirely the same sequence of points is used. It allows the hardware to iterate down the fast direction
ahead of time an construct fast scans using custom devices. On the Java side it is possible to create
custom point iterators, effectively allowing users to write logic, for instance looking at other device
values, when writing a scan. Users may still take advantage of the hardware acceleration because their
point generation scripts even though custom will generate a sequence of points which can be checked and
the fast sequences, if any, found.

The Lib folder here is taken from Jython 2.7.1.b3 which is assumed to be delivered with this bundle. If
the ScanPointGenerator cannot find 'collections' from Jython, it attempts to use this version of the
internal Jython scripts.

The Jython 2.7.1.b3 bundle name is set into org.eclipse.scanning by putting it in the target and using the system
property org.eclipse.scanning.jython.osgi.bundle.name to the bundle name. By default it is uk.ac.diamond.jython
which is provided as a dependency in the target for org.eclipse.scanning. However the system property may be used
to change it.