#!/bin/sh

read -p "Please enter version:  " version
RELEASE_TITLE="HD-DM-OpenCV-SDK"
STANDARD_NAME="${RELEASE_TITLE}-${version}"

rm -rf sdk_out
mkdir -p sdk_out/${STANDARD_NAME}

./build.sh 1
cp -a bin sdk_out/${STANDARD_NAME}

./build.sh 2
cp -a bin sdk_out/${STANDARD_NAME}

./build.sh 3
cp -a bin sdk_out/${STANDARD_NAME}

cp -a asset sdk_out/${STANDARD_NAME}
cp -a cmake sdk_out/${STANDARD_NAME}
cp -a src sdk_out/${STANDARD_NAME}
cp -a eSPDI sdk_out/${STANDARD_NAME}
cp -a include sdk_out/${STANDARD_NAME}
cp -a opencv_libs sdk_out/${STANDARD_NAME}
cp -a CMakeLists.txt sdk_out/${STANDARD_NAME}
cp -a build.sh sdk_out/${STANDARD_NAME}
cp -a run_eys3d_opencv.sh sdk_out/${STANDARD_NAME}
cp -a readme.txt sdk_out/${STANDARD_NAME}

#tar.gz folder
cd sdk_out
rm -rf ${STANDARD_NAME}.tar.gz
tar zcvf ${STANDARD_NAME}.tar.gz ${STANDARD_NAME}


