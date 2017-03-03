from utilities.listeners import MonitorHandler
from utilities.connections import create_connection, set_handlers, subscribe_all, send_all
 
conn_dict = {'device-response-topic': "/topic/org.eclipse.scanning.response.device.topic",
             'device-request-topic': "/topic/org.eclipse.scanning.request.device.topic",
             }

handlers = {'device-response-topic':MonitorHandler()}

conn = create_connection()

set_handlers(conn, handlers)

subscribe_all(conn, conn_dict, handlers)

request_1 = {"@type":"DeviceRequest","deviceType":"RUNNABLE"} # Get list of runnables
request_2 = {"@type":"DeviceRequest","deviceType":"SCANNABLE"} # Get list of scannables
request_3 = {"@type":"DeviceRequest","deviceType":"SCANNABLE","deviceName":"T"} # Can get values as well

messages = [
            ('device-request-topic', request_1, 1),
            ('device-request-topic', request_2, 1),
            ('device-request-topic', request_3, 1),
            ]

send_all(conn, conn_dict, messages)
  
# Required to keep Python running indefinitely.
raw_input()