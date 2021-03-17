#!/bin/sh

echo "Project list : "
echo "=================="
echo "1. x86-64"
echo "2. NVIDIA TX2"
echo "=================="
read -p "Please select project: " project

case $project in
        [1]* ) 
        echo "build x86_64 console tester"
                make CPU=X86 BITS=64 clean
                make CPU=X86 BITS=64
                break;;
        [2]* ) 
        echo "build NVIDIA TX2 console tester"
		make CPU=NVIDIA BITS=64 clean
                make CPU=NVIDIA BITS=64
		break;;
        * ) 
        echo "Please selet project no.";;
esac
