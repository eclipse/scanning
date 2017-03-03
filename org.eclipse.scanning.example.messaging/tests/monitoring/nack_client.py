# This code was used for testing the behaviour of ack and nack with
# activemq. When a queue message is nack'd, the message delivery
# service ought to resend it to another listener on the quue. This
# did not, however, nack properly. It may be that we are using a
# version of activemq that is too old.

## NACK CODE

import stomp
import json
from pprint import pprint
queue = "/queue/myqueue"
conn = stomp.Connection12(auto_content_length=False)

class NackListener(stomp.ConnectionListener):
    def on_error(self, headers, message):
        print('received an error "%s"' % message)
    def on_message(self, headers, message):
        data = json.loads(message)
        print("Got message.")
        pprint(data)
        pprint(headers)
        conn.nack(headers['message-id'], headers['subscription'])

conn.set_listener('', NackListener())
conn.start()
conn.connect('admin', 'password', wait=True)
conn.subscribe(destination=queue, id=1, ack='auto')

raw_input()
