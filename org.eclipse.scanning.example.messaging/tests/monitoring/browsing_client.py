# This was an attempt to test a monitoring queue subscriber.
# The additional parameter 'browser-end' that would keep the
# listener active after receiving queue messages does not
# work. The reason for this is unknown, although likely to do
# with the activemq version used.

# Try creating two instances using the following code, and you
# will find they both retrieve all the queue messages, without
# consuming them.

import stomp
from pprint import pprint
queue = "/queue/myqueue"
conn = stomp.Connection11(auto_content_length=False)

class BrowsingListener(stomp.ConnectionListener):
    def on_error(self, headers, message):
        print('received an error "%s"' % message)
    def on_message(self, headers, message):
        # When receiving a browser:end message, the following
        # line will fail, as message = ''
#         data = json.loads(message)
        print("Got message.")
        pprint(message)
        pprint(headers)

conn.set_listener('', BrowsingListener())
conn.start()
conn.connect('admin', 'password', wait=True)

# The browser-end header does not appear to work with our
# activemq version. It would be very useful for monitoring,
# however!

# conn.subscribe(destination=queue, id=1, ack='auto', headers={"browser":"true", "browser-end":"false", "from-seq":"-1"})
conn.subscribe(destination=queue, id=1, ack='auto', headers={"browser":"true", "from-seq":"-1"})

# At this point, all the queued up messages will be received,
# but not consumed. Once all messages are exhausted, the
# Listener will receive a browser:end header, with empty message
# body.
raw_input()
