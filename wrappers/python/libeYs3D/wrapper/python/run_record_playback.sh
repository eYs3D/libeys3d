VENDOR_SDK_ROOT=../../..
export PYTHONPATH=$VENDOR_SDK_ROOT/libeYs3D/out:$PYTHONPATH

if [ -z "$4" ]
then
	python3.7 record_playback.py -m $1 -i $2 -d $3
else
	python3.7 record_playback.py -m $1 -i $2 -d $3 -r
fi
