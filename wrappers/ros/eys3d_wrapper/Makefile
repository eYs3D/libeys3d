# Copyright 2020 eYs3D Co., Ltd. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
MKFILE_PATH := $(abspath $(lastword $(MAKEFILE_LIST)))
MKFILE_DIR := $(patsubst %/,%,$(dir $(MKFILE_PATH)))

include CommonDefs.mk

#SUDO ?= sudo



CMAKE_BUILD_EXTRA_OPTIONS ?=

.DEFAULT_GOAL := all

.PHONY: help
help:
	@echo "Usage:"
	@echo "  make help      	show help message"
	@echo "  make init      	init project"
	@echo "  make build     	build project"
	@echo "  make install   	build project and install wrapper libs to ros"
	@echo "  make uninstall   	clean wrapper libs from ros"
	@echo "  make dmpreview     build dm_preview package"
	@echo "  make robot    		build eys3d robot"
	@echo "  make robotall    	build eys3d robot and extra ros packages (example slam/navigation)"
	@echo "  make cleanros  	clean ros packages and ros workspace"
	@echo "  make pkg       	package ros"
	@echo "  make clean     	clean wrapper"
	@echo "  make cleanall  	clean all including wrapper and ros"

# all

all: init dmpreview

.PHONY: all

# init

init:
	@$(call echo,Make $@)
	@$(SH) ./scripts/init.sh $(INIT_OPTIONS)

.PHONY: init

# build

build:
	@$(call echo,Make $@)
	@$(call cmake_build,./_build,..,$(CMAKE_BUILD_EXTRA_OPTIONS))

.PHONY: build

# install

install: uninstall build
	@$(call echo,Make $@)
ifeq ($(HOST_OS),Linux)
	@cd ./_build;  make install
else
	@cd ./_build; make install
endif

.PHONY: install

uninstall:
	@$(call echo,Make $@)
ifeq ($(HOST_OS),Linux)
	 @$(call rm,../eys3d_ros/eys3d_ros_ws/src/dm_preview/eYs3D/)
endif

.PHONY: uninstall

# dmpreview

dmpreview: cleanros cleanpkg install
ifeq ($(HOST_OS),Linux)
	@$(call echo,Make $@)
	@cd ../eys3d_ros/eys3d_ros_ws && catkin_make
endif

cleanros:
	@$(call echo,Make $@)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/build/)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/devel/)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/install/)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/src/dm_preview/eYs3D/)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/.catkin_workspace)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/src/CMakeLists.txt)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/src/eys3d_robot/)

.PHONY: dmpreview cleanros

# robot
.PHONY: robot

robot: cleanros copy_robot all
	@$(call echo,Make $@)
copy_robot:	
	@dst=../eys3d_ros/eys3d_ros_ws/src/eys3d_robot/; \
	@$(call echo,Copy ../robot_code/eys3d_robot_base to $$dst ...,1;35); \
	@$(call echo,Copy ../robot_code/eys3d_robot_ctl to $$dst ...,1;35); \
	mkdir ../eys3d_ros/eys3d_ros_ws/src/eys3d_robot/; \
	$(ECHO) "CP: ../robot_code/eys3d_robot_base > $$dst"; cp -Rp "../robot_code/eys3d_robot_base/" "$$dst"; \
	$(ECHO) "CP: ../robot_code/eys3d_robot_ctl > $$dst"; cp -Rp "../robot_code/eys3d_robot_ctl/" "$$dst";

# robotall
.PHONY: robotall

robotall: cleanros copy_ros_packages all
	@$(call echo,Make $@)
copy_ros_packages:	
	@dst=../eys3d_ros/eys3d_ros_ws/src/eys3d_robot/; \
	@$(call echo,Copy ../robot_code/eys3d_robot_base to $$dst ...,1;35); \
	@$(call echo,Copy ../robot_code/eys3d_robot_ctl to $$dst ...,1;35); \
	@$(call echo,Copy ../robot_code/ros_package to $$dst ...,1;35); \
	mkdir ../eys3d_ros/eys3d_ros_ws/src/eys3d_robot/; \
	$(ECHO) "CP: ../robot_code/eys3d_robot_base > $$dst"; cp -Rp "../robot_code/eys3d_robot_base/" "$$dst"; \
	$(ECHO) "CP: ../robot_code/eys3d_robot_ctl > $$dst"; cp -Rp "../robot_code/eys3d_robot_ctl/" "$$dst"; \
	$(ECHO) "CP: ../robot_code/ros_package > $$dst"; cp -Rp "../robot_code/ros_package/" "$$dst";

# pkg

.PHONY: pkg
pkg: cleanros cleanpkg all
	@$(call echo,Make $@)
ifeq ($(HOST_OS),Linux)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/build/)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/devel/)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/install/)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/.catkin_workspace)
	@$(call rm,../eys3d_ros/eys3d_ros_ws/src/CMakeLists.txt)
	
	@dst=$(PKGNAME); \
	$(call echo,Copy ../eys3d_ros/eys3d_ros_ws to $$dst ...,1;35); \
	$(call rm,$$dst); $(ECHO) "CP: ../eys3d_ros/eys3d_ros_ws > $$dst"; cp -Rp "../eys3d_ros/." "$$dst"; \
	rm -rf _pkg_output;mkdir _pkg_output; \
	$(call echo,Compress $$dst.tar.gz ...,1;35); \
	tar -zcf ./_pkg_output/$$dst.tar.gz $$dst; \
	$(call echo,Compress $$dst.tar.gz done,1;35)
else
	$(error "Can't make pkg on $(HOST_OS)")
endif

.PHONY: cleanpkg
cleanpkg:
	@$(call echo,Make $@)
	@$(call rm_f,$(PKGNAME)*)
	@$(call rm,_pkg_output)

# clean

.PHONY: clean
clean:
	@$(call echo,Make $@)
	@$(call rm,./_build/)
	@$(call rm,./_lib_output/)
	@$(call rm,./_pkg_output/)
	@$(call rm_f,$(PKGNAME)*)
	@$(FIND) . -type f -name ".DS_Store" -print0 | xargs -0 rm -f

.PHONY: cleanall
cleanall: clean cleanros cleanpkg
	@$(call echo,Make $@)
	@$(call rm_f,build-*,./apps/)

.PHONY: host
host:
	@$(call echo,Make $@)
	@echo HOST_OS: $(HOST_OS)
	@echo HOST_ARCH: $(HOST_ARCH)
	@echo HOST_NAME: $(HOST_NAME)
	@echo ECHO: $(ECHO)
	@echo CC: $(CC)
	@echo CXX: $(CXX)
	@echo MAKE: $(MAKE)
	@echo BUILD: $(BUILD)
	@echo CMAKE: $(CMAKE)
	@echo LDD: $(LDD)
	@echo FIND: $(FIND)
	@echo PKGNAME: $(PKGNAME)
	@echo CMAKE_BUILD_EXTRA_OPTIONS: $(CMAKE_BUILD_EXTRA_OPTIONS)
