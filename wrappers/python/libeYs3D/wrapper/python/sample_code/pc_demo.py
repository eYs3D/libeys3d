"""
It's a simple demo to preview point cloud with openGL.
It only has been validated on mode index 1, 2 and 3 with module 8062.(HD frame, 30/60 fps)

Usage:
    Hot Keys:
        * Q\q\Esc: Quit
        * F1: Perform snapshot
        * F2: Dump frame info
        * F3: Dump IMU data
        * F/f: Enable/Disable Ply filter
    Mouse:
        * scroll: Zoom in/out
        * left click: Rotate
        * Double left click: Reset position
"""
import sys
import time

from OpenGL.GL import *
from OpenGL.GLU import *
import glfw

import numpy as np
import argparse
import math
import threading
import cv2

from eys3d import Pipeline, Config

xLen = 1280
yLen = 720
ptLen = xLen * yLen

aMaxBox = [-1e10] * 3
aMinBox = [1e10] * 3
aObjCnt = [90, 97, 500]
aEyeCnt = [0, 0, 0]
aViewMtx = (GLfloat * 16)()
mouseTime = 0
bMMov = False
mIn = [None] * 2
mOut = [None] * 2

lock = threading.Lock()
DURATION = 100.0  # For PC fps
timestamp = count = 0  # For PC fps
point_cloud_viewer_format = 0  # For PC format

# Flag defined
flag = {"filter": True}  #ply filter


def SetRot():
    xDif = mOut[0] - mIn[0]
    yDif = mOut[1] - mIn[1]
    dis = math.sqrt(xDif * xDif + yDif * yDif)
    glLoadIdentity()
    aTV = np.subtract(aEyeCnt, aObjCnt)
    glTranslatef(aTV[0], aTV[1], aTV[2])
    glRotatef(dis * 0.1, yDif, xDif, 0)
    glTranslatef(-aTV[0], -aTV[1], -aTV[2])
    glMultMatrixf(aViewMtx)


def InitView():
    glMatrixMode(GL_MODELVIEW)
    glLoadIdentity()
    gluLookAt(aEyeCnt[0], aEyeCnt[1], aEyeCnt[2], aEyeCnt[0], aEyeCnt[1],
              aEyeCnt[2] + 100.0, 0, -1, 0)


def InputKey(window, key, scancode, action, mods):
    # global dev
    if action == glfw.PRESS:
        if key in (glfw.KEY_Q, glfw.KEY_ESCAPE):
            glfw.set_window_should_close(window, True)
        if key == glfw.KEY_F1:
            dev.do_snapshot()
            print("[Python] Do snapshot")
        if key == glfw.KEY_F2:
            dev.dump_frame_info()
            print("[Python] Dump frame info")
        if key == glfw.KEY_F3:
            dev.dump_IMU_data()
            print("[Python] Dump imu data")
        if key == glfw.KEY_F:
            flag["filter"] = not (flag["filter"])
            if flag["filter"]:
                dev.enable_plyFilter()
                print("[Python]Enable Filter")
            else:
                dev.disable_plyFilter()
                print("[Python]Disable Filter")


def InputMouse(window, button, action, mods):
    global mIn
    global bMMov
    global mouseTime
    global aViewMtx
    if button == glfw.MOUSE_BUTTON_LEFT and action == glfw.PRESS:
        glGetFloatv(GL_MODELVIEW_MATRIX, aViewMtx)
        bMMov = True
        mTmp = glfw.get_cursor_pos(window)
        mIn[0] = mTmp[0]
        mIn[1] = mTmp[1]
    elif button == glfw.MOUSE_BUTTON_LEFT and action == glfw.RELEASE:
        bMMov = False

        mtNow = time.time() * 1000
        if mtNow - mouseTime < 500:
            InitView()
        mouseTime = mtNow


def MoveMouse(window, x, y):
    global mIn
    global mOut
    if bMMov:
        mOut[0] = x
        mOut[1] = y
        SetRot()


def DrawPCloud():
    global aXyz, aRgb, pcframe, pc
    try:
        lock.acquire()
        glEnableClientState(GL_VERTEX_ARRAY)
        glVertexPointer(3, GL_FLOAT, 0, aXyz)
        if point_cloud_viewer_format == 0:
            glEnableClientState(GL_COLOR_ARRAY)
            glColorPointer(3, GL_UNSIGNED_BYTE, 0, aRgb)  # aRgb, dRgb
        elif point_cloud_viewer_format == 1:
            glEnableClientState(GL_COLOR_ARRAY)
            glColorPointer(3, GL_UNSIGNED_BYTE, 0, dRgb)  # aRgb, dRgb
        elif point_cloud_viewer_format == 2:
            glColor4f(0, 1, 0, 0)  # Green
        glPointSize(2.0)
        glDrawArrays(GL_POINTS, 0, ptLen)
        glDisableClientState(GL_VERTEX_ARRAY)
        glDisableClientState(GL_COLOR_ARRAY)
        lock.release()
    except:
        lock.release()
        pass


def ScrollMouse(window, x, y):
    glTranslatef(0, 0, y * 8)


