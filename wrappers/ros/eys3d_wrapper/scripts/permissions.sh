#!/usr/bin/env bash

BASE_DIR=$(cd "$(dirname "$0")" && pwd)

source "$BASE_DIR/common/echo.sh"
source "$BASE_DIR/common/host.sh"

# config hid device permission

_linux_config() {
  if [ ! -f "/etc/udev/rules.d/eys3d_depth-8036.rules" ]; then
    _echo_s "config device permission"
    sudo cp $BASE_DIR/config/eys3d_depth-8036.rules /etc/udev/rules.d/
    sudo service udev reload
    sudo service udev restart
  fi
}

if [ "$HOST_OS" = "Linux" ]; then
  _linux_config
fi
