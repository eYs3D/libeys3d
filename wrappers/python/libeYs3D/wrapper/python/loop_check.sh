# Usage
# sh loop_check python_code module_name

export LD_LIBRARY_PATH=../../eSPDI:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=../../eSPDI/x64:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=../../eSPDI/opencv/x86_64/lib/:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=../../eSPDI/turbojpeg/x86_64/lib/:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=../../eSPDI/opencl/x86_64/lib/:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=../../eSPDI/DepthFilter/x86_64/lib:$LD_LIBRARY_PATH
export PYTHONPATH=../../out:$PYTHONPATH

code_name=$1
m=$2


if [ "$m" = "8053" ];then
	i=3
	echo "===================================="
	echo "************Current INDEX $i*********"
	echo "====================================\n\n"
	python3.7 $code_name -m $m -i $i
	sleep 2
elif [ "$m" = "8036" ];then
	echo "8036"

elif [ "$m" = "8062" ];then
	for i in $(seq 1 5)
	do
		echo "===================================="
		echo "************Current INDEX $i*********"
		echo "====================================\n\n"
		python3.7 $code_name -m $m -i $i
		sleep 2
	done
fi

