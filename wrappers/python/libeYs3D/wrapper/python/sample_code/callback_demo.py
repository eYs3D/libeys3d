import sys
import time

import eys3d

from eys3d import Pipeline, COLOR_RAW_DATA_TYPE, DEPTH_RAW_DATA_TYPE

DURATION = 100
count = 0
timestamp = 0


def color_frame_callback(frame):
    print("[Python][COLOR] The S/N in callback function: {}".format(
        frame.serialNumber))
    print("[Python][COLOR] The S/N in IMU Dataset: {}, count: {}".format(
        frame.sensorDataSet.serialNumber, frame.sensorDataSet.actualDataCount))


def depth_frame_callback(frame):
    print("[Python][DEPTH] The S/N in callback function: {}".format(
        frame.serialNumber))

    # For calculating callback fps
    # global count, DURATION, timestamp
    # if (count % DURATION) == 0:
    #     if count != 0:
    #         temp = (frame.tsUs - timestamp) / 1000 / DURATION
    #         print("[FPS][DEPTH] {:.2f}".format(1000.0 / temp))
    #         timestamp = frame.tsUs
    # count += 1


def imu_data_callback(sensor_data):
    print("[Python][IMU] The S/N in callback function: {}".format(
        sensor_data.serialNumber))


def callback_sample(device, config):
    pipe = Pipeline(device=device)
    conf = config

    if conf.interleavefps:
        pipe.enable_interleave_mode()
    pipe.start(conf,
               colorFrameCallback=color_frame_callback,
               depthFrameCallback=depth_frame_callback,
               IMUDataCallback=imu_data_callback)
    print("********[PIPELINE] Start stream with callback function********")
    time.sleep(10)
    pipe.pause()
    print("********[PIPELINE] Pause stream********")
    time.sleep(1)
    pipe.start()
    print("********[PIPELINE] Start previous stream********")
    time.sleep(2)
    pipe.stop()
    print("********[PIPELINE] Stop stream********")
    pipe.start(conf)
    print("********[PIPELINE] Start stream without callback function********")
    time.sleep(1)
    pipe.stop()
    print("********[PIPELINE] Stop stream********")
