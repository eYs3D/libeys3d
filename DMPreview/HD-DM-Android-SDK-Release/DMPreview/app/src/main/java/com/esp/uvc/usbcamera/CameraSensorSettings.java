package com.esp.uvc.usbcamera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import com.esp.android.usb.camera.core.EtronCamera;
import com.esp.android.usb.camera.core.USBMonitor;
import com.esp.android.usb.camera.core.USBMonitor.OnDeviceConnectListener;
import com.esp.android.usb.camera.core.USBMonitor.UsbControlBlock;
import com.esp.uvc.R;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CameraSensorSettings extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = "CameraSensorSettings";

    // for thread pool
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 10;
    private static ThreadPoolExecutor EXECUTER
        = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    //Handler msg
    public final static int MSG_REFRESH = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         getFragmentManager().beginTransaction()
             .replace(android.R.id.content, new SettingsFragment())
             .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements
        OnClickListener {

        //RadioButton ID
        private static final int ID_FW = 0;
        private static final int ID_ASIC = 1;
        private static final int ID_I2C = 2;

        // for accessing USB and USB camera
        private USBMonitor mUSBMonitor = null;
        private EtronCamera mUVCCamera = null;
        private UsbDevice mUsbDevice = null;
        private Context mContext = null;
        private View mRootView;
        private CheckBox mCheckBox_AE;
        private CheckBox mCheckBox_AWB;
        private int mCurrent = ID_FW;
        private boolean mAEStatus = false;
        private boolean mAWBStatus = false;
        private boolean mCameraSensorChange = false;

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
            mRootView = inflater.inflate(R.layout.camera_sensor_settings, null);
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
            mCheckBox_AE = (CheckBox) mRootView.findViewById(R.id.chkbox_ae);
            mCheckBox_AE.setEnabled(false);
            mCheckBox_AE.setChecked(false);
            mCheckBox_AE.setOnClickListener(this);
            mCheckBox_AWB = (CheckBox) mRootView.findViewById(R.id.chkbox_awb);
            mCheckBox_AWB.setEnabled(false);
            mCheckBox_AWB.setChecked(false);
            mCheckBox_AWB.setOnClickListener(this);
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
                            Log.e(TAG, "open uvccamera ret:" + ret);
                        } catch (Exception e) {
                            Log.e(TAG, "open uvccamera exception:" + e.toString());
                            return;
                        }
                        if (ret == 0 && mUVCCamera == null) {
                            mUVCCamera = camera;
                            mHandler.sendEmptyMessage(MSG_REFRESH);
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

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH:
                        if (mUVCCamera != null) {
                            mAEStatus = mUVCCamera.getAEStatusEnabled();
                            mCheckBox_AE.setEnabled(true);
                            mCheckBox_AE.setChecked(mAEStatus);
                            mAWBStatus = mUVCCamera.getAutoWhiteBalance() == 1;
                            mCheckBox_AWB.setEnabled(true);
                            mCheckBox_AWB.setChecked(mAWBStatus);
                        } else {
                            Log.i(TAG, "uvc camera do not connect");
                            Toast.makeText(mContext, "uvc camera do not connect", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                super.handleMessage(msg);
           }
        };

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.chkbox_ae:
                    Log.i(TAG, "onClick chkbox_ae");
                    if (mCheckBox_AE.isChecked()) {
                        mAEStatus = true;
                    } else {
                        mAEStatus = false;
                    }
                    writeData();
                    break;

                case R.id.chkbox_awb:
                    Log.i(TAG, "onClick chkbox_awb");
                    if (mCheckBox_AWB.isChecked()) {
                        mAWBStatus = true;
                    } else {
                        mAWBStatus = false;
                    }
                    writeData();
                    break;
            }
        }

        public void writeData() {

            AppSettings appSettings = AppSettings.getInstance(mContext);
            mCameraSensorChange = true;
            appSettings.put(AppSettings.AWB_STATUS, mAWBStatus);
//            appSettings.put(AppSettings.EXPOSURE_AUTO_ENABLE, mAEStatus);
            appSettings.put(AppSettings.CAMERA_SENSOR_CHANGE, mCameraSensorChange);
            appSettings.saveAll();
        }
    }

}