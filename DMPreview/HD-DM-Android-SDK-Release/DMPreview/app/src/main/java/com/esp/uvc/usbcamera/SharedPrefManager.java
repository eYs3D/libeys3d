package com.esp.uvc.usbcamera;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/**
 * Created by GrayJao on 2017/11/2.
 */

abstract public class SharedPrefManager {

    private String mPrefKey = "settings_prf";

    private boolean isChanged = false;
    private Map<String, Object> mSettings;

    private SharedPreferences mSharedPreferences;

    protected SharedPrefManager(Context context, String key) {

        mPrefKey = key;
        mSharedPreferences = context.getApplicationContext().getSharedPreferences(mPrefKey, Context.MODE_PRIVATE);
        mSettings = (Map<String, Object>) getAll();
        initDefaultValue();
    }

    abstract void initDefaultValue();

    public void put(String key, Object object) {

        mSettings.put(key, object);
        isChanged = true;
    }

    public void saveAll() {

        SharedPreferences.Editor editor = mSharedPreferences.edit();

        for(Map.Entry<String, ?> entry : mSettings.entrySet()) {

            String key = entry.getKey();
            Object object = entry.getValue();

            if (object instanceof String) {
                editor.putString(key, (String) object);
            } else if (object instanceof Integer) {
                editor.putInt(key, (Integer) object);
            } else if (object instanceof Boolean) {
                editor.putBoolean(key, (Boolean) object);
            } else if (object instanceof Float) {
                editor.putFloat(key, (Float) object);
            } else if (object instanceof Long) {
                editor.putLong(key, (Long) object);
            } else {
                editor.putString(key, object == null ? null :object.toString());
            }
        }

        editor.commit();
        isChanged = false;
    }

//    public Object get(Context context, String key, Object defaultObject) {
//
//        Object obj = mSettings.get(key);
//
//        if (defaultObject instanceof String) {
//            return mSharedPreferences.getString(key, (String) defaultObject);
//        } else if (defaultObject instanceof Integer) {
//            return mSharedPreferences.getInt(key, (Integer) defaultObject);
//        } else if (defaultObject instanceof Boolean) {
//            return mSharedPreferences.getBoolean(key, (Boolean) defaultObject);
//        } else if (defaultObject instanceof Float) {
//            return mSharedPreferences.getFloat(key, (Float) defaultObject);
//        } else if (defaultObject instanceof Long) {
//            return mSharedPreferences.getLong(key, (Long) defaultObject);
//        }
//
//        return null;
//    }

    public String get(String key, String defaultValue) {

        Object obj = mSettings.get(key);
        if(obj != null && obj instanceof String) {
            return (String) obj;
        } else {
            String value = mSharedPreferences.getString(key, defaultValue);
            mSettings.put(key, value);

            return value;
        }
    }

    public int get(String key, int defaultValue) {

        Object obj = mSettings.get(key);
        if(obj != null && obj instanceof Integer) {
            return (int) obj;
        } else {
            int value = mSharedPreferences.getInt(key, defaultValue);
            mSettings.put(key, value);

            return value;
        }
    }

    public boolean get(String key, boolean defaultValue) {

        Object obj = mSettings.get(key);
        if(obj != null && obj instanceof Boolean) {
            return (boolean) obj;
        } else {
            boolean value = mSharedPreferences.getBoolean(key, defaultValue);
            mSettings.put(key, value);

            return value;
        }
    }

    public float get(String key, float defaultValue) {

        Object obj = mSettings.get(key);
        if(obj != null && obj instanceof Float) {
            return (float) obj;
        } else {
            float value = mSharedPreferences.getFloat(key, defaultValue);
            mSettings.put(key, value);

            return value;
        }
    }

    public long get(String key, long defaultValue) {

        Object obj = mSettings.get(key);
        if(obj != null && obj instanceof Long) {
            return (long) obj;
        } else {
            long value = mSharedPreferences.getLong(key, defaultValue);
            mSettings.put(key, value);

            return value;
        }
    }

    protected void remove(String key) {

        mSettings.remove(key);

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(key);
        editor.commit();
    }

    protected void clear() {

        mSettings.clear();

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    protected boolean contains(String key) {
        return mSettings.containsKey(key) || mSharedPreferences.contains(key);
    }

    protected Map<String, ?> getAll() {

        if(mSettings != null && isChanged) {
            return mSettings;
        } else {
            return mSharedPreferences.getAll();
        }
    }

    @Override
    protected void finalize() throws Throwable {

        if(isChanged) {
            saveAll();
        }
    }
}
