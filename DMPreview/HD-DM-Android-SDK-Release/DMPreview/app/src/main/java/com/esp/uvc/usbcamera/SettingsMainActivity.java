package com.esp.uvc.usbcamera;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import androidx.appcompat.app.AppCompatActivity;

import com.esp.uvc.R;


public class SettingsMainActivity extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = "SettingsMianActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         getFragmentManager().beginTransaction()
             .replace(android.R.id.content, new SettingsFragment())
             .commit();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_main);
        }
    }

}