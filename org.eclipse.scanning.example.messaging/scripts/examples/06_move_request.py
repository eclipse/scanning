import uuid
from utilities.listeners import MonitorHandler
from utilities.connections import create_connection, set_handlers, subscribe_all, send_all

conn_dict = {'device-response-topic': "/topic/org.eclipse.scanning.response.device.topic",
             'device-request-topic': "/topic/org.eclipse.scanning.request.device.topic",
             
             # This one is used for position reports
             'position-response-topic': "/topic/org.eclipse.scanning.request.position.topic",
             # Use this one to request a position
             'positioner-request-topic': "/topic/org.eclipse.scanning.request.positioner.topic",
             # And this one gives you a final response
             'positioner-response-topic': "/topic/org.eclipse.scanning.response.positioner.topic",
             }

# Used to maintain reference to the same request. This is used by the server example to determine
# the correct ScannablePositioner to use for the move request. This is why a SET command must be
# sent before a GET command.
unique_id = str(uuid.uuid4())

handlers = {'position-response-topic':MonitorHandler(),
            'positioner-response-topic':MonitorHandler(),
            'positioner-request-topic':MonitorHandler(),
            }

conn = create_connection()

set_handlers(conn, handlers)

subscribe_all(conn, conn_dict, handlers)


set_message_1 = {'@type':'PositionerRequest',
                                  'positionType':'SET',
                                  'position': {
                                               'values':{'T':290},
                                               'indices':{'T':0},
                                               'stepIndex': -1,
                                               'dimensionNames':[['T']],
                                               },
                                  'uniqueId': unique_id
                                  }

set_message_2 = {'@type':'PositionerRequest',
                                  'positionType':'SET',
                                  'position': {
                                               'values':{'T':300},
                                               'indices':{'T':0},
                                               'stepIndex': -1,
                                               'dimensionNames':[['T']],
                                               },
                                  'uniqueId': unique_id
                                  }

abort_message = {'@type':'PositionerRequest',
                                    'positionType':'ABORT',
                                    'uniqueId': unique_id
                                    }

get_message = {'@type':'PositionerRequest',
                                  'positionType':'GET',
                                  'uniqueId': unique_id
                                  }

messages = [
            ('positioner-request-topic', set_message_1, 1),
            ('positioner-request-topic', set_message_2, 1),
            
            # This doesn't actually abort the move. Instead, it aborts the series of movements demanded by the
            # ScannablePositioner. You need to send a TERMINATE message on the request.device topic to stop the
            # actual movement.
            ('positioner-request-topic', abort_message, 1),
            
            # Need to plug in the unique id from before to get the positioner to use the GET command
            ('positioner-request-topic', get_message, 1),
            ]

send_all(conn, conn_dict, messages)

# Required to keep Python running indefinitely.
raw_input()