package com.esp.uvc.usbcamera;

import android.content.Context;

/**
 * Created by GrayJao on 2017/11/2.
 */

public class AppSettings extends SharedPrefManager {

    private static volatile AppSettings ourInstance;
    private static final String SETTINGS_PRF = "settings_prf";

    /**
     * Keys
     */
    public final static String ONLY_COLOR = "mOnlyColor";
    public final static String ENABLE_STREAM_COLOR = "mEnableStreamColor";
    public final static String ENABLE_STREAM_DEPTH = "mEnableStreamDepth";
    public final static String REVERSE = "mReverse";
    public final static String LANDSCAPE = "mLandscape";
    public final static String FLIP = "mFlip";

    public final static String COLOR_FRAME_RATE = "mColorFrameRate";
    public final static String DEPTH_FRAME_RATE = "mDepthFrameRate";

    public final static String COLOR_W = "Color_W";
    public final static String COLOR_H = "Color_H";
    public final static String DEPTH_W = "Depth_W";
    public final static String Depth_H = "Depth_H";

    public final static String ETRON_INDEX = "mEtronIndex";
    public final static String DEPTH_INDEX = "mDepthIndex";

    public final static String DEPTH_DATA_TYPE = "mDepthDataType";

    public final static String SUPPORTED_SIZE = "mSupportedSize";

    public final static String CURRENT = "mCurrent";
    public final static String AWB_STATUS = "mAWBStatus";
    public final static String CAMERA_SENSOR_CHANGE = "mCameraSensorChange";

    public final static String POST_PROCESS_OPENCL = "mPostProcessOpencl";
    public final static String MONITOR_FRAMERATE = "mMonitorFrameRate";
    public final static String Z_FAR = "mZFar";

    public final static String IS_CLEARED = "IS_CLEARED";

    public final static String LANDSCAPE_MODE = "LANDSCAPE_MODE";
    public final static String UPSIDEDOWN_MODE = "UPSIDEDOWN_MODE";
    public final static String MIRROR_MODE = "MIRROR_MODE";
    public final static String INTERLEAVE_FPS_CHOSEN = "INTERLEAVE_FPS_CHOSEN";

    public final static String IR_OVERRIDE = "IR_OVERRIDE";
    public final static String IR_MIN = "IR_MIN";
    public final static String IR_MAX = "IR_MAX";
    public final static String IR_VALUE = "IR_VALUE";
    public final static String IR_EXTENDED = "IR_EXTENDED";

    public final static String CAMERA_VERSION = "CAMERA_VERSION";

    public final static String KEY_USB_TYPE = "KEY_USB_TYPE";
    public final static String VALUE_USB_TYPE_2 = "2.0";
    public final static String VALUE_USB_TYPE_3 = "3.0";


    public final static String DEPTH_RANGE = "DEPTH_RANGE";

    public final static String IS_USING_PRESET = "IS_USING_PRESET";
    public final static String PRESET_NUMBER = "PRESET_NUMBER";

    public final static String KEY_PRESET_MODE = "KEY_PRESET_MODE";

    /**
     * Sensor Settings
     */
    public final static String KEY_AUTO_EXPOSURE = "KEY_AUTO_EXPOSURE";
    public final static String KEY_EXPOSURE_ABSOLUTE_TIME = "KEY_EXPOSURE_ABSOLUTE_TIME";

    public final static String KEY_AUTO_WHITE_BALANCE = "KEY_AUTO_WHITE_BALANCE";
    public final static String KEY_CURRENT_WHITE_BALANCE = "KEY_CURRENT_WHITE_BALANCE";
    public final static String KEY_MIN_WHITE_BALANCE = "KEY_MIN_WHITE_BALANCE";
    public final static String KEY_MAX_WHITE_BALANCE = "KEY_MAX_WHITE_BALANCE";

    public final static String KEY_CURRENT_LIGHT_SOURCE = "KEY_CURRENT_LIGHT_SOURCE";
    public final static String KEY_MIN_LIGHT_SOURCE = "KEY_MIN_LIGHT_SOURCE";
    public final static String KEY_MAX_LIGHT_SOURCE = "KEY_MAX_LIGHT_SOURCE";

    public final static String KEY_LOW_LIGHT_COMPENSATION = "KEY_LOW_LIGHT_COMPENSATION";

    /**
     * PLY related
     */
    public final static String KEY_POST_PROCESS_PLY = "KEY_POST_PROCESS_PLY";

    /**
     * Default values
     */
    private final static int WINDOWS_TYPE_DEFAULT = 1;
    private final static boolean ENABLE_STREAM_COLOR_DEFAULT = true;
    private final static boolean ENABLE_STREAM_DEPTH_DEFAULT = true;
    private final static boolean ONLY_COLOR_DEFAULT = false;
    private final static boolean REVERSE_DEFAULT = true;
    private final static boolean LANDSCAPE_DEFAULT = false;
    private final static boolean FLIP_DEFAULT = true;
    private final static boolean POST_PROCESS_OPENCL_DEFAULT = false;
    private final static boolean MONITOR_FRAMERATE_DEFAULT = false;
    private final static int Color_W_DEFAULT = 0;
    private final static int Color_H_DEFAULT = 0;
    private final static int Depth_W_DEFAULT = 0;
    private final static int Depth_H_DEFAULT = 0;
    public  final static boolean POST_PROCESS_PLY_DEFAULT = false;

    private final static int ONLY_COLOR_POSITION_DEFAULT = 0;
    private final static int ETRON_POSITION_DEFAULT = 0;
    private final static int DEPTH_POSITION_DEFAULT = 0;

    private final static int FISH_POSITION_DEFAULT = 0;
    private final static int FISH_RECORD_FRAME_DEFAULT = 60;


    public static AppSettings getInstance(Context context) {

        if (ourInstance == null) {
            synchronized (AppSettings.class) {
                if (ourInstance == null)
                    ourInstance = new AppSettings(context);
            }
        }
        return ourInstance;
    }

    private AppSettings(Context context) {
        super(context, SETTINGS_PRF);
    }

    public void init() {
        initDefaultValue();
    }

    @Override
    void initDefaultValue() {

        if (!contains(ONLY_COLOR)) {
            put(ONLY_COLOR, ONLY_COLOR_DEFAULT);
        }
        if (!contains(REVERSE)) {
            put(REVERSE, REVERSE_DEFAULT);
        }
        if (!contains(LANDSCAPE)) {
            put(LANDSCAPE, LANDSCAPE_DEFAULT);
        }
        if (!contains(FLIP)) {
            put(FLIP, FLIP_DEFAULT);
        }
        if (!contains(POST_PROCESS_OPENCL)) {
            put(POST_PROCESS_OPENCL, POST_PROCESS_OPENCL_DEFAULT);
        }
        if (!contains(MONITOR_FRAMERATE)) {
            put(MONITOR_FRAMERATE, MONITOR_FRAMERATE_DEFAULT);
        }
        if (!contains(KEY_POST_PROCESS_PLY)) {
            put(KEY_POST_PROCESS_PLY, POST_PROCESS_PLY_DEFAULT);
        }
    }

    @Override
    public void clear() {
        super.clear();
        super.put(IS_CLEARED, true);
    }

    @Override
    public void put(String key, Object object) {
        super.put(key, object);
        super.put(IS_CLEARED, false);
    }

}
