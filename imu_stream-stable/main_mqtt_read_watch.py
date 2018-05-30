# this is a test

import paho.mqtt.client as mqtt
import time
import json
from simplecrypt import encrypt, decrypt
import pickle
import numpy as np
from sklearn.cluster import KMeans
import sys

# how bad the clock on watch drift
# assuming a absolute clock in house
# optimize 100ms accuracy timestamp
# no requirement on mqtt rate
# bluetooth broadcasting
# save data locally
# connect to raspberry
# month and day and milliseconds

def on_message(client, userdata, message):
    global mqtt_data,receive_msg
    mqtt_data = message.payload
    receive_msg = True

if __name__ == "__main__":
    mqtt_data = {}
    receive_msg = False
    name = "laptop"
    topic = "tAxiY7W4P58QH5Oq/sensors/watch"
    broker_address = "iot.eclipse.org" # 198.41.30.241
    client = mqtt.Client(name)
    client.on_message = on_message
    client.connect(broker_address, 1883, 60)

    client.subscribe(topic, qos=0)

    while True:
        client.loop_start()
        if receive_msg:
            receive_msg = False
            print(str(mqtt_data))
        time.sleep(1)
        client.loop_stop()
