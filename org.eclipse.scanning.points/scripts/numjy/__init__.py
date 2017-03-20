###
# Copyright (c) 2016 Diamond Light Source Ltd.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#    Peter Chang - initial API and implementation and/or initial documentation
# 
###
import sys
if sys.hexversion < 0x02040000:
    raise 'Must use python of at least version 2.4'

import os

from jycore import *
from jymaths import *
from jycomparisons import *
