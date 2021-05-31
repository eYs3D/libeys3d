#!/bin/sh
cd build
echo "run x86_64"
export LD_LIBRARY_PATH=../eSPDI:$LD_LIBRARY_PATH 
export LD_LIBRARY_PATH=../eSPDI/opencv/x86_64/lib/:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=../eSPDI/turbojpeg/x86_64/lib/:$LD_LIBRARY_PATH 
export LD_LIBRARY_PATH=../eSPDI/opencl/x86_64/lib/:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=../wrapper/x86_64/lib/:$LD_LIBRARY_PATH
rm -rf eYs3D
mkdir eYs3D
cp -R ../cfg eYs3D/
sync
./DMPreview
cd ..
