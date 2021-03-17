import os
import sys

import argparse

from eys3d import Device, Config, COLOR_RAW_DATA_TYPE, DEPTH_RAW_DATA_TYPE

from cv_demo import cv_sample
from pc_demo import pc_sample
from accuracy_demo import accuracy_sample
from callback_demo import callback_sample
from record_playback import record_playback_sample

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--device_index",
        "-d",
        default=0,
        type=int,
        dest="device_index",
        help="which device would like to initialize. Default is 0.")
    parser.add_argument("--module",
                        "-m",
                        default="8062",
                        type=str,
                        help="module name. Default module is 8062.")
    parser.add_argument(
        "--index",
        "-i",
        default=1,
        type=int,
        help="mode index for config setting. Default index is 1.")
    args = parser.parse_args()
    #pipe = Pipeline(args.device_index)
    device = Device(camera_index=args.device_index)
    conf = Config()

    if "8053" == args.module:
        # conf.set_color_stream(
        #     COLOR_RAW_DATA_TYPE.COLOR_RAW_DATA_MJPG,
        #     1280,
        #     720,
        #     fps=30,
        # )
        # conf.set_depth_stream(
        #     DEPTH_TRANSFER_CTRL.DEPTH_IMG_COLOR_TRANSFER,
        #     640,
        #     360,
        #     fps=30,
        # )
        # conf.set_depth_dataType(DEPTH_RAW_DATA_TYPE.DEPTH_RAW_DATA_11_BITS)

        conf.set_preset_mode_config(0x138, args.index)
    elif "8036" == args.module:
        conf.set_color_stream(
            COLOR_RAW_DATA_TYPE.COLOR_RAW_DATA_YUY2,
            1280,
            720,
            fps=30,
        )
        conf.set_depth_stream(
            DEPTH_TRANSFER_CTRL.DEPTH_IMG_NON_TRANSFER,  # DEPTH_TRANSFER_CTRL
            1280,
            720,
            fps=30,
        )
        conf.set_depth_dataType(DEPTH_RAW_DATA_TYPE.DEPTH_RAW_DATA_14_BITS)
    elif "8062" == args.module:
        conf.set_preset_mode_config(0x162, args.index)
    else:
        print("Please input correct module name")
        sys.exit()

    if conf.get_config()['colorWidth']:
        sample_list = {
            "cv_demo": cv_sample,
            "pc_demo": pc_sample,
            "callback_demo": callback_sample,
            "accuracy_demo": accuracy_sample,
            "record_playback_demo": record_playback_sample
        }
    else:
        sample_list = {
            "cv_demo": cv_sample,
            "callback_demo": callback_sample,
            "accuracy_demo": accuracy_sample
        }

    while True:
        print("\n\n\n\tCamera module: {}, mode index: {}, device index: {}.".
              format(args.module, args.index, args.device_index))
        print("\tSample code: ")
        for idx, term in enumerate(sample_list):
            print("\t{}. {}".format(idx + 1, term))
        print("\t{}. Exit".format(len(sample_list) + 1))
        sample_index = input(
            "\tPlease input the index of sample you would like to execute(1~{})? \t"
            .format(len(sample_list) + 1))
        try:
            if int(sample_index) in range(1, len(sample_list) + 2):
                sample_index = int(sample_index)
                break
        except ValueError:
            print("\t******Please input again*******")

    if sample_index == len(sample_list) + 1:
        print("Exit sample code")
        sys.exit()
    else:
        list(sample_list.values())[(sample_index) - 1](device, conf)
    # if 1 == sample_index:
    #     cv_sample(device, conf)
    # elif 2 == sample_index:
    #     pc_sample(device, conf)
    # elif 3 == sample_index:
    #     callback_sample(device, conf)
    # elif 4 == sample_index:
    #     accuracy_sample(device, conf)
    # elif 5 == sample_index:
    #     record_playback_sample(device, conf)
    # else:
    #     print("Exit system")
    #     sys.exit()
