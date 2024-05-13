from nxt.sensor import generic
import nxt.locator, time, atexit, logging, socket
from nxt import *

# NXT relating code was adapted from its tutorial, see
# https://ni.srht.site/nxt-python/latest/handbook/tutorial.html

# For sending packets using sockets see
# https://docs.python.org/3/library/socket.html

# logging.basicConfig(level=logging.DEBUG)

# For message sending
local = '127.0.0.1'
port = 5555

with nxt.locator.find() as b:
    # Register brick connection termination
    atexit.register(b.close)
    try:
        print(b.get_device_info()[0:2])

        b.play_tone(480, 200)

        # Adapt for other sensors
        button1 = b.get_sensor(nxt.motor.Port.A, nxt.sensor.generic.Touch)
        button2 = b.get_sensor(nxt.motor.Port.B, nxt.sensor.generic.Touch)

        # Measure distances
        while True:
            # Send results
            blocked = button1.get_sample()
            reset = button2.get_sample()
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.connect((local, port))
                s.sendall("{},{}".format(blocked, reset).encode())
            time.sleep(0.1)

    except ConnectionRefusedError:
        print("BrickMusic closed its port, nxt script terminating")
        b.play_tone(600, 100)
        time.sleep(0.2)
        b.play_tone(600, 100)
    except Exception as e:
        print(e)

        for x in range(3):
            b.play_tone(520, 200)
            time.sleep(0.2)
            b.play_tone(505, 200)
            time.sleep(0.2)
            b.play_tone(490, 200)
            time.sleep(0.2)
