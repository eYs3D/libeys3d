package com.esp.uvc.old.usbcamera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.esp.android.usb.camera.core.EtronCamera;
import com.esp.android.usb.camera.core.Size;
import com.esp.android.usb.camera.core.StreamInfo;
import com.esp.android.usb.camera.core.USBMonitor;
import com.esp.android.usb.camera.core.UVCCamera;
import com.esp.uvc.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class PreviewImageSettings extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = "PreviewImageSettings";
    private static final String KEY_ENABLE_STREAM_COLOR_CHECKED = "enable_stream_color_checked";
    private static final String KEY_ENABLE_STREAM_DEPTH_CHECKED = "enable_stream_depth_checked";
    private static final String KEY_ETRON_PREVIEW_FRAME_SIZE = "etron_preview_frame_size";
    private static final String KEY_DEPTH_PREVIEW_FRAME_SIZE = "depth_preview_frame_size";
    private static final String KEY_COLOR_PREVIEW_FRAME_RATE = "color_preview_frame_rate";
    private static final String KEY_DEPTH_PREVIEW_FRAME_RATE = "depth_preview_frame_rate";
    private static final String KEY_REVERSE_VIEW_WINDOW_CHECKED = "reverse_view_window_checked";
    private static final String KEY_LANDSCAPE_VIEW_WINDOW_CHECKED = "landscape_view_window_checked";
    private static final String KEY_FLIP_VIEW_WINDOW_CHECKED = "flip_view_window_checked";
    private static final String KEY_POST_PROCESS_OPENCL = "post_process_opencl";
    private static final String KEY_MONITOR_FRAMERATE = "monitor_framerate";
    private static final String KEY_DEPTH_DATATYPE = "depth_data_type";
    private static final String KEY_Z_FAR = "z_far";

    @Override
    public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         getFragmentManager().beginTransaction()
             .replace(android.R.id.content, new SettingsFragment())
             .commit();

    }

    public static class SettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

        private final CharSequence mEntriesDepthDataType[] = {  "COLOR_ONLY",
                                                                "8_BITS",
                                                                "14_BITS",
                                                                "8_BITS_x80",
                                                                "11_BITS",
                                                                "OFF_RECTIFY",
                                                                 "8_BITS_RAW",
                                                                "14_BITS_RAW",
                                                                "8_BITS_x80_RAW",
                                                                "11_BITS_RAW",
                                                                };
        private CharSequence mEntriesColor[];
        private CharSequence mEntriesDepth[];
        List<Size> mSupportedSizeListColor;
        List<Size> mSupportedSizeListDepth;
        private static final int CORE_POOL_SIZE = 1;
        private static final int MAX_POOL_SIZE = 4;
        private static final int KEEP_ALIVE_TIME = 10;
        private static ThreadPoolExecutor EXECUTER
                = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        private USBMonitor mUSBMonitor = null;
        private EtronCamera mUVCCamera = null;
        private UsbDevice mUsbDevice = null;
        private String mSupportedSize;
        private boolean mEnableStreamColor = true;
        private boolean mEnableStreamDepth = false;
        private boolean mReverse = true;
        private boolean mLandscape = false;
        private boolean mFlip = true;
        private boolean mPostProcessOpencl = false;
        private boolean mMonitorFrameRate = false;

        private int mEtronIndex = 0;
        private int mDepthIndex = 0;
        private int mDepthDataType = 1;
        private int mColorFrameRate = 30;
        private int mDepthFrameRate = 30;
        private int mZFar = 1000;//mm

        private CheckBoxPreference mCheckBoxPreference_EnableStreamColorChecked;
        private CheckBoxPreference mCheckBoxPreference_EnableStreamDepthChecked;
        private CheckBoxPreference mCheckBoxPreference_ReverseViewWindowChecked;
        private CheckBoxPreference mCheckBoxPreference_LandscapeViewWindowChecked;
        private CheckBoxPreference mCheckBoxPreference_FlipViewWindowChecked;
        private CheckBoxPreference mCheckBoxPreference_PostProcessOpencl;
        private CheckBoxPreference mCheckBoxPreference_MonitorFrameRate;
        private ListPreference mListPreference_EtronPreviewFrameSize;
        private ListPreference mListPreference_DepthPreviewFrameSize;
        private ListPreference mListPreference_DepthDataType;
        private EditTextPreference mEditTextPreference_OnlyColorPreviewFrameRate;
        private EditTextPreference mEditTextPreference_ColorPreviewFrameRate;
        private EditTextPreference mEditTextPreference_DepthPreviewFrameRate;
        private EditTextPreference mEditTextPreference_ZFar;
        private Context mContext = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preview_image_settings);
            this.mContext = getActivity();
            mUSBMonitor = new USBMonitor(mContext, mOnDeviceConnectListener);
            if (mUSBMonitor == null) {
                Log.d(TAG, "Error!! can not get USBMonitor " );
                return;
            }
            initUI();
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

            readData();
            updateResolutionSetting();
            updateUI();
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

        private void updateResolutionSetting(){

            if(mUVCCamera!=null) {
                mEntriesColor = listToEntries(mUVCCamera.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_COLOR));
                mEntriesDepth = listToEntries(mUVCCamera.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_DEPTH));
            }
            else{
                mEntriesColor = getEntriesFromSetting(UVCCamera.INTERFACE_NUMBER_COLOR);
                mEntriesDepth = getEntriesFromSetting(UVCCamera.INTERFACE_NUMBER_DEPTH);
            }

            mListPreference_EtronPreviewFrameSize.setEntries(mEntriesColor);
            mListPreference_EtronPreviewFrameSize.setEntryValues(mEntriesColor);
            mListPreference_DepthPreviewFrameSize.setEntries(mEntriesDepth);
            mListPreference_DepthPreviewFrameSize.setEntryValues(mEntriesDepth);

            if (mEntriesColor.length == 0) {
                mEtronIndex = -1;
            } else if (mEtronIndex >= mEntriesColor.length ) {
                mEtronIndex = 0;
            }


            if (mEntriesDepth.length == 0) {
                mDepthIndex = -1;
            } else if (mDepthIndex >= mEntriesDepth.length) {
                mDepthIndex = 0;
            }

        }

        private void initUI() {
            mCheckBoxPreference_EnableStreamColorChecked
                    = (CheckBoxPreference) findPreference(KEY_ENABLE_STREAM_COLOR_CHECKED);
            mCheckBoxPreference_EnableStreamDepthChecked
                    = (CheckBoxPreference) findPreference(KEY_ENABLE_STREAM_DEPTH_CHECKED);
            mCheckBoxPreference_ReverseViewWindowChecked
                = (CheckBoxPreference) findPreference(KEY_REVERSE_VIEW_WINDOW_CHECKED);
            mCheckBoxPreference_LandscapeViewWindowChecked
                = (CheckBoxPreference) findPreference(KEY_LANDSCAPE_VIEW_WINDOW_CHECKED);
            mCheckBoxPreference_FlipViewWindowChecked
                = (CheckBoxPreference) findPreference(KEY_FLIP_VIEW_WINDOW_CHECKED);
            mCheckBoxPreference_PostProcessOpencl
                    = (CheckBoxPreference) findPreference(KEY_POST_PROCESS_OPENCL);
            mCheckBoxPreference_MonitorFrameRate
                    = (CheckBoxPreference) findPreference(KEY_MONITOR_FRAMERATE);
            mListPreference_EtronPreviewFrameSize        = (ListPreference) findPreference(KEY_ETRON_PREVIEW_FRAME_SIZE);
            mListPreference_EtronPreviewFrameSize.setOnPreferenceChangeListener(SettingsFragment.this);
            mListPreference_DepthPreviewFrameSize        = (ListPreference) findPreference(KEY_DEPTH_PREVIEW_FRAME_SIZE);
            mListPreference_DepthPreviewFrameSize.setOnPreferenceChangeListener(SettingsFragment.this);
            mListPreference_DepthDataType                = (ListPreference) findPreference(KEY_DEPTH_DATATYPE);
            mListPreference_DepthDataType.setOnPreferenceChangeListener(SettingsFragment.this);

            mEditTextPreference_ColorPreviewFrameRate = (EditTextPreference) findPreference(KEY_COLOR_PREVIEW_FRAME_RATE);
            mEditTextPreference_ColorPreviewFrameRate.setOnPreferenceChangeListener(SettingsFragment.this);

            mEditTextPreference_DepthPreviewFrameRate = (EditTextPreference) findPreference(KEY_DEPTH_PREVIEW_FRAME_RATE);
            mEditTextPreference_DepthPreviewFrameRate.setOnPreferenceChangeListener(SettingsFragment.this);

            mEditTextPreference_ZFar = (EditTextPreference) findPreference(KEY_Z_FAR);
            mEditTextPreference_ZFar.setOnPreferenceChangeListener(SettingsFragment.this);
            buildUI();
        }

        public void buildUI() {
            mListPreference_EtronPreviewFrameSize.setEntries(mEntriesColor);
            mListPreference_EtronPreviewFrameSize.setEntryValues(mEntriesColor);
            mListPreference_DepthPreviewFrameSize.setEntries(mEntriesDepth);
            mListPreference_DepthPreviewFrameSize.setEntryValues(mEntriesDepth);
            mListPreference_DepthDataType.setEntries(mEntriesDepthDataType);
            mListPreference_DepthDataType.setEntryValues(mEntriesDepthDataType);
        }

        public void updateUI() {
            if(mEtronIndex >-1){
                mListPreference_EtronPreviewFrameSize.setValueIndex(mEtronIndex);
                mListPreference_EtronPreviewFrameSize.setSummary(mEntriesColor[mEtronIndex]);
            }
            if(mDepthIndex >-1){
                mListPreference_DepthPreviewFrameSize.setValueIndex(mDepthIndex);
                mListPreference_DepthPreviewFrameSize.setSummary(mEntriesDepth[mDepthIndex]);
            }
            mListPreference_DepthDataType.setValueIndex(mDepthDataType);
            if(mDepthDataType == EtronCamera.VideoMode.COLOR_ONLY || mDepthDataType == EtronCamera.VideoMode.OFF_RECTIFY) {
                mEnableStreamDepth = false;
                mCheckBoxPreference_EnableStreamDepthChecked.setEnabled(false);
                mEditTextPreference_DepthPreviewFrameRate.setEnabled(false);
            } else {
                mCheckBoxPreference_EnableStreamDepthChecked.setEnabled(true);
                mEditTextPreference_DepthPreviewFrameRate.setEnabled(true);
            }

            mListPreference_DepthDataType.setSummary(mEntriesDepthDataType[mDepthDataType]);
            mEditTextPreference_ColorPreviewFrameRate.setSummary(String.valueOf(mColorFrameRate));
            mEditTextPreference_ColorPreviewFrameRate.setText(String.valueOf(mColorFrameRate));
            mEditTextPreference_DepthPreviewFrameRate.setSummary(String.valueOf(mDepthFrameRate));
            mEditTextPreference_DepthPreviewFrameRate.setText(String.valueOf(mDepthFrameRate));
            mEditTextPreference_ZFar.setSummary(String.valueOf(mZFar));

            mCheckBoxPreference_EnableStreamColorChecked.setChecked(mEnableStreamColor);
            mCheckBoxPreference_EnableStreamDepthChecked.setChecked(mEnableStreamDepth);
            mCheckBoxPreference_ReverseViewWindowChecked.setChecked(mReverse);
            mCheckBoxPreference_LandscapeViewWindowChecked.setChecked(mLandscape);
            mCheckBoxPreference_FlipViewWindowChecked.setChecked(mFlip);
            mCheckBoxPreference_PostProcessOpencl.setChecked(mPostProcessOpencl);
            mCheckBoxPreference_MonitorFrameRate.setChecked(mMonitorFrameRate);
        }

        private void readData() {

            AppSettings appSettings = AppSettings.getInstance(mContext);
            mEnableStreamColor = appSettings.get(AppSettings.ENABLE_STREAM_COLOR, mEnableStreamColor);
            mEnableStreamDepth = appSettings.get(AppSettings.ENABLE_STREAM_DEPTH, mEnableStreamDepth);
            mEtronIndex = appSettings.get(AppSettings.ETRON_INDEX, mEtronIndex);
            mDepthIndex = appSettings.get(AppSettings.DEPTH_INDEX, mDepthIndex);
            mDepthDataType = appSettings.get(AppSettings.DEPTH_DATA_TYPE, mDepthDataType);
            mColorFrameRate = appSettings.get(AppSettings.COLOR_FRAME_RATE, mColorFrameRate);
            mDepthFrameRate = appSettings.get(AppSettings.DEPTH_FRAME_RATE, mDepthFrameRate);
            mReverse = appSettings.get(AppSettings.REVERSE, mReverse);
            mLandscape = appSettings.get(AppSettings.LANDSCAPE, mLandscape);
            mFlip = appSettings.get(AppSettings.FLIP, mFlip);
            mSupportedSize = appSettings.get(AppSettings.SUPPORTED_SIZE,"");
            mPostProcessOpencl = appSettings.get(AppSettings.POST_PROCESS_OPENCL, mPostProcessOpencl);
            if(!EtronCamera.isSupportOpenCL()) {
                Log.e(TAG, "Not support OpenCL!");
                mPostProcessOpencl = false;
                mCheckBoxPreference_PostProcessOpencl.setChecked(false);
                mCheckBoxPreference_PostProcessOpencl.setEnabled(false);
                mCheckBoxPreference_PostProcessOpencl.setSummary("The device does not support OpenCL");
            }
            mMonitorFrameRate = appSettings.get(AppSettings.MONITOR_FRAMERATE, mMonitorFrameRate);
            mZFar = appSettings.get(AppSettings.Z_FAR, mZFar);
        }

        public void writeData() {

            AppSettings appSettings = AppSettings.getInstance(mContext);

            appSettings.put(AppSettings.ENABLE_STREAM_COLOR, mEnableStreamColor);
            appSettings.put(AppSettings.ENABLE_STREAM_DEPTH, mEnableStreamDepth);
            appSettings.put(AppSettings.REVERSE, mReverse);
            appSettings.put(AppSettings.LANDSCAPE, mLandscape);
            appSettings.put(AppSettings.FLIP, mFlip);

            appSettings.put(AppSettings.ETRON_INDEX, mEtronIndex);
            appSettings.put(AppSettings.DEPTH_INDEX, mDepthIndex);
            appSettings.put(AppSettings.DEPTH_DATA_TYPE, mDepthDataType);
            appSettings.put(AppSettings.COLOR_FRAME_RATE, mColorFrameRate);
            appSettings.put(AppSettings.DEPTH_FRAME_RATE, mDepthFrameRate);
            appSettings.put(AppSettings.Z_FAR, mZFar);

            appSettings.put(AppSettings.SUPPORTED_SIZE, mSupportedSize);

            appSettings.put(AppSettings.POST_PROCESS_OPENCL, mPostProcessOpencl);

            appSettings.put(AppSettings.MONITOR_FRAMERATE, mMonitorFrameRate);
            appSettings.saveAll();

        }

        public boolean onPreferenceChange(Preference preference, Object objValue) {
            final String key = preference.getKey();
            if (KEY_ETRON_PREVIEW_FRAME_SIZE.equals(key)) {
                mEtronIndex = mListPreference_EtronPreviewFrameSize.findIndexOfValue((String) objValue);
            } else if (KEY_DEPTH_PREVIEW_FRAME_SIZE.equals(key)) {
                mDepthIndex = mListPreference_DepthPreviewFrameSize.findIndexOfValue((String) objValue);
            } else if (KEY_DEPTH_DATATYPE.equals(key)) {
                mDepthDataType = mListPreference_DepthDataType.findIndexOfValue((String) objValue);
            } else if (KEY_COLOR_PREVIEW_FRAME_RATE.equals(key)) {
                mColorFrameRate =  Integer.valueOf((String) objValue);
            } else if (KEY_DEPTH_PREVIEW_FRAME_RATE.equals(key)) {
                mDepthFrameRate =  Integer.valueOf((String) objValue);
            } else if (KEY_Z_FAR.equals(key)) {
                mZFar =  Integer.valueOf((String) objValue);
            }
            updateUI();
            writeData();
            return true;
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference == mCheckBoxPreference_EnableStreamColorChecked) {
                mEnableStreamColor = mCheckBoxPreference_EnableStreamColorChecked.isChecked();
            } else if (preference == mCheckBoxPreference_EnableStreamDepthChecked) {
                mEnableStreamDepth = mCheckBoxPreference_EnableStreamDepthChecked.isChecked();
            } else if (preference == mCheckBoxPreference_ReverseViewWindowChecked) {
                mReverse = mCheckBoxPreference_ReverseViewWindowChecked.isChecked();
            } else if (preference == mCheckBoxPreference_LandscapeViewWindowChecked) {
                mLandscape = mCheckBoxPreference_LandscapeViewWindowChecked.isChecked();
            } else if (preference == mCheckBoxPreference_FlipViewWindowChecked) {
                mFlip = mCheckBoxPreference_FlipViewWindowChecked.isChecked();
            } else if (preference == mCheckBoxPreference_PostProcessOpencl) {
                mPostProcessOpencl = mCheckBoxPreference_PostProcessOpencl.isChecked();
            } else if (preference == mCheckBoxPreference_MonitorFrameRate) {
                mMonitorFrameRate = mCheckBoxPreference_MonitorFrameRate.isChecked();
            }
            updateUI();
            writeData();
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        private CharSequence[] getEntriesFromSetting(int interfaceNumber){
            List<Size> list =EtronCamera.getSupportedSizeList(mSupportedSize,interfaceNumber);
            CharSequence[] entries=new CharSequence[list.size()];
            for(int i=0;i<list.size();i++){
                String type = list.get(i).type==6 ? "MJPEG" : "YUV";
                entries[i]=String.format("%dx%d_%s",list.get(i).width,list.get(i).height ,type);
                Log.i(TAG,String.format("getEntriesFromSetting(): entries[%d]=%s",i,entries[i].toString()));
            }
            return entries;
        }

        private CharSequence[] listToEntries(List<Size> list){
            CharSequence[] entries=new CharSequence[list.size()];
            List<String> listString=new ArrayList<String>();
            for(int i=0;i<list.size();i++){
                String type="";
                if(list.get(i).type==6)type="MJPEG";
                if(list.get(i).type==4)type="YUV";
                listString.add(String.format("%dx%d",list.get(i).width,list.get(i).height ));
                entries[i]=String.format("%dx%d",list.get(i).width,list.get(i).height );
                //Log.i(TAG,String.format("entries[%d]=%s",i,entries[i].toString()));
            }
            return entries;
        }

        private CharSequence[] listToEntries(StreamInfo[] streamInfoList){
            CharSequence[] entries=new CharSequence[streamInfoList.length];
            for(int i=0;i<streamInfoList.length;i++){
                String type = streamInfoList[i].bIsFormatMJPEG ? "MJPEG" : "YUV";
                entries[i]=String.format("%dx%d_%s",streamInfoList[i].width,streamInfoList[i].height ,type);
                Log.i(TAG,String.format("entries[%d]=%s",i,entries[i].toString()));
            }
            return entries;
        }

        private String streamInfoToSuppoertedSize(StreamInfo[] streamInfoList){
            String supportedSize = "";
            for(int i=0;i<streamInfoList.length;i++){
                String type = streamInfoList[i].bIsFormatMJPEG ? "MJPEG" : "YUV";
                supportedSize += String.format("%dx%d_%s",streamInfoList[i].width,streamInfoList[i].height ,type);

            }
            Log.i(TAG,"streamInfoToSuppoertedSize:"+supportedSize);
            return supportedSize;
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
                if (DEBUG) Log.v(TAG, ">>>> onAttach UsbDevice:" + mUsbDevice );
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
                            Log.i(TAG, "open uvccamera ret:" + ret);
                        } catch (Exception e) {
                            Log.e(TAG, "open uvccamera exception:" + e.toString());
                            return;
                        }
                        if (ret == EtronCamera.EYS_OK && mUVCCamera == null) {
                            mUVCCamera = camera;
                            mSupportedSize = mUVCCamera.getSupportedSize();

                            updateResolutionSetting();
                            writeData();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                }
                            });

                        }
                        else{
                            Log.e(TAG, "open uvccamera ret:" + ret);
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
                Toast.makeText(mContext, R.string.usb_device_detached, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                if (DEBUG) Log.v(TAG, "onCancel:");
            }
        };
    }

}