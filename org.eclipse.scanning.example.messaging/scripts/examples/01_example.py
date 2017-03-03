# This example uses the stomp library directly. Have a look at 'listeners.py'
# and 'connections.py' Python modules to see how I use stomp for the rest of
# the examples.

import stomp
import json
from pprint import pprint

conn_dict = {'device-response-topic': "/topic/org.eclipse.scanning.response.device.topic",
             'device-request-topic': "/topic/org.eclipse.scanning.request.device.topic",
             }

conn = stomp.Connection12(auto_content_length=False)

class MyListener(stomp.ConnectionListener):
    def on_error(self, headers, message):
        print('received an error "%s"' % message)
    def on_message(self, headers, message):
        sub = headers['subscription']
        data = json.loads(message)
        print("Got message on " + conn_dict[sub])
        pprint(data)

conn.set_listener('', MyListener())
conn.start()
conn.connect('admin', 'password', wait=True)

conn.subscribe(destination=conn_dict['device-response-topic'], id='device-request-topic', ack='auto')

message = {"@type":"DeviceRequest","deviceType":"SCANNABLE","deviceName":"T","configure":"true"}
conn.send(body=json.dumps(message), destination=conn_dict['device-request-topic'])

# Required to keep Python running indefinitely.
raw_input()
