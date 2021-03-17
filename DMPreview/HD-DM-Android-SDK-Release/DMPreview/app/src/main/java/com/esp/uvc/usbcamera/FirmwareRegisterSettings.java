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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
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


public class FirmwareRegisterSettings extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = "FirmwareRegister";

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
        private Button mButton_Get;
        private Button mButton_Set;
        private CheckBox mCheckBox_Address;
        private CheckBox mCheckBox_Value;
        private EditText mEditText_SlaveId;
        private EditText mEditText_Address;
        private EditText mEditText_Value;
        private RadioButton mRadioButton_FW;
        private RadioButton mRadioButton_ASIC;
        private RadioButton mRadioButton_I2C;
        private RadioGroup mRadioGroup_Group;
        private int mCurrent = ID_FW;

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
            mRootView = inflater.inflate(R.layout.firmware_register_settings, null);
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

            mCheckBox_Address = (CheckBox) mRootView.findViewById(R.id.chkbox_address);
            mCheckBox_Address.setEnabled(false);
            mCheckBox_Address.setChecked(false);
            mCheckBox_Value = (CheckBox) mRootView.findViewById(R.id.chkbox_vaule);
            mCheckBox_Value.setEnabled(false);
            mCheckBox_Value.setChecked(false);
            mEditText_SlaveId = (EditText) mRootView.findViewById(R.id.slave_id_edit);
            mEditText_SlaveId.setEnabled(false);
            mEditText_Address = (EditText) mRootView.findViewById(R.id.address_edit);
            mEditText_Value = (EditText) mRootView.findViewById(R.id.value_edit);
            mRadioButton_FW = (RadioButton) mRootView.findViewById(R.id.radiobtn_fw);
            mRadioButton_FW.setChecked(true);
            mRadioButton_ASIC = (RadioButton) mRootView.findViewById(R.id.radiobtn_asic);
            mRadioButton_I2C = (RadioButton) mRootView.findViewById(R.id.radiobtn_i2c);
            mRadioGroup_Group = (RadioGroup) mRootView.findViewById(R.id.rgroup);
            mRadioGroup_Group.setOnCheckedChangeListener(mOnCheckedChangeListener);


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
            hideSoftKeyboard(mEditText_Address);
            hideSoftKeyboard(mEditText_Value);
        }
        public void hideSoftKeyboard(View view) {
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        private void doRead() {
            Log.i(TAG, "doRead...");
            if (mUVCCamera != null) {
                String address_edit = mEditText_Address.getText().toString();
                if (TextUtils.isEmpty(address_edit)) {
                    Toast.makeText(mContext, R.string.firmware_register_hint, Toast.LENGTH_SHORT).show();
                    return;
                }
               switch (mCurrent) {
                   case ID_FW:
                       String[] hexStringFW = new String[1];
                       int address_fw = Integer.parseInt(address_edit, 16);
                       int ret_fw = mUVCCamera.getFWRegisterValue(hexStringFW, address_fw);
                       Log.i(TAG, "UVCCamera getFWRegisterValue result:" + ret_fw);
                       if (!TextUtils.isEmpty(hexStringFW[0])) {
                           mEditText_Value.setText(hexStringFW[0]);
                       }
                       break;
                   case ID_ASIC:
                       String[] hexStringASIC = new String[1];
                       int address_asic = Integer.parseInt(address_edit, 16);
                       int ret_asic = mUVCCamera.getHWRegisterValue(hexStringASIC, address_asic);
                       Log.i(TAG, "UVCCamera getHWRegisterValue result:" + ret_asic);
                       if (!TextUtils.isEmpty(hexStringASIC[0])) {
                           mEditText_Value.setText(hexStringASIC[0]);
                       }
                       break;
                   case ID_I2C:
                       String slaveId_edit = mEditText_SlaveId.getText().toString();
                       if (TextUtils.isEmpty(slaveId_edit)) {
                           Toast.makeText(mContext, R.string.firmware_register_hint, Toast.LENGTH_SHORT).show();
                           return;
                       }
                       int address_i2c = Integer.parseInt(address_edit, 16);
                       int slaveId = Integer.parseInt(slaveId_edit, 16);
                       int flag = 0;
                       if(mCheckBox_Address.isChecked()) {
                           flag |= EtronCamera.FG_Address_2Byte;
                       } else {
                           flag |= EtronCamera.FG_Address_1Byte;
                       }

                       if(mCheckBox_Value.isChecked()) {
                           flag |= EtronCamera.FG_Value_2Byte;
                       } else {
                           flag |= EtronCamera.FG_Value_1Byte;
                       }
                       String[] hexStringI2C = new String[1];
                       int ret_i2c = mUVCCamera.getSensorRegisterValue(hexStringI2C,
                           slaveId, address_i2c, flag);
                       Log.i(TAG, "UVCCamera getSensorRegisterValue result:" + ret_i2c);
                       if (!TextUtils.isEmpty(hexStringI2C[0])) {
                           mEditText_Value.setText(hexStringI2C[0]);
                       }
                       break;
               }
            } else {
                Log.i(TAG, "uvc camera do not connect");
                Toast.makeText(mContext, "uvc camera do not connect", Toast.LENGTH_SHORT).show();
            }
        }

        private void doWrite() {
            Log.i(TAG, "doWrite...");
            if (mUVCCamera != null) {
                String address_edit = mEditText_Address.getText().toString();
                String vaule_edit = mEditText_Value.getText().toString();
                if (TextUtils.isEmpty(address_edit) || TextUtils.isEmpty(vaule_edit)) {
                    Toast.makeText(mContext, R.string.firmware_register_hint, Toast.LENGTH_SHORT).show();
                    return;
                }
               switch (mCurrent) {
                   case ID_FW:
                       int address_fw = Integer.parseInt(address_edit, 16);
                       int vaule_fw = Integer.parseInt(vaule_edit, 16);
                       int ret_fw = mUVCCamera.SetFWRegisterValue(address_fw, vaule_fw);
                       Log.i(TAG, "UVCCamera SetFWRegisterValue result:" + ret_fw);
                       break;
                   case ID_ASIC:
                       int address_asic = Integer.parseInt(address_edit, 16);
                       int vaule_asic = Integer.parseInt(vaule_edit, 16);
                       int ret_asic = mUVCCamera.setHWRegisterValue(address_asic, vaule_asic);
                       Log.i(TAG, "UVCCamera setHWRegisterValue result:" + ret_asic);
                       break;
                   case ID_I2C:
                       String slaveId_edit = mEditText_SlaveId.getText().toString();
                       if (TextUtils.isEmpty(slaveId_edit)) {
                           Toast.makeText(mContext, R.string.firmware_register_hint, Toast.LENGTH_SHORT).show();
                           return;
                       }
                       int address_i2c = Integer.parseInt(address_edit, 16);
                       int slaveId = Integer.parseInt(slaveId_edit, 16);
                       int flag = 0;
                       if(mCheckBox_Address.isChecked()) {
                           flag |= EtronCamera.FG_Address_2Byte;
                       } else {
                           flag |= EtronCamera.FG_Address_1Byte;
                       }

                       if(mCheckBox_Value.isChecked()) {
                           flag |= EtronCamera.FG_Value_2Byte;
                       } else {
                           flag |= EtronCamera.FG_Value_1Byte;
                       }
                       int vaule_i2c = Integer.parseInt(vaule_edit, 16);
                       int ret_i2c = mUVCCamera.setSensorRegisterValue(slaveId, address_i2c, vaule_i2c, flag);
                       Log.i(TAG, "UVCCamera setSensorRegisterValue result:" + ret_i2c);
                       break;
               }
            } else {
                Log.i(TAG, "uvc camera do not connect");
                Toast.makeText(mContext, "uvc camera do not connect", Toast.LENGTH_SHORT).show();
            }
        }

        private RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener =
            new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radiobtn_fw:
                        mCurrent = ID_FW;
                        mCheckBox_Address.setChecked(false);
                        mCheckBox_Value.setChecked(false);
                        mCheckBox_Address.setEnabled(false);
                        mCheckBox_Value.setEnabled(false);
                        mEditText_SlaveId.setEnabled(false);
                        break;
                    case R.id.radiobtn_asic:
                        mCurrent = ID_ASIC;
                        mCheckBox_Address.setChecked(true);
                        mCheckBox_Value.setChecked(false);
                        mCheckBox_Address.setEnabled(false);
                        mCheckBox_Value.setEnabled(false);
                        mEditText_SlaveId.setEnabled(false);
                        break;
                    case R.id.radiobtn_i2c:
                        mCurrent = ID_I2C;
                        mCheckBox_Address.setEnabled(true);
                        mCheckBox_Value.setEnabled(true);
                        mCheckBox_Address.setChecked(false);
                        mCheckBox_Value.setChecked(false);
                        mEditText_SlaveId.setEnabled(true);
                        break;
                }
            }
        };
    }

}