import time
import uuid
from utilities.connections import create_connection, set_handlers, subscribe_all, send_all
from utilities.tempfiles import print_scan_file_status
from pprint import pprint
import os
import tempfile

conn_dict = {'scanning-submission-queue': "/queue/org.eclipse.scanning.submission.queue",
             'scanning-status-queue': "/queue/org.eclipse.scanning.status.set",
             'scanning-status-topic': "/topic/org.eclipse.scanning.status.topic",
             'position-response-topic': "/topic/org.eclipse.scanning.request.position.topic",
             }

# Used to maintain reference to the same request.
unique_id = str(uuid.uuid4())
# Keeps the last received message on the scanning status topic.
latest_message = ""
# Tempfile to use for scan. Can check file size to determine if used.
fd, tempfile = tempfile.mkstemp(".nxs", prefix="scan_test_")

os.close(fd)

print_scan_file_status(tempfile)


class StatusQueueHandler(object):
    
    def on_message(self, headers, data):
        status = data['status']
        if (status == "SUBMITTED"):
            print("Received message on status queue.")


class StatusTopicHandler(object):
    
    def on_message(self, headers, data):
        global latest_message
        latest_message = data
        
        pc = data['percentComplete']
        
        print("Percentage complete: " + str(pc) + "%")
#         pprint(data)
    
        if (data['status'] == "COMPLETE"):
            print(data['message'])
            print(data['position'])
            print_scan_file_status(tempfile)
        
        if (data['status'] == "TERMINATED"):
            print("Terminated.")


class PositionerResponseHandler(object):
    
    def on_message(self, headers, data):
#         pprint(data)
        print("Positioner movement: " + str(data['position']))


handlers = {'scanning-status-queue': StatusQueueHandler(),
            'scanning-status-topic': StatusTopicHandler(),
            'position-response-topic': PositionerResponseHandler(),
            }

conn = create_connection()

set_handlers(conn, handlers)

subscribe_all(conn, conn_dict, handlers)

submission_time = int(time.time() * 1000)

scan_bean = {"@type":"ScanBean",
             "uniqueId":unique_id,
             "status":"SUBMITTED",
             "percentComplete":0.0,
             "submissionTime":submission_time,
             "scanRequest":{"@type":"ScanRequest",
                            "compoundModel":{"@type":"CompoundModel",
                                             "models":[{"@type":"GridModel",
                                                        "name":"Grid",
                                                        "boundingBox":{"@type":"BoundingBox",
                                                                       "fastAxisName":"stage_x",
                                                                       "slowAxisName":"stage_y",
                                                                       "fastAxisStart":0.0,
                                                                       "fastAxisLength":3.0,
                                                                       "slowAxisStart":0.0,
                                                                       "slowAxisLength":3.0
                                                                       },
                                                        "fastAxisName":"stage_x",
                                                        "slowAxisName":"stage_y",
                                                        "fastAxisPoints":5,
                                                        "slowAxisPoints":5,
                                                        "snake":False
                                                        }]
                                             },
                            "start":{"values":{"p":1.0,"q":2.0,"T":290.0},
                                     "indices":{},
                                     "stepIndex":-1,
                                     "dimensionNames":[["p","q","T"]]
                                     },
                            "end":{"values":{"p":6.0,"q":7.0,"T":295.0},
                                   "indices":{},
                                   "stepIndex":-1,
                                   "dimensionNames":[["p","q","T"]]
                                   },
                            "ignorePreprocess":False,
                            "filePath":tempfile,
                            "detectors":{"mandelbrot": {"@type": "MandelbrotModel",
                                                        "columns": 301,
                                                        "enableNoise": False,
                                                        "escapeRadius": 10.0,
                                                        "exposureTime": 0.1,
                                                        "imaginaryAxisName": "stage_y",
                                                        "maxImaginaryCoordinate": 1.2,
                                                        "maxIterations": 500,
                                                        "maxRealCoordinate": 1.5,
                                                        "name": "mandelbrot",
                                                        "noiseFreeExposureTime": 5.0,
                                                        "points": 1000,
                                                        "realAxisName": "stage_x",
                                                        "rows": 241,
                                                        "saveImage": False,
                                                        "saveSpectrum": False,
                                                        "saveValue": True,
                                                        "timeout": 0},
                                                        }
                            },
             "point":0,
             "size":0,
             "scanNumber":0,
             }

latest_message = scan_bean


def generate_messages(command_list):
    for topic, command, wait_time in command_list:
        msg = create_request(command)
        yield (topic, msg, wait_time)


def create_request(new_status):
    """ Creates a request, using the previously received message and a new status."""
    msg = latest_message
    msg['previousStatus'] = msg['status']
    msg['status'] = new_status
    return msg 


# This is a list of requests to make during my test run.
command_list = [('scanning-submission-queue', "SUBMITTED", 3),
#                 ('scanning-status-topic', "REQUEST_PAUSE", 3),
#                 ('scanning-status-topic', "REQUEST_RESUME", 1),
#                 ('scanning-status-topic', "REQUEST_PAUSE", 1),
#                 ('scanning-status-topic', "REQUEST_RESUME", 0),
                # If paused beforehand, terminate will cause, on the server,
                # REQUEST_RESUME, then RESUME, then TERMINATE to avoid a bug
                # on the example client where a thread ends up hanging. This
                # behaviour may change in the future.
#                 ('scanning-status-topic', "REQUEST_TERMINATE", 0),
                ]

send_all(conn, conn_dict, generate_messages(command_list))

# Required to keep Python running indefinitely.
raw_input()

# You can mess around with the file, then press enter in the terminal to delete it.
os.remove(tempfile)