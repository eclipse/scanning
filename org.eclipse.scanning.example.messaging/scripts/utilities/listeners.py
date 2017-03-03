from stomp import ConnectionListener
import json
from pprint import pprint

class DelegatingListener(ConnectionListener):
    """This is designed to monitor messages, and dispatch to the correct handler.
    
    The sole purpose of this class is to allow each subscribed topic to callback a
    separate handler, based on the 'handlers' dictionary. The rest is basic error
    checking.
    
    """
    
    def __init__(self, handlers):
        # Dictionary of subscription name to destination topic / queue.
        self.handlers = handlers
    
    def set_handler(self, subscription, destination):
        self.handlers[subscription] = destination
    
    def delete_handler(self, sub_name):
        self.handlers.pop(sub_name, None)

    def on_error(self, headers, message):
        print('received an error "%s"' % message)
        
    def on_message(self, headers, message):         
        subscription_handler = self.handlers[headers['subscription']]
        
        try:
            data = json.loads(message)
        except ValueError as e:
            # Either empty message (eg. from browser:end) or invalid json
            if message != "":
                raise e
            else:
                data = None
        
        subscription_handler.on_message(headers, data)

        
def retrieve_data(message):
    try:
        data = json.loads(message)
    except ValueError as e:
        # Either empty message (eg. from browser:end) or invalid json
        if message == "":
            data = {}
        else:
            raise e
    return data

class MonitorHandler(object):
    """This is designed to only print the decoded json data."""
    
    def on_message(self, headers, data):
        sub = headers['subscription']
        print("Got message on " + sub)
        pprint(data)