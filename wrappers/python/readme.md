# Feature
* Frame callback
* Pipeline
* Preview Pointcloud with openGL.
* Support 8053, 8062
* Interleave mode 
* DepthFilter 
* Accuracy

# Prerequisite
* eYs3D camera module 

## Required
* python3.7
* python3.7-venv
* eSPDI SDK

# Clone this repository with submodules
* git clone git@github.com:eYs3D/python-wrapper.git --recursive

# Clone this repository and init submodules
* git clone git@github.com:eYs3D/python-wrapper.git
* git submodule init
* git submodule update

## Setup enviroment
* sudo apt install python3.7
* sudo apt-get install python3-pip
* sudo apt-get install python3.7-venv
* sudo apt install libpython3.7-dev
* sudo apt-get install python3.7-dev
* sudo apt install libx11-dev
* sudo apt install libudev-dev
* sudo apt install libglfw3
* sudo apt install libglfw3-dev
* sudo apt install libusb-1.0.0-dev

## Create virtual environment
> python3.7 -m venv ./pyEnv <br>
>```console 
>source ./pyEnv/bin/activate 
>```
> If you want to exit environment <br>
> ```console
>deactivate 
>```

## install requirements package with pip

> python3.7 -m pip install -r requirements.txt 


## make shared object

> cd libeYs3D && mkdir build && cd build  <br>
> cmake .. -DCMAKE_CXX_FLAGS="-Wno-format-truncation " && make install -j16

### make clean
> cd libeYs3D/build
> cmake --build . --target clean

# Struct inforamtion
## Frame 
* tsUs
* serialNumber
* width
* height
* actualDataBufferSize
* dataBufferSize
* actualRGBBufferSize
* rgbBufferSize
* actualZDDepthBufferSize
* zdDepthBufferSize
* dataFormat
* rgbFormat
* rgbTranscodingTime
* filteringTime
* sensorDataSet
* depthAccuracyInfo
* rgbData
* rawData
* zData

## PCFrame
* tsUs
* serialNumber
* width
* height
* rgbDataVec
* xyzDataVec
* transcodingTime
* sensorDataSet
* rgbData
* xyzData

## sensorData
* data
* type
* serialNumber

## sensorDataSet
* tsUs
* serialNumber
* sensorDataType
* actualDataCount
* sensorDataVec

# Save File 
> Default is at $HOME/.eYs3D
> Please execute below script if user want to specify save folder.
>```console
>export EYS3D_HOME="The directory user would like"
>```

## Snapshot 
> `$HOME/.eYs3D/snapshots`

## Log file. (Register)
> `$HOME/.eYs3D/logs`

# Code
## test function
> cd libeYs3D/wrapper/python <br>
> sh run_pytest.sh

## Run demo code
> ```console 
> cd libeYs3D/wrapper/python
> sh run_demo.sh [module_name] [mode_index] [camera_index]
> Then select index to execute sample code.
> 1. cv_demo 
> 2. pc_demo
> 3. callback_demo
> 4. accuracy_demo
> 5. record_playback_demo
>```
> ex: If your module is 8062, mode index 1 on ModeConfig.db and the index of camera device is 0.<br>
> ```console 
> sh run_demo.sh 8062 1 0
>```

### Depth Accuracy 
Please execute the `accuracy_demo` to calculate the quality of depth frame. <br>
User should decide the region ratio and ground truth distance in mm to calculate. <br>
Notice this function would not guarantee the performance.<br>


## Run Python-Cli
### Preview 
![Imgur](https://i.imgur.com/eeNXPrO.png)
### Code 
>```console
> cd libeYs3D/wrapper/python/eYs3Dcli
> sh run_cli.sh
### Usage
* cd: Change directory.
* pwd: Print name of current/working directory.
* ls: List.
* (on the leaf) execute: Excute sample code.
* exit:  Exit python-cli 


## Testing pipeline implementation in libeYs3D
> Please follow the instructions  to compile eYs3D.test for testing and verifying libeYs3D <br>
> ```console
> cd libeYs3D && sh run_test.sh 

### Monitor memory usage of eYs3D.test
```console
sudo pmap ${PID_OF_eYs3D.test} | tail -n 1
```
