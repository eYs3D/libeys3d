cmake_minimum_required(VERSION 2.8.3)
project(eys3d_robot)

## Find catkin macros and libraries
## if COMPONENTS list like find_package(catkin REQUIRED COMPONENTS xyz)
## is used, also find other catkin packages
find_package(catkin REQUIRED COMPONENTS
  geometry_msgs
  roscpp
  rospy
  rosserial_python
  sensor_msgs
  riki_msgs
  std_msgs
  tf
)
catkin_package(
  INCLUDE_DIRS include
  LIBRARIES eys3d_robot
  CATKIN_DEPENDS roscpp rospy tf nav_msgs riki_msgs geometry_msgs sensor_msgs std_msgs 
  DEPENDS system_lib
)

###########
## Build ##
###########

include_directories(
  include
  ${catkin_INCLUDE_DIRS}
)

add_library(eys3d_robot_base src/riki_base.cpp)
add_executable(eys3d_robot_base_node src/riki_base_node.cpp)
target_link_libraries(eys3d_robot_base_node eys3d_robot_base ${catkin_LIBRARIES})

