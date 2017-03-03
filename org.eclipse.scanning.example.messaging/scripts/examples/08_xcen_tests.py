import time
import uuid
from utilities.connections import create_connection, set_handlers, subscribe_all, send_all
from pprint import pprint

conn_dict = {'xcen-submission-queue': "/queue/dataacq.xcen.SUBMISSION_QUEUE",
             'xcen-status-queue': "/queue/dataacq.xcen.STATUS_QUEUE",
             'xcen-status-topic': "/topic/dataacq.xcen.STATUS_TOPIC",
             }

# Used to maintain reference to the same request.
unique_id = str(uuid.uuid4())
# Keeps the last received message.
latest_message = ""


class StatusQueueHandler(object):
    
    def on_message(self, headers, data):
        if (data['uniqueId'] != unique_id):
            return
        status = data['status']
        if (status == "SUBMITTED"):
            print("Received message on status queue.")


class StatusTopicHandler(object):
    
    def on_message(self, headers, data):
        if (data['uniqueId'] != unique_id):
            return
        global latest_message
        latest_message = data
        
        pc = data['percentComplete']
        
        print("Percentage complete: " + str(pc) + "%")
#         pprint(data)

        if (data['status'] == "COMPLETE"):
            print(data['message'])
            print("x: " + str(data['x']) + ", y: " + str(data['y']) + ", z: " + str(data['z']))

        if (data['status'] == "TERMINATED"):
            print("Terminated.")


handlers = {'xcen-status-queue': StatusQueueHandler(),
            'xcen-status-topic': StatusTopicHandler(),
            }

conn = create_connection()

set_handlers(conn, handlers)

subscribe_all(conn, conn_dict, handlers)

submission_time = int(time.time() * 1000)
 
xcen_bean = {"@type":"XcenBean",
                           "uniqueId":unique_id,
                           "status":"SUBMITTED",
                           "name":"sapA-x56_A  (10:33:49)",
                           "percentComplete":0.0,
                           "userName":"lkz95212",
                           "submissionTime":submission_time,
                           "beamline":"i04-1",
                           "visit":"nt5073-40",
                           "collection":"sapA-x56_A",
                           "x":0.0,
                           "y":0.0,
                           "z":0.0
                           }

latest_message = xcen_bean


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
command_list = [('xcen-submission-queue', "SUBMITTED", 3),
                ('xcen-status-topic', "REQUEST_PAUSE", 3),
                ('xcen-status-topic', "REQUEST_RESUME", 1),
#                 ('xcen-status-topic', "REQUEST_PAUSE", 1),
#                 ('xcen-status-topic', "REQUEST_RESUME", 0),
                # If paused beforehand, terminate will cause, on the server,
                # REQUEST_RESUME, then RESUME, then TERMINATE to avoid a bug
                # on the example client where a thread ends up hanging. This
                # behaviour may change in the future.
#                 ('xcen-status-topic', "REQUEST_TERMINATE", 0),
                ]

send_all(conn, conn_dict, generate_messages(command_list))


# Required to keep Python running indefinitely.
raw_input()
