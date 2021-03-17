package com.esp.uvc.usbcamera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esp.android.usb.camera.core.EtronCamera;
import com.esp.android.usb.camera.core.USBMonitor;
import com.esp.android.usb.camera.core.USBMonitor.OnDeviceConnectListener;
import com.esp.android.usb.camera.core.USBMonitor.UsbControlBlock;
import com.esp.uvc.R;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class FirmwareVersionSettings extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = "FirmwareVersionSettings";

    // for thread pool
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 10;
    private static ThreadPoolExecutor EXECUTER
        = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Override
    public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         getFragmentManager().beginTransaction()
             .replace(android.R.id.content, new SettingsFragment())
             .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements
        OnClickListener {

        // for accessing USB and USB camera
        private USBMonitor mUSBMonitor = null;
        private EtronCamera mUVCCamera = null;
        private UsbDevice mUsbDevice = null;
        private Context mContext = null;
        private View mRootView;
        private Button mButton_Get;
        private Button mButton_Set;
        private EditText mEditText_ProductId;
        private EditText mEditText_VendorId;
        private EditText mEditText_Serialnum;
        private TextView mTextView_Result;
        private String mAsciString_version;
        private String mHexString_pid;
        private String mHexString_vid;
        private String mAsciString_serialNum;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mContext = getActivity();
            mUSBMonitor = new USBMonitor(mContext, mOnDeviceConnectListener);
            if (mUSBMonitor == null) {
                Log.d(TAG, "Error!! can not get USBMonitor " );
                return;
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mRootView = inflater.inflate(R.layout.firmware_version_settings, null);
            initUI();
            return mRootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mUSBMonitor != null)
                mUSBMonitor.register();

            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera = null;
            }
        }


        @Override
        public void onPause() {
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera = null;
            }

            if (mUSBMonitor != null)
                mUSBMonitor.unregister();

            super.onPause();
        }

        @Override
        public void onDestroy() {
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera = null;
            }

            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }

            super.onDestroy();
        }

        private void initUI() {
            mButton_Get = (Button) mRootView.findViewById(R.id.read_button);
            mButton_Get.setOnClickListener(this);
            mButton_Set = (Button) mRootView.findViewById(R.id.write_button);
            mButton_Set.setOnClickListener(this);
            mButton_Set.setVisibility(View.INVISIBLE);
            mEditText_ProductId = (EditText) mRootView.findViewById(R.id.product_id_edit);
            mEditText_VendorId = (EditText) mRootView.findViewById(R.id.vendor_id_edit);
            mEditText_Serialnum = (EditText) mRootView.findViewById(R.id.serialnum_edit);
            mTextView_Result = (TextView) mRootView.findViewById(R.id.fw_version_result);
        }

        private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
            @Override
            public void onAttach(final UsbDevice device) {
                if (device != null) {
                    mUsbDevice = device;
                } else {
                    final int n = mUSBMonitor.getDeviceCount();
                    if (DEBUG) Log.v(TAG, ">>>> onAttach getDeviceCount:" + n);
                    if (n ==1) {
                        mUsbDevice = mUSBMonitor.getDeviceList().get(0);
                    }
                }
                if (DEBUG) Log.v(TAG, ">>>> onAttach UsbDevice:" + mUsbDevice);
                if (mUsbDevice != null) {
                    mUSBMonitor.requestPermission(mUsbDevice);
                }
            }

            @Override
            public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
                if (mUVCCamera != null) return;
                if (DEBUG) Log.v(TAG, ">>>> onConnect UsbDevice:" + device);
                if (DEBUG) Log.v(TAG, ">>>> onConnect UsbControlBlock:" + ctrlBlock);
                if (DEBUG) Log.v(TAG, ">>>> onConnect createNew:" + createNew);
                if(DEBUG)Log.d(TAG, ">>>> getVenderId:" + ctrlBlock.getVenderId());
                if(DEBUG)Log.d(TAG, ">>>> getProductId:" + ctrlBlock.getProductId());
                if(DEBUG)Log.d(TAG, ">>>> getFileDescriptor:" + ctrlBlock.getFileDescriptor());
                final EtronCamera camera = new EtronCamera();
                EXECUTER.execute(new Runnable() {
                    @Override
                    public void run() {
                        int ret = -1; 
                        try {
                            ret = camera.open(ctrlBlock);
                            Log.i(TAG, "open uvccamera ret:" + ret);
                        } catch (Exception e) {
                            Log.e(TAG, "open uvccamera exception:" + e.toString());
                            return;
                        }
                        if (ret == EtronCamera.EYS_OK && mUVCCamera == null) {
                            mUVCCamera = camera;
                        }
                        else{
                            Log.e(TAG, "open uvccamera ret:" + ret);
                        }
                    }
                });
            }

            @Override
            public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
                if (DEBUG) Log.v(TAG, "onDisconnect");
                if (mUVCCamera != null && device.equals(mUVCCamera.getDevice())) {
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                    mUVCCamera = null;
                }
            }

            @Override
            public void onDetach(final UsbDevice device) {
                Toast.makeText(mContext, R.string.usb_device_detached, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                if (DEBUG) Log.v(TAG, "onCancel:");
            }
        };

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.read_button:
                    doRead();
                    break;

                case R.id.write_button:
                    doWrite();
                    break;
            }
        }

        private void doRead() {
            Log.i(TAG, "doRead...");
            if (mUVCCamera != null) {
                //getFwVersionValue
                mAsciString_version = mUVCCamera.getFwVersionValue();
                if (!TextUtils.isEmpty(mAsciString_version)) {
                    mTextView_Result.setText(mAsciString_version);
                }
                //getPidValue
                mHexString_pid = mUVCCamera.getPidValue();
                if (!TextUtils.isEmpty(mHexString_pid)) {
                    mEditText_ProductId.setText(mHexString_pid);
                }
                //getVidValue
                mHexString_vid = mUVCCamera.getVidValue();
                if (!TextUtils.isEmpty(mHexString_vid)) {
                    mEditText_VendorId.setText(mHexString_vid);
                }
                //getSerialNumberValue
                mAsciString_serialNum = mUVCCamera.getSerialNumberValue();
                if (!TextUtils.isEmpty(mAsciString_serialNum)) {
                    mEditText_Serialnum.setText(mAsciString_serialNum);
                }
            } else {
                Log.i(TAG, "uvc camera do not connect");
                Toast.makeText(mContext, "uvc camera do not connect", Toast.LENGTH_SHORT).show();
            }
        }

        private void doWrite() {
            Log.i(TAG, "doWrite...");
            if (mUVCCamera != null) {
                //setPidValue
                String hexString_pid = mEditText_ProductId.getText().toString();
                String hexString_vid = mEditText_VendorId.getText().toString();
                if (!TextUtils.isEmpty(hexString_pid) &&
                    !TextUtils.isEmpty(hexString_pid)) {
                    int nPid = Integer.parseInt(hexString_pid, 16);
                    int nVid = Integer.parseInt(hexString_vid, 16);
                    int ret_pidvid = mUVCCamera.setPidVidValue(nPid, nVid);
                    Log.i(TAG, "UVCCamera setPidVidValue result:" + ret_pidvid);
                }
                //setSerialNumberValue
                String asciString_serialNum = mEditText_Serialnum.getText().toString();
                Log.i(TAG, "UVCCamera asciString_serialNum :" + asciString_serialNum);
                if (!TextUtils.isEmpty(asciString_serialNum)) {
                    int ret_serialNum = mUVCCamera.setSerialNumberValue(asciString_serialNum);
                    Log.i(TAG, "UVCCamera setSerialNumberValue result:" + ret_serialNum);
                }
            } else {
                Log.i(TAG, "uvc camera do not connect");
                Toast.makeText(mContext, "uvc camera do not connect", Toast.LENGTH_SHORT).show();
            }
        }

    }

}