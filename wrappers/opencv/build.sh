#!/bin/sh

if [ -z "$1" ]
then
    echo "\$1 is empty"
    echo "Project list : "
    echo "=================="
    echo "1. OpenCV(x86-64)-system default path"
    echo "2. OpenCV(x86-64)-3.4.2 support GTK"
    echo "3. OpenCV(x86-64)-3.4.2 support QT&OpencGL"
    echo "=================="
    echo

    read -p "Please select project: " project
else
    echo "\$1 is NOT empty"
    project=$1
fi

case $project in
        [1]* )
        echo "1. OpenCV(x86-64)-system default path"
            rm -rf build
            rm -rf bin
            mkdir build
            cd build
            cmake ../ -DSupport=default
            make
            make install
            cd ../bin
            ln -s ../asset/
            ln -s ../eSPDI/opencv/x86_64/share/OpenCV/haarcascades/
            mv eys3d_opencv eys3d_opencv_default
        break;;
        [2]* )
        echo "2. OpenCV(x86-64)-3.4.2 support GTK"
            rm -rf build
            rm -rf bin
            mkdir build
            cd build
            cmake ../ -DSupport=gtk
            make -j8
            make install
            cd ../bin
            ln -s ../asset/
            ln -s ../eSPDI/opencv/x86_64/share/OpenCV/haarcascades/
            mv eys3d_opencv eys3d_opencv_gtk
        break;;
        [3]* )
        echo "3. OpenCV(x86-64)-3.4.2 support QT&OpencGL"
            rm -rf build
            rm -rf bin
            mkdir build
            cd build
            cmake ../ -DSupport=opengl
            make -j8
            make install
            cd ../bin
            ln -s ../asset/
            ln -s ../eSPDI/opencv/x86_64/share/OpenCV/haarcascades/
            mv eys3d_opencv eys3d_opencv_opengl
        break;;
        * )
        echo "Please select project no.";;
esac

