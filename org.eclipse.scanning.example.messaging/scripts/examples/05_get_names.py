from utilities.connections import create_connection, set_handlers, subscribe_all, send_all

conn_dict = {'device-response-topic': "/topic/org.eclipse.scanning.response.device.topic",
             'device-request-topic': "/topic/org.eclipse.scanning.request.device.topic",
             }

class DeviceNamesHandler(object):
    
    def on_message(self, headers, data):
        print(data['deviceType'] + " names are:")
        for device in data['devices']:
            print device['name']
        print("")

handlers = {'device-response-topic':DeviceNamesHandler()}

conn = create_connection()

set_handlers(conn, handlers)

subscribe_all(conn, conn_dict, handlers)

runnable_request = {"@type":"DeviceRequest","deviceType":"RUNNABLE","configure":"true"}
scannable_request = {"@type":"DeviceRequest","deviceType":"SCANNABLE","configure":"true"}

messages = [
            ('device-request-topic', runnable_request, 0),
            ('device-request-topic', scannable_request, 0),
            ]

send_all(conn, conn_dict, messages)

# Required to keep Python running indefinitely.
raw_input()
