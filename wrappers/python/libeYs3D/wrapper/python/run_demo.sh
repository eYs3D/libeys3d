VENDOR_SDK_ROOT=../../..
PYTHON_EXECUTE=$(which python3.7)
export PYTHONPATH=$VENDOR_SDK_ROOT/libeYs3D/out:$PYTHONPATH

sudo --preserve-env=PYTHONPATH $PYTHON_EXECUTE sample_code/demo.py -m $1 -i $2 -d $3

# Anaconda
# sudo --preserve-env=PYTHONPATH /path/to/anaconda3/envs/bin/python sample_code/demo.py -m $1 -i $2 -d $3
