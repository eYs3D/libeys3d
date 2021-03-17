"""A simple demo to demonstrate DepthAccuracy function.

User need to pass module name , mode index, region ratio and ground truth distance.
This demo would only preview depth frame and calculate the depth accuracy.

Ex: sh run_accuracy_demo.sh 8062 1 0.2 300

Notice: 
Region ratio and ground truth distance are optional.
It is normal if user could not pass region ratio. 

It does not guarantee the performance when depth accuracy is enabled.
"""
import sys
import time

import cv2
import argparse
import numpy as np
from threading import Lock

from eys3d import Pipeline, Config


def accuracy_sample(device, config):
    region_ratio = float(input("Please input region ratio ( 0 ~ 1.0 ): "))
    ground_truth = float(input("Please input ground truth ( 1 ~ 300)(mm): "))
    pipe = Pipeline(device=device)
    conf = config
    pipe.start(conf)

    # Enable interleave if interleave mode
    if conf.interleavefps:
        pipe.enable_interleave_mode()
    # DepthAccuracy
    if region_ratio > 0:
        depthAccuracy = pipe.get_depthAccuracy()
        depthAccuracy.enable()
        depthAccuracy.set_region_ratio(region_ratio)
        depthAccuracy.set_groundTruth_distance(300)
        print("[Python][Accuracy] Enable? {}".format(
            depthAccuracy.is_enabled()))
        print("[Python][Accuracy] Ragion ratio: {:.2f}".format(
            depthAccuracy.get_region_ratio()))
        print("[Python][Accuracy] Ground Truth: {:.2f}".format(
            depthAccuracy.get_groundTruth_distance()))

    time.sleep(1)
    count = 0
    while 1:
        dret, dframe = pipe.get_depth_frame()  #unblock mode
        if not dret:
            pass
        bgr_dframe = cv2.cvtColor(dframe, cv2.COLOR_RGB2BGR)
        if region_ratio > 0:
            (h, w, _) = bgr_dframe.shape
            cv2.rectangle(bgr_dframe, (int(w * (1 - region_ratio) // 2),
                                       int(h * (1 - region_ratio) // 2)),
                          (int(w * (1 + region_ratio) // 2),
                           int(h * (1 + region_ratio) // 2)), (0, 255, 255), 5)
        cv2.imshow("Depth image", bgr_dframe)
        print("[Python][Accuracy] Info: {}".format(pipe.get_accuracy_info()))
        if cv2.waitKey(1) & 0xFF == ord('q'):
            cv2.destroyAllWindows()
            pipe.stop()
            break
