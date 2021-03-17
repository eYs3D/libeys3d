package com.esp.uvc.old.usbcamera;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.esp.android.usb.camera.core.EtronCamera;
import com.esp.android.usb.camera.core.RectifyLogData;
import com.esp.android.usb.camera.core.USBMonitor;
import com.esp.android.usb.camera.core.USBMonitor.OnDeviceConnectListener;
import com.esp.android.usb.camera.core.USBMonitor.UsbControlBlock;
import com.esp.android.usb.camera.core.UVCCamera;
import com.esp.uvc.R;
import com.esp.uvc.old.utility.MagicFileChooser;
import com.esp.uvc.old.widget.TextDialogPreference;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kotlin.text.CharCategory;

public class FirmwareTableSettings extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private Spinner mSpinnerIndex ;
    private TextView mTextViewGetFilePath;

    private static final String TAG = "FirmwareTableSettings";
    private static final String KEY_SENSOR_OFFSET_READ = "sensor_offset_read";
    private static final String KEY_SENSOR_OFFSET_WRITE = "sensor_offset_write";
    private static final String KEY_RECTIFY_TABLE_READ = "rectify_table_read";
    private static final String KEY_RECTIFY_TABLE_WRITE = "rectify_table_write";
    private static final String KEY_ZD_TABLE_READ = "zd_table_read";
    private static final String KEY_ZD_TABLE_WRTIE = "zd_table_write";
    private static final String KEY_RECTIFY_LOG_READ = "rectify_log_read";
    private static final String KEY_RECTIFY_LOG_WRITE = "rectify_log_write";
    private static final String KEY_GET_FILE_PATH = "get_file_path";

    //File id
    private static final int ETronDI_Y_OFFSET_FILE_ID_0		=		30;
    private static final int ETronDI_RECTIFY_FILE_ID_0		=		40;
    private static final int ETronDI_ZD_TABLE_FILE_ID_0		=		50;
    private static final int ETronDI_CALIB_LOG_FILE_ID_0	=		240;
    // for thread pool
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 10;
    private static final int SensorOffsetRead = 100;
    private static final int SensorOffsetWrite = 101;
    private static final int RectifyTableRead = 102;
    private static final int RectifyTableWrite = 103;
    private static final int ZdTableRead = 104;
    private static final int ZdTableWrite = 105;
    private static final int LogRead = 106;
    private static final int LogWrite = 107;
    public static int mIndex=0;
    public static File mFile;
    private static SettingsFragment mSettingsFragment;
    private MagicFileChooser mMagicFileChooser;
    private static ThreadPoolExecutor EXECUTER
        = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());


    @Override
    public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
        setContentView(R.layout.firmware_table_layout);
        mMagicFileChooser = new MagicFileChooser(this);


        initUI();

        mSettingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
             .replace(R.id.preference_holder, mSettingsFragment)
             .commit();



    }
    private void initUI(){
        mSpinnerIndex =(Spinner) findViewById(R.id.spinner_table_index);
        mTextViewGetFilePath= (TextView) findViewById(R.id.textview_table_file_path);
        Integer[] items = new Integer[]{0,1,2,3,4,5,6};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, items);
        mSpinnerIndex.setAdapter(adapter);
        mSpinnerIndex.setOnItemSelectedListener(mOnItemSelected);

        mTextViewGetFilePath.setOnClickListener(mOnClickListener);

    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG,"onActivityResult(requestCode = " + requestCode + ", resultCode = " + resultCode +")");

        if(mMagicFileChooser.onActivityResult(requestCode, resultCode, data)) {
            File[] files = mMagicFileChooser.getChosenFiles();
            mFile = files[0];
            mTextViewGetFilePath.setText("File path: \n" + mFile.getAbsolutePath());
        } else {
            Toast.makeText(this, "No choose file", Toast.LENGTH_SHORT).show();
        }

    }
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG,"mOnClickListener click");
            mMagicFileChooser.showFileChooser("*/*", "Choose file", false, true);
        }
    };

    private AdapterView.OnItemSelectedListener mOnItemSelected = new AdapterView.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
        {
            // TODO Auto-generated method stub
            Log.i(TAG,"onItemSelected:"+position);
            mIndex = position;
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0)
        {
            // TODO Auto-generated method stub
        }
    };
    public static class SettingsFragment extends PreferenceFragment {

        // for accessing USB and USB camera
        private USBMonitor mUSBMonitor = null;
        private EtronCamera mUVCCamera = null;
        private UsbDevice mUsbDevice = null;

        private Preference mPreference_GetFilePath;
        private TextDialogPreference mPreference_SensorOffsetRead;
        private Preference mPreference_SensorOffsetWrite;
        private TextDialogPreference mPreference_RectifyTableRead;
        private Preference mPreference_RectifyTableWrite;
        private TextDialogPreference mPreference_ZdTableRead;
        private Preference mPreference_ZdTableWrite;
        private TextDialogPreference mPreference_RectifyLogRead;
        private Preference mPreference_RectifyLogWrite;
        private Context mContext;


        public String mDirPath = Environment.getExternalStorageDirectory() + "/" + "uvccamera/" ;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.firmware_table_settings);
            mContext = getActivity();
            mUSBMonitor = new USBMonitor(mContext, mOnDeviceConnectListener);

            if (mUSBMonitor == null) {
                Log.d(TAG, "Error!! can not get USBMonitor " );
                return;
            }
            initUI();
            String[] permissions = {
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
            };
            int requestCode = 200;


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.requestPermissions(permissions, requestCode);
            }
            //builder for
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
                            Log.e(TAG, "open uvccam era exception:" + e.toString());
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

        private void initUI() {
            mPreference_GetFilePath = (Preference) findPreference(KEY_GET_FILE_PATH);
            mPreference_SensorOffsetRead = (TextDialogPreference) findPreference(KEY_SENSOR_OFFSET_READ);
            mPreference_SensorOffsetWrite = (Preference) findPreference(KEY_SENSOR_OFFSET_WRITE);
            mPreference_RectifyTableRead = (TextDialogPreference) findPreference(KEY_RECTIFY_TABLE_READ);
            mPreference_RectifyTableWrite = (Preference) findPreference(KEY_RECTIFY_TABLE_WRITE);
            mPreference_ZdTableRead = (TextDialogPreference) findPreference(KEY_ZD_TABLE_READ);
            mPreference_ZdTableWrite = (Preference) findPreference(KEY_ZD_TABLE_WRTIE);
            mPreference_RectifyLogRead = (TextDialogPreference) findPreference(KEY_RECTIFY_LOG_READ);
            mPreference_RectifyLogWrite = (Preference) findPreference(KEY_RECTIFY_LOG_WRITE);

        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

            if (mUVCCamera == null) {
                Log.i(TAG, "uvc camera do not connect");
                Toast.makeText(mContext, "uvc camera do not connect", Toast.LENGTH_SHORT).show();
            }
            else {
                StringBuilder sb = new StringBuilder();
                String serialNumber = mUVCCamera.getSerialNumberValue();
                Character last = serialNumber.charAt(serialNumber.length() - 1);
                if (CharCategory.CONTROL.contains(last)) {
                    serialNumber = serialNumber.substring(0, serialNumber.length() - 1); //Special characters at the end of sn string
                }
                if (preference == mPreference_SensorOffsetRead) {
                    byte[] buffer = mUVCCamera.getYOffsetValue();
                    if (buffer == null) {
                        Toast.makeText(mContext, "failed to read sensor", Toast.LENGTH_LONG).show();
                    }
                    else{
                        for (int i = 0; i < buffer.length; i++) {
                            sb.append(String.format("%02X ", buffer[i]));
                        }
                        mPreference_SensorOffsetRead.setDialogMessage(sb.toString());
                        mPreference_SensorOffsetRead.showDialog();
                        writeToFile(mDirPath ,serialNumber+KEY_SENSOR_OFFSET_READ,buffer);
                        Toast.makeText(mContext, "Write path:"+mDirPath + KEY_SENSOR_OFFSET_READ, Toast.LENGTH_LONG).show();
                    }
                }
                else if (preference == mPreference_SensorOffsetWrite) {
                    if(mFile!=null && mFile.exists()){
                        byte[] buffer = readFromFile(mFile);
                        if(mUVCCamera.setYOffsetValue(buffer,0)!=0){
                            Toast.makeText(mContext, "Write failed", Toast.LENGTH_LONG).show();
                        }
                        else{
                            Toast.makeText(mContext, "Write complete", Toast.LENGTH_LONG).show();
                        }
                    }
                    else{
                        Toast.makeText(mContext, "No file has been select", Toast.LENGTH_LONG).show();
                    }
                }
                else if (preference == mPreference_RectifyTableRead) {
                    int numberOfDepthStream = mUVCCamera.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_DEPTH).length;
                    StringBuilder rectifyTableString = new StringBuilder();
                    for(int index=0; index < numberOfDepthStream;index++){
                        byte[] buffer = mUVCCamera.getRectifyTableValue(index);
                        if(buffer == null){
                            if(index==0)Toast.makeText(mContext, "failed to read rectify table", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        else{
                            Log.i(TAG, "RectifyTableRead "+index+":" + rectifyTableString.toString());
                            rectifyTableString.append("---- RectifyTableRead "+index+" ----\n");
                            for (int i = 0; i < buffer.length; i++) {
                                rectifyTableString.append(String.format("%02X ", buffer[i]));
                            }
                            rectifyTableString.append("--------\n");
                            writeToFile(mDirPath , serialNumber+KEY_RECTIFY_TABLE_READ+index,buffer);
                            Toast.makeText(mContext, "Write path:"+mDirPath + KEY_RECTIFY_TABLE_READ+index, Toast.LENGTH_LONG).show();
                        }
                    }

                    mPreference_RectifyTableRead.setDialogMessage(rectifyTableString.toString());
                    mPreference_RectifyTableRead.showDialog();

                }
                else if (preference == mPreference_RectifyTableWrite) {
                    if(mFile!=null && mFile.exists()) {
                        byte[] buffer = readFromFile(mFile);
                        if (mUVCCamera.setRectifyTableValue(buffer , mIndex) != 0) {
                            Toast.makeText(mContext, "Write failed index:"+mIndex, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(mContext, "Write complete index:"+mIndex, Toast.LENGTH_LONG).show();
                        }
                    }
                    else{
                        Toast.makeText(mContext, "No file has been select", Toast.LENGTH_LONG).show();
                    }
                }

                else if (preference == mPreference_ZdTableRead) {
                    int numberOfDepthStream = mUVCCamera.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_DEPTH).length;
                    StringBuilder ZDTableString = new StringBuilder();

                    for(int index=0; index < numberOfDepthStream;index++){
                        int[] zdTable = mUVCCamera.getZDTableValue(index);
                        byte[] buffer = mUVCCamera.getFileData(ETronDI_ZD_TABLE_FILE_ID_0+index);
                        if(buffer == null || zdTable == null){
                            if(index==0)Toast.makeText(mContext, "failed to read zdTable", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        else{

                            Log.i(TAG, "ZDTable "+index+":" + ZDTableString.toString());
                            ZDTableString.append("---- ZDTable "+index+" -----\n");
                            for (int i = 0; i < zdTable.length; i++) {
                                ZDTableString.append(String.format("zdTable[%d]:%d \n",i, zdTable[i]));
                            }
                            ZDTableString.append("---------\n");
                            writeToFile(mDirPath , serialNumber+KEY_ZD_TABLE_READ+index,buffer);
                            Toast.makeText(mContext, "Write path:"+mDirPath + KEY_ZD_TABLE_READ+index, Toast.LENGTH_LONG).show();
                        }
                    }
                    mPreference_ZdTableRead.setDialogMessage(ZDTableString.toString());
                    mPreference_ZdTableRead.showDialog();

                }
                else if (preference == mPreference_ZdTableWrite) {
                    if(mFile!=null && mFile.exists()) {
                        byte[] buffer = readFromFile(mFile);
                        if (mUVCCamera.setZDTableValue(buffer, mIndex, 0) != 0) {
                            Toast.makeText(mContext, "Write failed index:"+mIndex, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(mContext, "Write complete index:"+mIndex, Toast.LENGTH_LONG).show();
                        }
                    }
                    else{
                        Toast.makeText(mContext, "No file has been select", Toast.LENGTH_LONG).show();
                    }
                }
                else if (preference == mPreference_RectifyLogRead) {
                    int numberOfDepthStream = mUVCCamera.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_DEPTH).length;
                    StringBuilder rectifyLogDataString = new StringBuilder();
                    for(int i=0; i < numberOfDepthStream;i++){
                        int index = i;
                        RectifyLogData rectifyLogData = mUVCCamera.getRectifyLogData(index);
                        byte[] buffer = mUVCCamera.getFileData(ETronDI_CALIB_LOG_FILE_ID_0 + index);
                        if(rectifyLogData == null){
                            if(i==0)Toast.makeText(mContext, "failed to read rectify log", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        else{
                            Log.i(TAG, "rectifyLogData "+index+":" + rectifyLogData.toString());
                            rectifyLogDataString.append("---- RectifyLogData "+index+" ----\n");
                            rectifyLogDataString.append(rectifyLogData.toString());
                            rectifyLogDataString.append("---------\n");
                            writeToFile(mDirPath , serialNumber+KEY_RECTIFY_LOG_READ+index,buffer);
                            Toast.makeText(mContext, "Write path:"+mDirPath + KEY_RECTIFY_LOG_READ+index, Toast.LENGTH_LONG).show();
                        }
                    }
                    mPreference_RectifyLogRead.setDialogMessage(rectifyLogDataString.toString());
                    mPreference_RectifyLogRead.showDialog();

                } else if (preference == mPreference_RectifyLogWrite) {
                    if(mFile!=null && mFile.exists()) {
                        byte[] buffer = readFromFile(mFile);
                        if (mUVCCamera.setFileData(buffer, ETronDI_CALIB_LOG_FILE_ID_0+mIndex) != 0) {
                            Toast.makeText(mContext, "Write failed index:"+mIndex, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(mContext, "Write complete index:"+mIndex, Toast.LENGTH_LONG).show();
                        }
                    }
                    else{
                        Toast.makeText(mContext, "No file has been select", Toast.LENGTH_LONG).show();
                    }
                }

            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }


    }
    static byte[] readFromFile(File file){
        if(file==null|| !file.exists()){
            return null;
        }
        int size = (int) file.length();
        byte[] buffer = new byte[size];

        BufferedInputStream bis;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(buffer);
            bis.close();
            Log.i(TAG, "Read from file...");
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
    static void writeToFile(String dirPath,String fileName,byte[] buffer){
        File dir = new File(dirPath);
        if( !dir.exists() ) {
            dir.mkdirs();
        }
        else if( !dir.isDirectory() && dir.canWrite() ){
            dir.delete();
            dir.mkdirs();
        }
        File file = new File(dirPath+fileName);

        BufferedOutputStream bos;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(buffer);
            bos.flush();
            bos.close();
            Log.i(TAG, "Read Complete...");
        } catch (IOException e) {
            Log.e(TAG, "write read buffer to file error:" + e.toString());e.printStackTrace();
        }

    }
}