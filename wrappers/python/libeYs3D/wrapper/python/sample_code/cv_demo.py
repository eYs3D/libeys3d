"""
It's a demo to preview 2D frame with opencv.

Usage:
    Hot Keys:
        * Q/q/Esc: Quit
        * E/e: Enable/Disable AE
        * F1: Perform snapshot
        * F2: Dump frame info
        * F3: Dump IMU data
        * I/i: Enable/Disable extend maximum IR value
        * M/m: Increase IR level
        * N/n: Decrease IR level
        * P/p: Enabel/Disable HW PP
"""

import sys
import time

import cv2

from eys3d import Pipeline


def cv_sample(device, config, interleaveMode=False):
    pipe = Pipeline(device=device)
    conf = config
    if conf.get_config()['colorHeight'] is not 0:
        COLOR_ENABLE = True
    else:
        COLOR_ENABLE = False
    pipe.start(conf)
    time.sleep(1)

    if interleaveMode or conf.interleavefps:
        pipe.enable_interleave_mode()

    # Flag defined
    flag = dict({
        'exposure': True,
        'Extend_IR': True,
        'HW_pp': True,
    })

    camera_property = device.get_cameraProperty()
    ir_property = device.get_IRProperty()
    ir_value = ir_property.get_IR_value()
    status = 'play'

    while 1:
        try:
            if COLOR_ENABLE:
                cret, cframe = pipe.get_color_frame()
                if cret:
                    bgr_cframe = cv2.cvtColor(cframe, cv2.COLOR_RGB2BGR)
                    cv2.imshow("Color image", bgr_cframe)
            dret, dframe = pipe.get_depth_frame()
            if dret:
                bgr_dframe = cv2.cvtColor(dframe, cv2.COLOR_RGB2BGR)
                cv2.imshow("Depth image", bgr_dframe)

            status = {
                -1: status,
                27: 'exit',  # Esc
                ord('q'): 'exit',
                ord('Q'): 'exit',
                ord('e'): 'exposure',
                ord('E'): 'exposure',
                65470: 'snapshot',  # F1
                65471: 'dump_frame_info',  # F2
                65472: 'dump_imu_data',  # F3
                ord('i'): 'extend_IR',
                ord('I'): 'extend_IR',
                ord('m'): 'increased_IR',
                ord('M'): 'increased_IR',
                ord('n'): 'decreased_IR',
                ord('N'): 'decreased_IR',
                65361: 'play',  # Left arrow
                ord('p'): 'HW_pp',
                ord('P'): 'HW_pp',
            }[cv2.waitKeyEx(10)]
            if status == 'play':
                pass
            if status == 'exit':
                cv2.destroyAllWindows()
                pipe.pause()
                break
            if status == 'exposure':
                flag["exposure"] = not (flag["exposure"])
                if flag["exposure"]:
                    print("Enable exposure")
                    camera_property.enable_AE()
                else:
                    print("Disable exposure")
                    camera_property.disable_AE()
                status = 'play'
            if status == 'snapshot':
                device.do_snapshot()
                print(status)
                status = 'play'
            if status == 'dump_frame_info':
                device.dump_frame_info()
                print(status)
                status = 'play'
            if status == 'dump_imu_data':
                device.dump_IMU_data()
                print(status)
                status = 'play'
            if status == 'extend_IR':
                flag["Extend_IR"] = not (flag["Extend_IR"])
                if flag["Extend_IR"]:
                    print("Enable extend IR")
                    ir_property.enable_extendIR()
                else:
                    print("Disable extend IR")
                    ir_property.disable_extendIR()
                status = 'play'
            if status == 'increased_IR':
                ir_value = min(ir_value + 1, ir_property.get_IR_max())
                ir_property.set_IR_value(ir_value)
                time.sleep(0.1)
                print("Increase IR, current value = {}".format(ir_value))
                status = 'play'
            if status == 'decreased_IR':
                ir_value = max(ir_value - 1, ir_property.get_IR_min())
                ir_property.set_IR_value(ir_value)
                time.sleep(0.1)
                print("Decrease IR, current value = {}".format(ir_value))
                status = 'play'
            if status == 'HW_pp':
                flag['HW_pp'] = not (flag['HW_pp'])
                if flag["HW_pp"]:
                    device.enable_HWPP()
                    print("Enable HW PP")
                else:
                    device.disable_HWPP()
                    print("Disable HW PP")
                status = 'play'

        except (TypeError, ValueError, cv2.error, KeyError) as e:
            pass
    pipe.stop()
