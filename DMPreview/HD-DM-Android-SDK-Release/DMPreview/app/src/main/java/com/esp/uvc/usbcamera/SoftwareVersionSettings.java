package com.esp.uvc.usbcamera;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.esp.android.usb.camera.core.EtronCamera;
import com.esp.uvc.BuildConfig;
import com.esp.uvc.R;


public class SoftwareVersionSettings extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = "SoftwareVersionSettings";


    @Override
    public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         getFragmentManager().beginTransaction()
             .replace(android.R.id.content, new SettingsFragment())
             .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        private View mRootView;
        private String mVersionAPP ="";
        private String mVersionSDK="";
        private TextView mTextViewVersion ;
        private TextView mTextViewVersionSDK ;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            readVersion();

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mRootView = inflater.inflate(R.layout.software_version_settings, null);
            initUI();
            return mRootView;
        }
        private void initUI() {
            mTextViewVersion = (TextView) mRootView.findViewById(R.id.app_version);
            mTextViewVersionSDK = (TextView) mRootView.findViewById(R.id.sdk_version);
            mTextViewVersion.setText("App Version:"+ mVersionAPP);
            mTextViewVersionSDK.setText("SDK Version:"+mVersionSDK);
        }

        void readVersion(){

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(BuildConfig.VERSION_NAME);
            stringBuilder.append(' ');
            stringBuilder.append('(');
            stringBuilder.append(BuildConfig.BUILD_NUMBER);
            stringBuilder.append(')');

            mVersionAPP = stringBuilder.toString();
//            mVersionSDK = com.esp.android.usb.camera.core.BuildConfig.VERSION_NAME;
            mVersionSDK = EtronCamera.getSDKVerion();

            Log.i(TAG,"Version:"+ mVersionAPP);
            Log.i(TAG,"Version SDK:"+mVersionSDK);
        }
    }

}