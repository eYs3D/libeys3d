package com.esp.uvc.usbcamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.esp.android.usb.camera.core.EtronCamera;
import com.esp.android.usb.camera.core.USBMonitor;
import com.esp.uvc.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FirmwareUpdateSettings extends AppCompatActivity {

    private static final String TAG = "FirmwareUpdateSettings";
    private static final boolean DEBUG = true;

    // for thread pool
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 10;

    private static ThreadPoolExecutor EXECUTER
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };



    public static class SettingsFragment extends PreferenceFragment implements
            View.OnClickListener {

        private static Activity mActivity;
        // for accessing USB and USB camera
        private USBMonitor mUSBMonitor = null;
        private EtronCamera mUVCCamera = null;
        private UsbDevice mUsbDevice = null;
        private Context mContext = null;
        private View mRootView;
        private Button mButton_Read;
        private Button mButton_Write;
        private Button mButton_WriteFromBackup;
        private CheckBox mChkBoxSN;
        private CheckBox mChkBoxSP;
        private CheckBox mChkBoxREC;
        private CheckBox mChkBoxZD;
        private CheckBox mChkBoxCL;
        private CheckBox mChkBoxLT;
        private CheckBox mChkBoxISP;
        private CheckBox mChkBoxFWTag;
        private ProgressBar mProgressBarLoading;
        private String mToastMessage;
        private boolean isProcessing = false;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mContext = getActivity();
            mUSBMonitor = new USBMonitor(mContext, mOnDeviceConnectListener);
            if (mUSBMonitor == null) {
                Log.d(TAG, "Error!! can not get USBMonitor " );
                return;
            }
            String[] permissions = {
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
            };
            int requestCode = 200;

            mActivity = this.getActivity();
            mActivity.requestPermissions(permissions, requestCode);

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mRootView = inflater.inflate(R.layout.firmware_update_settings, null);
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

            mButton_Read  = (Button) mRootView.findViewById(R.id.read_button);
            mButton_Read.setOnClickListener(this);

            mButton_Write = (Button) mRootView.findViewById(R.id.write_button);
            mButton_Write.setOnClickListener(this);

            mButton_WriteFromBackup = (Button) mRootView.findViewById(R.id.write_from_backup_button);
            mButton_WriteFromBackup.setOnClickListener(this);
            mButton_WriteFromBackup.setVisibility(View.INVISIBLE);
            mChkBoxSN  = (CheckBox) mRootView.findViewById(R.id.kdc_checkbox_1);
            mChkBoxSP  = (CheckBox) mRootView.findViewById(R.id.kdc_checkbox_2);
            mChkBoxREC = (CheckBox) mRootView.findViewById(R.id.kdc_checkbox_3);
            mChkBoxZD  = (CheckBox) mRootView.findViewById(R.id.kdc_checkbox_4);
            mChkBoxCL  = (CheckBox) mRootView.findViewById(R.id.kdc_checkbox_5);
            mChkBoxLT  = (CheckBox) mRootView.findViewById(R.id.kdc_checkbox_6);
            mChkBoxISP = (CheckBox) mRootView.findViewById(R.id.checkbox_isp );
            mChkBoxFWTag= (CheckBox) mRootView.findViewById(R.id.checkbox_set_fw_tag);
            mProgressBarLoading = (ProgressBar)mRootView.findViewById(R.id.loadingPanel);
            mProgressBarLoading.setVisibility(View.INVISIBLE);

            mChkBoxLT.setVisibility(View.GONE);
            mChkBoxISP.setVisibility(View.GONE);
            mChkBoxFWTag.setVisibility(View.GONE);
        }

        private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
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
            public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
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
                            Log.d(TAG, "open uvccamera ret:" + ret);
                            if (ret == EtronCamera.EYS_OK) {
                                mUVCCamera = camera;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "open uvccamera exception:" + e.toString());
                            return;
                        }
                    }
                });
            }

            @Override
            public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
                if (DEBUG) Log.v(TAG, "onDisconnect");
                if (mUVCCamera != null && device.equals(mUVCCamera.getDevice())) {
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                    mUVCCamera = null;
                }
            }

            @Override
            public void onDetach(final UsbDevice device) {
                Toast.makeText(mContext, R.string.usb_device_detached, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancel() {
                if (DEBUG) Log.v(TAG, "onCancel:");
            }
        };

        public void onClick(View v) {
            if(isProcessing == true){
                return;
            }
            if (mUVCCamera != null) {
                mProgressBarLoading.setVisibility(View.VISIBLE);
                Runnable runnable = new Runnable() {public void run() {}};
                switch (v.getId()) {
                    case R.id.read_button:
                        Toast.makeText(mActivity, "Reading...", Toast.LENGTH_SHORT).show();
                        runnable = mRunnableRead;
                        break;

                    case R.id.write_button:
                        Toast.makeText(mActivity, "Writing...", Toast.LENGTH_SHORT).show();
                        runnable = mRunnableWrite;
                        break;

                    case R.id.write_from_backup_button:
                        doWriteFromBackup();
                        break;
                }
                EXECUTER.execute(runnable);
                //mProgressBarLoading.setVisibility(View.INVISIBLE);
                Log.i(TAG, "etVisibility(View.VISI");

            } else {
                Log.i(TAG, "uvc camera do not connect");
                Toast.makeText(mContext, "uvc camera do not connect", Toast.LENGTH_LONG).show();
            }

        }

        private boolean doRead() {
            Log.i(TAG, "doRead...");
            final File sdcard_dir = new File(Environment.getExternalStorageDirectory(), "eYs3D");
            File file = new File(sdcard_dir.getPath(), "fw.bin");
            Log.i(TAG, "file path"+ file.getPath());
            byte[] buffer = mUVCCamera.readFlashData();
            if(buffer ==null){
                return false;
            }
            BufferedOutputStream bos;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(file));
                bos.write(buffer);
                bos.flush();
                bos.close();
                Log.i(TAG, "Read Complete...:"+file.getAbsolutePath());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

        }

        private int doWrite() {
            Log.i(TAG, "doWrite...");

            final File sdcard_dir = new File(Environment.getExternalStorageDirectory(), "eYs3D");
            File file = new File(sdcard_dir.getPath(), "fw.bin");
            Log.i(TAG, "file path"+ file.getPath());
            byte[] buffer = new byte[1024*104];

            BufferedInputStream bis;
            try {
                bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(buffer);
                bis.close();
                Log.i(TAG, "Read from file...");
            } catch (IOException e) {
                e.printStackTrace();
            }

            boolean bIsSerialNumberKeep       = false;
            boolean bIsSensorPositionKeep     = false;
            boolean bIsRectificationTableKeep = false;
            boolean bIsZDTableKeep            = false;
            boolean bIsCalibrationLogKeep     = false;
            boolean bIsParaLutKeep            = false;
            boolean bIsISPKeep                = false;
            boolean bSetFWTag                 = false;

            if(mChkBoxSN.isChecked())  bIsSerialNumberKeep       = true;
            if(mChkBoxSP.isChecked())  bIsSensorPositionKeep     = true;
            if(mChkBoxREC.isChecked()) bIsRectificationTableKeep = true;
            if(mChkBoxZD.isChecked())  bIsZDTableKeep            = true;
            if(mChkBoxCL.isChecked())  bIsCalibrationLogKeep     = true;
            if(mChkBoxLT.isChecked())  bIsParaLutKeep            = true;
            if(mChkBoxISP.isChecked()) bIsISPKeep                = true;
            if(mChkBoxFWTag.isChecked()) bSetFWTag               = true;

            return mUVCCamera.writeFlashData( buffer,
                                                    bIsSerialNumberKeep,
                                                    bIsSensorPositionKeep,
                                                    bIsRectificationTableKeep,
                                                    bIsZDTableKeep,
                                                    bIsCalibrationLogKeep,
                                                    bIsParaLutKeep,
                                                    bIsISPKeep,
                                                    bSetFWTag);


        }

        private void doWriteFromBackup() {
            Log.i(TAG, "doWriteFromBackup...");
            if (mUVCCamera != null) {
                final File sdcard_dir = new File(Environment.getExternalStorageDirectory(), "eYs3D");
                //Load bin file
                File file = new File(sdcard_dir.getPath(), "fw.bin");
                Log.i(TAG, "file path"+ file.getPath());
                byte[] buffer = new byte[1024*104];

                BufferedInputStream bis;
                try {
                    bis = new BufferedInputStream(new FileInputStream(file));
                    bis.read(buffer);
                    bis.close();
                    Log.i(TAG, "Read from file...");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Load backup file
                File backupFile = new File(sdcard_dir.getPath(), "backup.bin");
                Log.i(TAG, "backupFile path"+ backupFile.getPath());
                byte[] bufferBackupFile = new byte[1024*104];

                try {
                    bis = new BufferedInputStream(new FileInputStream(backupFile));
                    bis.read(bufferBackupFile);
                    bis.close();
                    Log.i(TAG, "Read from backup file...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int result = mUVCCamera.writeFlashDataASIC( buffer,bufferBackupFile);

                if(result == 0) {
                    Log.i(TAG, "Write to flash complete...");
                    Toast.makeText(mActivity, "Write to flash complete...", Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, "Write to flash failed...:"+result);
                    Toast.makeText(mActivity, "Write to flash failed...", Toast.LENGTH_LONG).show();
                }

            } else {
                Log.i(TAG, "uvc camera do not connect");
                Toast.makeText(mContext, "uvc camera do not connect", Toast.LENGTH_LONG).show();
            }
        }

        private Runnable mRunnableRead = new Runnable(){
            public void run() {
                isProcessing = true;
                if(doRead() ==true){
                    String path= Environment.getExternalStorageDirectory().getPath()+ "/eYs3D/fw.bin";
                    toastMessage("Read complete :"+path);
                }
                else{
                    toastMessage("Read failed...");

                }
                mActivity.runOnUiThread(new Runnable(){
                    public void run() {
                        mProgressBarLoading.setVisibility(View.INVISIBLE);
                    }
                });
                isProcessing = false;
            }
        };

        private void toastMessage(String messageString){
            mToastMessage =messageString;
            mActivity.runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    Toast.makeText(mActivity, mToastMessage, Toast.LENGTH_LONG).show();
                }
            });
        }

        private Runnable mRunnableWrite = new Runnable(){
            public void run() {
                isProcessing = true;
                int result = doWrite();
                if(result == 0) {
                    Log.i(TAG, "Write to flash complete...");
                    String path= Environment.getExternalStorageDirectory().getPath()+ "/eYs3D/fw.bin";
                    toastMessage("Write to flash complete...:"+path);
                } else if(result == EtronCamera.ETronDI_KEEP_DATA_FAIL_SERIALNUMBER) {
                    Log.d(TAG, "Write to flash failed...:"+result);
                    toastMessage("Write to flash failed...:ETronDI_KEEP_DATA_FAIL_SERIALNUMBER");
                }else if(result == EtronCamera.ETronDI_KEEP_DATA_FAIL_SENSORPOSITION) {
                    Log.d(TAG, "Write to flash failed...:"+result);
                    toastMessage("Write to flash failed...:ETronDI_KEEP_DATA_FAIL_SENSORPOSITION");
                }else if(result == EtronCamera.ETronDI_KEEP_DATA_FAIL_RECTIFYTABLE) {
                    Log.d(TAG, "Write to flash failed...:"+result);
                    toastMessage("Write to flash failed...:ETronDI_KEEP_DATA_FAIL_RECTIFYTABLE");
                }else if(result == EtronCamera.ETronDI_KEEP_DATA_FAIL_CALIBRATIONLOG) {
                    Log.d(TAG, "Write to flash failed...:"+result);
                    toastMessage("Write to flash failed...:ETronDI_KEEP_DATA_FAIL_CALIBRATIONLOG");
                }else if(result == EtronCamera.ETronDI_KEEP_DATA_FAIL_ZDTABLE) {
                    Log.d(TAG, "Write to flash failed...:"+result);
                    toastMessage("Write to flash failed...:ETronDI_KEEP_DATA_FAIL_ZDTABLE");
                }else if(result == EtronCamera.ETronDI_KEEP_DATA_FAIL_LUT) {
                    Log.d(TAG, "Write to flash failed...:"+result);
                    toastMessage("Write to flash failed...:ETronDI_KEEP_DATA_FAIL_LUT");
                }else if(result == EtronCamera.ETronDI_KEEP_DATA_FAIL_ISP) {
                    Log.d(TAG, "Write to flash failed...:"+result);
                    toastMessage("Write to flash failed...:ETronDI_KEEP_DATA_FAIL_ISP");
                }else if(result == EtronCamera.ETronDI_KEEP_DATA_FAIL_FWTAG) {
                    Log.d(TAG, "Write to flash failed...:"+result);
                    toastMessage("Write to flash failed...:ETronDI_KEEP_DATA_FAIL_FWTAG");
                }else {
                    Log.d(TAG, "Write to flash failed...:"+result);
                    toastMessage("Write to flash failed...:"+result);
                }
                mActivity.runOnUiThread(new Runnable(){
                    public void run() {
                        mProgressBarLoading.setVisibility(View.INVISIBLE);
                    }
                });
                isProcessing = false;
            }
        };
    }
}
