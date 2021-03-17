## Support platforms
* Linux - X64

## default mode
color:1280x720 60fps
depth:1280x720 60fps 14bit

* change mode config must modify main.cpp and rebuild
camera_open_config config = {
        0 , 1280 ,720 , 60 ,1280 ,720, 2
};


## Run eYs3D OpenCV
`./run_eys3d_opencv.sh`

## fun list
0: Preview Color , Depth

1: Preview Color, Depth, Grey, Canny

2: Face detect

3: Face mask detect

4: Point cloud view

5: Point cloud view with opengl

6: OpenGL example

I or i: Show opencv build information

Q or q: Exit

## Issue handle
1.Describe the steps

2.Attach error log

3.Attach the core dump file (/bin/core) if any

4.Contact eYs3D FAE

## Build eYs3D OpenCV SDK
`./build.sh`


