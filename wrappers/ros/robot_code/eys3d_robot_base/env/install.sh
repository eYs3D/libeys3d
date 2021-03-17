#!/usr/bin/env bash
if [ -z "$EYS3DBASE" ] ; then
echo "export EYS3DBASE=tank" >> ~/.bashrc
echo "export EYS3DLIDAR=rplidar" >> ~/.bashrc
echo "export ROS_IP=$(hostname -I | awk '{print $1;}')" >> ~/.bashrc
echo "export ROS_MASTER_URI=http://$(hostname -I | awk '{print $1;}'):11311" >> ~/.bashrc
sudo cp rplidar.rules /etc/udev/rules.d
sudo cp robobase.rules /etc/udev/rules.d

oldstr="include\/opencv"
newstr="include\/opencv4"
sudo sed -i "s/$oldstr/$newstr/g" /opt/ros/melodic/share/cv_bridge/cmake/cv_bridgeConfig.cmake
sudo sed -i "s/$oldstr/$newstr/g" /opt/ros/melodic/share/image_geometry/cmake/image_geometryConfig.cmake
. ~/.bashrc
fi

