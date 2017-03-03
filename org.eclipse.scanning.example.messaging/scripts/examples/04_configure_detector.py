# Configuring will run device.configure(request.getDeviceModel()), setting the
# device model to the one given in the request.

from utilities.listeners import MonitorHandler
from utilities.connections import create_connection, set_handlers, subscribe_all, send_all

conn_dict = {'device-response-topic': "/topic/org.eclipse.scanning.response.device.topic",
             'device-request-topic': "/topic/org.eclipse.scanning.request.device.topic",
             }

handlers = {'device-response-topic':MonitorHandler()}

conn = create_connection()

set_handlers(conn, handlers)

subscribe_all(conn, conn_dict, handlers)


device_request_message = {'@type':'DeviceRequest',
                          'deviceType':'RUNNABLE',
                          'configure':'true',
                          'deviceName':'dkExmpl',
                          'deviceAction':'CONFIGURE',

                          'deviceModel': {
                                          '@type': 'DarkImageModel',
                                          'columns': 100,
                                          'frequency': 100,
                                          'name': 'meep',
                                          'rows': 72
                                         }
                          }
messages = [
            ('device-request-topic', device_request_message, None),
            ]

send_all(conn, conn_dict, messages)
 
# Required to keep Python running indefinitely.
raw_input()