def DrawAxis():
    aaA = [[200, 0, 0], [0, -200, 0], [0, 0, -200]]
    aaC = [[1.0, 0, 0], [0, 1.0, 0], [0, 0, 1.0]]
    glLineWidth(3)
    glBegin(GL_LINES)
    for i in range(3):
        glColor3fv(aaC[i])
        glVertex3fv(aObjCnt)
        aT = np.add(aObjCnt, aaA[i])
        glVertex3fv(aT)
    glEnd()


def depth_frame_callback(dframe):
    global dRgb
    dRgb = dframe.rgbData


def pc_frame_callback(pcframe):
    """It's a callback function to grab point cloud frame.

    PCFrame:
        tsUS (int)              : Timestamp(microsec).
        serialNumber (int)      : The serial number of this frame.
        width (int)             : The width of this frame.
        height (int)            : The height of this frame.
        rgbData (:numpy array:) : The rgb data of this frame. The size is (H * W * 3).
        xyzData (:numpy array:) : The xyz data of this frame. The size is (H * W * 3).
        transcodingTime (int)   : For performance benchmark purpose in micro seconds.
    """
    global aXyz, aRgb, count, timestamp
    lock.acquire()
    aXyz = pcframe.xyzData
    aRgb = pcframe.rgbData

    # For calculating PC callback fps
    if (count % DURATION) == 0:
        if count != 0:
            temp = (pcframe.tsUs - timestamp) / 1000 / DURATION
            print("[FPS][PC Callback] {:.2f}".format(1000.0 / temp))
            timestamp = pcframe.tsUs
    count += 1
    lock.release()


def pc_sample(device, config):
    global point_cloud_viewer_format
    while True:
        point_cloud_viewer_format = input(
            "Please input point cloud viewer format(0: Color, 1: Depth input, 2: Single color)? "
        )
        try:
            if int(point_cloud_viewer_format) in (0, 1, 2):
                point_cloud_viewer_format = int(point_cloud_viewer_format)
                break
        except:
            pass
    global dev
    dev = device
    pipe = Pipeline(device=device)
    # if config.get_config()['colorHeight'] is 0:
    #     config.ep0Width = config.ep1Width
    #     config.ep0Height = config.ep1Height
    pipe.start(config,
               depthFrameCallback=depth_frame_callback,
               PCFrameCallback=pc_frame_callback)
    # Enable interleave if interleave mode
    if config.interleavefps:
        pipe.enable_interleave_mode()
    (H, W) = config.get_color_stream_resolution(
    )  # Get the resolution of color stream
    # W = 1280
    # H = 760

    global aXyz, aRgb, pc

    # Initialize the library
    if not glfw.init():
        sys.exit()
    # Create a windowed mode window and its OpenGL context
    window = glfw.create_window(W, H, "point cloud", None, None)
    if not window:
        glfw.terminate()
        sys.exit()
    glfw.set_key_callback(window, InputKey)
    glfw.set_mouse_button_callback(window, InputMouse)
    glfw.set_cursor_pos_callback(window, MoveMouse)
    glfw.set_scroll_callback(window, ScrollMouse)

    # Make the window's context current
    glfw.make_context_current(window)

    # Loop until the user closes the window
    glMatrixMode(GL_PROJECTION)
    glLoadIdentity()
    #glOrtho(-1300, 1300, -800, 800, -2000, 2000)
    #glOrtho(-650, 650, -400, 400, -2000, 2000)
    gluPerspective(75.0, W / H, 0.01,
                   16384.0)  # Set up a perspective projection matrix

    InitView()
    # quad = gluNewQuadric()
    glClearColor(0, 0, 0, 0.5)  # Background color set
    glEnable(GL_DEPTH_TEST)
    fCount = 0

    t1 = time.time()
    while not glfw.window_should_close(window):

        # Render here, e.g. using pyOpenGL
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

        DrawAxis()
        DrawPCloud()

        # Swap front and back buffers
        glfw.swap_buffers(window)

        # Poll for and process events
        glfw.poll_events()

        # For Render FPS


#        fCount += 1
#        if fCount == DURATION:
#            fCount = 0
#            t2 = time_for_fps
#            time_span = (t2 - t1) / 1000.0 / DURATION
#            print("[Python] t2 = {}, time_span = {}".format(t2, time_span))
#            print("[FPS] {:.3f}".format( 1000.0 / time_span ))  # Render FPS
#            t1 = t2

#         # Color and depth frame reference.
#         try:
#             cret, cframe = pipe.get_color_frame()
#             dret, dframe = pipe.get_depth_frame()
#             if cret and dret:
#                 bgr_cframe = cv2.cvtColor(cframe, cv2.COLOR_RGB2BGR)
#                 bgr_dframe = cv2.cvtColor(dframe, cv2.COLOR_RGB2BGR)
#                 cv2.imshow("Color image", bgr_cframe)
#                 cv2.imshow("Depth image", bgr_dframe)
#         except (TypeError, ValueError, cv2.error) as e:
#             pass
#         if cv2.waitKey(1) & 0xff == ord('q'):
#             cv2.destroyAllWindows() # opencv close window
#             break

    glfw.terminate()  # opengl close

    pipe.stop()
