from stomp import Connection12
import json
import time
from utilities.listeners import DelegatingListener

def create_connection():
    conn = Connection12(host_and_ports=[('localhost', 61613)], auto_content_length=False)
      
    conn.start()
    conn.connect('admin', 'admin', wait=True)
    
    return conn

def set_handlers(conn, handlers):
    conn.set_listener('', DelegatingListener(handlers))

def subscribe_all(conn, conn_dict, topics):
    for topic in topics:
        conn.subscribe(destination=conn_dict[topic], id=topic, ack='auto')

def send_all(conn, conn_dict, messages):
    for (topic, message, wait_time) in messages:
        conn.send(body=json.dumps(message), destination=conn_dict[topic])
        if wait_time:
            time.sleep(wait_time)
