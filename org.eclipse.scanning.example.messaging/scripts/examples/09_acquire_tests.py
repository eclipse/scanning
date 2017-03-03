import time
import uuid
from utilities.connections import create_connection, set_handlers, subscribe_all, send_all
from utilities.tempfiles import print_scan_file_status
from pprint import pprint
import os
import tempfile

conn_dict = {'acquire-request-topic': "/topic/org.eclipse.scanning.request.acquire.topic",
             'acquire-response-topic': "/topic/org.eclipse.scanning.response.acquire.topic",
             }

# Used to maintain reference to the same request.
unique_id = str(uuid.uuid4())
# Tempfile to use for scan. Can check file size to determine if used.
fd, tempfile = tempfile.mkstemp(".nxs", prefix="acquire_test")

os.close(fd)

print_scan_file_status(tempfile)

class StatusTopicHandler(object):
    
    def on_message(self, headers, data):
        
        pprint(data)
        
        if (data['status'] == "COMPLETE"):
            print("Complete.")
            print_scan_file_status(tempfile)
         
        if (data['status'] == "FAILED"):
            print("Failed.")
            print_scan_file_status(tempfile)
        

handlers = {'acquire-response-topic': StatusTopicHandler(),
            }

conn = create_connection()

set_handlers(conn, handlers)

subscribe_all(conn, conn_dict, handlers)

submission_time = int(time.time() * 1000)
 
acquire_request = {"@type":"AcquireRequest",
                   "uniqueId":"c8f12aee-d56a-49f6-bc03-9c7de9415674",
                   "detectorName":"mandelbrot",
                    "detectorModel":{"@type":"MandelbrotModel",
                                     "name":"mandelbrot",
                                     "exposureTime":0.01,
                                     "maxIterations":500,
                                     "escapeRadius":10.0,
                                     "columns":301,
                                     "rows":241,
                                     "points":1000,
                                     "maxRealCoordinate":1.5,
                                     "maxImaginaryCoordinate":1.2,
                                     "realAxisName":"xNex",
                                     "imaginaryAxisName":"yNex",
                                     "enableNoise":"false",
                                     "noiseFreeExposureTime":5.0,
                                     "timeout":-1
                                     },
                   "filePath":tempfile,
                   "status":"NONE"
                   }

send_all(conn, conn_dict,[("acquire-request-topic", acquire_request, 0)])

# Required to keep Python running indefinitely.
raw_input()

# You can mess around with the file, then press enter in the terminal to delete it.
os.remove(tempfile)