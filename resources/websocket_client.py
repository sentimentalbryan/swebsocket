#!/usr/bin/python

#from websocket import create_connection
#ws = create_connection("ws://localhost:8080/websocket")
#print "Sending 'Hello, World'..."
#ws.send("Hello, World")
#print "Sent"
#print "Reeiving..."
#result =  ws.recv()
#print "Received '%s'" % result
#ws.close()


import websocket
import thread
import time

def on_message(ws, message):
    print message

def on_error(ws, error):
    print error

def on_close(ws):
    print "### closed ###"

def on_open(ws):
    def run(*args):
        for i in range(30000):
            time.sleep(1)
            ws.send("Hello %d" % i)
        time.sleep(1)
        ws.close()
        print "thread terminating..."
    thread.start_new_thread(run, ())


if __name__ == "__main__":
    websocket.enableTrace(True)
    ws = websocket.WebSocketApp("ws://echo.websocket.org/",
                                on_message = on_message,
                                on_error = on_error,
                                on_close = on_close)
    ws.on_open = on_open

    ws.run_forever()
