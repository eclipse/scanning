# This simply creates a number of messages for the test queue.
# These messages can be deleted by running the ack_client, as
# it will consume them.

import stomp
import json
queue = "/queue/myqueue"
conn = stomp.Connection11(auto_content_length=False)

conn.start()
conn.connect('admin', 'password', wait=True)
message = {"hi":"imhere","blob":"cat"}

# Repeat as necessary!
for i in xrange(20):
    conn.send(body=json.dumps(message), destination=queue)
