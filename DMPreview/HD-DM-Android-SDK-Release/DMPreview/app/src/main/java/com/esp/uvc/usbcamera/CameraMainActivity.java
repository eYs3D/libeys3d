package com.esp.uvc.usbcamera;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.esp.android.usb.camera.core.EtronCamera;
import com.esp.android.usb.camera.core.IFrameCallback;
import com.esp.android.usb.camera.core.RectifyLogData;
import com.esp.android.usb.camera.core.StreamInfo;
import com.esp.android.usb.camera.core.USBMonitor;
import com.esp.android.usb.camera.core.USBMonitor.OnDeviceConnectListener;
import com.esp.android.usb.camera.core.USBMonitor.UsbControlBlock;
import com.esp.android.usb.camera.core.UVCCamera;
import com.esp.uvc.R;
import com.esp.uvc.utility.ColorPaletteGenerator;
import com.esp.uvc.utility.PlyWriter;
import com.esp.uvc.widget.UVCCameraTextureView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.esp.android.usb.camera.core.EtronCamera.PRODUCT_VERSION_EX8029;
import static com.esp.android.usb.camera.core.EtronCamera.PRODUCT_VERSION_EX8036;
import static com.esp.android.usb.camera.core.EtronCamera.PRODUCT_VERSION_EX8037;


public class CameraMainActivity extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = "CameraMainActivity";

    private ImageView mImageViewSavePly = null;
    private ImageView mImageViewMeasure = null;
    private ImageView mImageViewFive = null;
    private SeekBar mIRSeekBar;
    private TextView mText_IRValue;
    private TextView mTextViewIR = null;
    private LinearLayout mMainLayoutTop;
    private RelativeLayout mRelativeLayout_L;
    private RelativeLayout mRelativeLayout_R;
    private RelativeLayout mRelativeLayout_processed;
    private LinearLayout mBtnContainer = null;

    private ImageView mImageViewMeasureSpot;
    private TextView mTextViewMeasure;
    private TextView mTextViewFrameRate;
    private Context mContext;


    // for thread pool
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 10;
    protected static final ThreadPoolExecutor EXECUTER
        = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor = null;
    private EtronCamera mUVCCamera = null;
    private UVCCameraTextureView mUVCCameraViewR = null;
    private UVCCameraTextureView mUVCCameraViewL = null;
    private UVCCameraTextureView mUVCCameraViewProcessed = null;
    private Surface mLeftPreviewSurface = null;
    private Surface mRightPreviewSurface = null;
    private boolean mOnlyColor = true;
    private boolean mEnableStreamColor = true;
    private boolean mEnableStreamDepth = true;
    private int mColorCount = 0;
    private int mDepthCount = 0;
    private int Color_W = 640;
    private int Color_H = 480;
    private int Depth_W = 320;
    private int Depth_H = 480;
    private int mStreamInfoIndexColor = 0;
    private int mStreamInfoIndexDepth = 0;
    private int mDepthDataType = 1;
    private UsbDevice mUsbDevice = null;
    private boolean mReverse = false;
    private boolean mLandscape = true;
    private boolean mFlip = false;
    private boolean mAEStatus = false;
    private boolean mAWBStatus = false;
    private boolean mCameraSensorChange = false;
    private boolean mPostProcessingOpencl =false;
    private boolean mIsRunning = false;
    //Menu contorl
    private Menu mOptionsMenu;
    private static final int MENU_SETTINGS = Menu.FIRST;

    //Handler msg
    public final static int MSG_RECONNECT_HANDLE = 0;
    public final static int MSG_SETTINGS = 1;
    public final static int MSG_PROGRESS_ON = 101;
    public final static int MSG_PROGRESS_OFF = 102;
    public final static int MSG_SHOW_TOAST = 103;

    String mProductVersion;

    // IR default value
    public static final int IR_DEFAULT_VALUE_8029 =   0x0A;
    public static final int IR_DEFAULT_VALUE_8036 =   0x04;
    public static final int IR_DEFAULT_VALUE_8037 =   0x04;
    public static final int IR_DEFAULT_VALUE_8029_MAX =   0x10;
    public static final int IR_DEFAULT_VALUE_8036_MAX =   0x06;
    public static final int IR_DEFAULT_VALUE_8037_MAX =   0x0A;

    // IR parameter
    private boolean mHasIR = false;
    private boolean mIRSwitch = false;
    private int mIRValueCurrent = 0;
    private int mIRValueDefault = 0;
    private int mIRValueMax = 0;

    private final boolean mMoveMeasureSpot = false;
    private boolean mShowMeasure = false;
    private boolean mMonitorFrameRate = false;
    private int mColorFrameRate = 30;
    private int mDepthFrameRate = 30;
    private int mZFar = 1000;//mm

    private int[] mZDBuffer;

    private final int[] mSupportPidList = {
            0x0112, //EX8037
    };

    private FrameGrabber mFrameGrabber = new FrameGrabber();
    private Dialog mProgressDialog;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.camera_main);
        initAll();
        // mOnDeviceConnectListener is callback interface for USB event.
        mUSBMonitor = new USBMonitor(CameraMainActivity.this, mOnDeviceConnectListener);
        if (mUSBMonitor == null) {
            Log.d(TAG, "Error!! can not get USBMonitor " );
            return;
        }
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int requestCode = 200;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(permissions, requestCode);
        }
    }


    @Override
    public void onResume() {

        super.onResume();

        readSettings();
        checkViewOrientation();

       //Register to monitor USB events.
        if (mUSBMonitor != null)
            mUSBMonitor.register();

        if (mUVCCamera != null) {
            mUVCCamera.destroy();
            mUVCCamera = null;
        }

        updateOptionsMenu();
        calculateDisplaySize(true);

        resetIR();
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

        mUVCCameraViewR = null;
        mUVCCameraViewL = null;
        super.onDestroy();
    }



    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {

        @Override
        public void onAttach(final UsbDevice device) {

            final int n = mUSBMonitor.getDeviceCount();
            UsbDevice usbDevice = null;
            if (DEBUG) Log.v(TAG, ">>>> onAttach getDeviceCount:" + n);

            //Get devices list
            if(n == 1) {
                usbDevice = mUSBMonitor.getDeviceList().get(0);
            } else if(n > 1) {
                for (UsbDevice deviceInfo : mUSBMonitor.getDeviceList()) {
                    for(int pid : mSupportPidList){
                        if(pid == deviceInfo.getProductId()){
                            usbDevice = deviceInfo;
                            break;
                        }
                    }
                    if(usbDevice!=null)break;
                }
            }

            //Request permission
            if(usbDevice == null) {
                CameraDialog.showDialog(CameraMainActivity.this, mUSBMonitor);
            } else {
                mUSBMonitor.requestPermission(usbDevice);
            }
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            if (mUVCCamera != null) {
                Log.i(TAG,"Camera already exist.");
                return;
            }
            if (DEBUG) Log.v(TAG, ">>>> onConnect UsbDevice:" + device);
            if (DEBUG) Log.v(TAG, ">>>> onConnect UsbControlBlock:" + ctrlBlock);
            if (DEBUG) Log.v(TAG, ">>>> onConnect createNew:" + createNew);
            mUVCCamera = new EtronCamera();
            EXECUTER.execute(new Runnable() {
                @Override
                public void run() {
                    mUVCCamera.open(ctrlBlock);
                    updatePreviewSizeSetting(); // for shared preference setting

                    //Get resolution and format from device.
                    StreamInfo[] streamInfoColorList = mUVCCamera.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_COLOR);
                    StreamInfo[] streamInfoDepthList = mUVCCamera.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_DEPTH);

                    if(streamInfoColorList != null && streamInfoColorList.length > 0){
                        Color_W = streamInfoColorList[mStreamInfoIndexColor].width;
                        Color_H = streamInfoColorList[mStreamInfoIndexColor].height;
                    }

                    if(streamInfoDepthList != null && streamInfoDepthList.length > 0){
                        Depth_W = streamInfoDepthList[mStreamInfoIndexDepth].width;
                        Depth_H = streamInfoDepthList[mStreamInfoIndexDepth].height;

                        if(mDepthDataType == EtronCamera.VideoMode.RECTIFY_8_BITS || mDepthDataType == EtronCamera.VideoMode.RAW_8_BITS) {
                            Depth_W *= 2;
                        }
                    }

                    mProductVersion = mUVCCamera.getProductVersion();
                    getZDTableValue();
                    setMeasureCallback();
                    checkIRValue();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIRSeekBar.setMax(mIRValueMax);
                            mIRSeekBar.setProgress(mIRValueCurrent);
                            mIRSwitch = mIRValueCurrent > 0 ? true : false;
                            setIRIcon();
                            calculateDisplaySize(false);
                        }
                    });

                    if (mLeftPreviewSurface != null) {
                        mLeftPreviewSurface.release();
                        mLeftPreviewSurface = null;
                    }
                    if (mRightPreviewSurface != null) {
                        mRightPreviewSurface.release();
                        mRightPreviewSurface = null;
                    }

                    try {
                        if (mUVCCamera != null  ) {
                            int result = mUVCCamera.setVideoMode(mDepthDataType);
                            Log.i(TAG, "setVideoMode:" + result);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "setVideoMode exception:" + e.toString());
                        return;
                    }

                    if (mUVCCamera != null && mEnableStreamColor && streamInfoColorList != null && streamInfoColorList.length >0) {
                        try {
                            if (DEBUG) Log.d(TAG, "set color uvccamera");
                            mUVCCamera.setPreviewSize(streamInfoColorList[mStreamInfoIndexColor], mColorFrameRate);
                            //set texture for preview.
                            final SurfaceTexture surfaceTexture_l = mUVCCameraViewL.getSurfaceTexture();
                            if (surfaceTexture_l != null)mLeftPreviewSurface = new Surface(surfaceTexture_l);
                            mUVCCamera.setPreviewDisplay(mLeftPreviewSurface, EtronCamera.CAMERA_COLOR);
                            //callback function for processing.
                            mUVCCamera.setFrameCallback(mColorIFrameCallback, EtronCamera.PIXEL_FORMAT_RGBX, EtronCamera.CAMERA_COLOR);
                            mUVCCamera.startPreview(EtronCamera.CAMERA_COLOR);
                            mIsRunning= true;
                        } catch (Exception e) {
                            Log.e(TAG, "set color uvccamera exception:" + e.toString());
                            return;
                        }
                    }

                    if (mUVCCamera != null && mEnableStreamDepth && streamInfoDepthList != null && streamInfoDepthList.length >0) {
                        try {
                            if (DEBUG) Log.d(TAG, "set depth uvccamera");
                            mUVCCamera.setPreviewSize(streamInfoDepthList[mStreamInfoIndexDepth], mDepthFrameRate);
                            final SurfaceTexture surfaceTexture_r = mUVCCameraViewR.getSurfaceTexture();
                            if (surfaceTexture_r != null)mRightPreviewSurface = new Surface(surfaceTexture_r);
                            mUVCCamera.setPreviewDisplay(mRightPreviewSurface, EtronCamera.CAMERA_DEPTH);
                            mUVCCamera.startPreview(EtronCamera.CAMERA_DEPTH);
                            mIsRunning= true;
                        } catch (Exception e) {
                            Log.e(TAG, "set depth uvccamera exception:" + e.toString());
                            return;
                        }
                    }

                    if(mUVCCamera != null && mPostProcessingOpencl && mEnableStreamColor && mEnableStreamDepth) {
                        EXECUTER.execute(mRunnablePostProcessingOpencl);
                    }

                    if(mUVCCamera != null &&mMonitorFrameRate){
                        monitorFrameRate();
                    }
                }
            });
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect");

            mShowMeasure = false;
            setMeasure();
            mProductVersion = "";
            mIRValueMax = 0;

            if (mUVCCamera != null && device.equals(mUVCCamera.getDevice())) {
                if (mUVCCamera != null) {
                    mUVCCamera.stopPreview(EtronCamera.CAMERA_COLOR);
                    mUVCCamera.stopPreview(EtronCamera.CAMERA_DEPTH);
                }
                if (mLeftPreviewSurface != null) {
                    mLeftPreviewSurface.release();
                    mLeftPreviewSurface = null;
                }
                if (mRightPreviewSurface != null) {
                    mRightPreviewSurface.release();
                    mRightPreviewSurface = null;
                }
                //mUVCCamera.releaseSwPostProc();
                mIsRunning = false;
                mUVCCamera.close();
                mUVCCamera.destroy();
                mUVCCamera = null;
            }
        }

        @Override
        public void onDetach(final UsbDevice device) {
            Toast.makeText(CameraMainActivity.this, R.string.usb_device_detached, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel() {
            if (DEBUG) Log.v(TAG, "onCancel:");
        }
    };

    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }


    private final IFrameCallback mColorIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame, int frameCount) {
            if (DEBUG) Log.v(TAG, "onFrame color callback frame:" + frame);
            mColorCount ++;
        }
    };

    private void saveRGBXImage(ByteBuffer frame, String filename){
        BufferedOutputStream bos = null;
        try {
            File outputFile = new File(filename);
            bos = new BufferedOutputStream(new FileOutputStream(outputFile));
            Bitmap bmp = Bitmap.createBitmap(Color_W,Color_H, Bitmap.Config.ARGB_8888);
            frame.rewind();
            bmp.copyPixelsFromBuffer(frame);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bmp.recycle();
            Log.i(TAG, ""+mColorCount+":" + Color_W + "x" + Color_H + " frame as '" + outputFile.getAbsolutePath() + "'");
        } catch (Exception e) {
            Log.e(TAG, "saveCurrentFrame error:" + e.toString());
        }

    }

    private final IFrameCallback mDepthIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame, int frameCount) {
            if (DEBUG) Log.v(TAG, "onFrame depth callback frame:" + frame);

        }
    };
    private Runnable mRunnableMonitorFrameRate = new Runnable(){
        public void run() {

            while(mIsRunning && mUVCCamera != null) {
                final EtronCamera.CurrentFrameRate frameRateColor = mUVCCamera.getCurrentFrameRate(EtronCamera.CAMERA_COLOR);
                final EtronCamera.CurrentFrameRate frameRateDepth = mUVCCamera.getCurrentFrameRate(EtronCamera.CAMERA_DEPTH);

                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String fpsString="";
                            if(mEnableStreamColor && frameRateColor != null){
                                fpsString +=String.format("Color FPS uvc:%2.2f preview:%2.2f\n",frameRateColor.mFrameRateUvc, frameRateColor.mFrameRatePreview);
                            }
                            if(mEnableStreamDepth && frameRateColor != null){
                                fpsString += String.format("Depth FPS uvc:%2.2f preview:%2.2f",frameRateDepth.mFrameRateUvc, frameRateDepth.mFrameRatePreview);
                            }
                            mTextViewFrameRate.setText(fpsString);
                        }
                    });
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e(TAG, "Exception:" + e.toString());
                }
            }
        }
    };
    private final Runnable mRunnablePostProcessingOpencl = new Runnable() {
        @Override
        public void run() {
            mUVCCamera.createSwPostProc(8);
            final int COLORPALETTE_SIZE=256;
            byte colorPalette[] = new byte[COLORPALETTE_SIZE*4];
            boolean reverseRedToBlue = true;

            ColorPaletteGenerator.generatePalette(colorPalette,COLORPALETTE_SIZE,reverseRedToBlue);

            for(int i=0;i<COLORPALETTE_SIZE;i++){
                Log.i(TAG,String.format("colorPalette[%d]:%3d,%3d,%3d",i,colorPalette[i*4+0],colorPalette[i*4+1],colorPalette[i*4+2]));
            }

            mUVCCamera.setFrameCallback(mFrameGrabber.getCallbackColor(), EtronCamera.PIXEL_FORMAT_RGBX, EtronCamera.CAMERA_COLOR);
            mUVCCamera.setFrameCallback(mFrameGrabber.getCallbackDepth(), EtronCamera.FRAME_FORMAT_YUYV, EtronCamera.CAMERA_DEPTH);
            while (mIsRunning) {
                mFrameGrabber.grabLatestFrame();
                ByteBuffer frameDepth = mFrameGrabber.getLatestFrameDepth();
                ByteBuffer frameColor = mFrameGrabber.getLatestFrameColor();
                ByteBuffer framePostprocess = ByteBuffer.allocateDirect(frameDepth.capacity()); // disparity to RGBX
                mUVCCamera.doSwPostProc(frameColor.array(), true, frameDepth.array(), framePostprocess.array(), Color_W, Color_H);
                ByteBuffer frameResult = ByteBuffer.allocateDirect(frameDepth.capacity() * 4); // disparity to RGBX
                byte[] byteArray = frameResult.array();
                byte[] byteArrayProcessed = framePostprocess.array();
                for (int i = 0; i < framePostprocess.capacity(); i++) {
                    byte d = byteArrayProcessed[i];
                    byteArray[i * 4 + 0] = colorPalette[(0xff & d)*4 + 0];
                    byteArray[i * 4 + 1] = colorPalette[(0xff & d)*4 + 1];
                    byteArray[i * 4 + 2] = colorPalette[(0xff & d)*4 + 2];
                    byteArray[i * 4 + 3] = (byte) 0xff;
                }

                if (mUVCCameraViewProcessed != null) {
                    Matrix matrix = new Matrix();
                    Bitmap mFrame;

                    int width = Color_W;
                    int height = Color_H;
                    mFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    final float scaleX = mUVCCameraViewProcessed.getWidth() / (float) width;
                    final float scaleY = mUVCCameraViewProcessed.getHeight() / (float) height;
                    matrix.reset();
                    matrix.postScale(scaleX, scaleY);

                    try {
                        frameResult.clear();
                        mFrame.copyPixelsFromBuffer(frameResult);
                        final Canvas canvas = mUVCCameraViewProcessed.lockCanvas();
                        if (canvas != null) {
                            try {
                                canvas.drawBitmap(mFrame, matrix, null);
                            } catch (final Exception e) {
                                Log.w(TAG, e);
                            } finally {
                                mUVCCameraViewProcessed.unlockCanvasAndPost(canvas);
                            }
                        }
                    } catch (final Exception e) {
                        Log.w(TAG, e);
                    }
                }
            }
            mUVCCamera.releaseSwPostProc();
            final Canvas canvas = mUVCCameraViewProcessed.lockCanvas();
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.BLACK);
                } catch (final Exception e) {
                    Log.w(TAG, e);
                } finally {
                    mUVCCameraViewProcessed.unlockCanvasAndPost(canvas);
                }
            }
        }
    };

    private void updatePreviewSizeSetting(){
        String supportedSize = mUVCCamera.getSupportedSize();
        if (DEBUG) Log.i(TAG, "supportedSize:" + supportedSize );
        if(supportedSize != null && !supportedSize.isEmpty()){
            int streamInfoListSizeColor = mUVCCamera.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_COLOR).length;
            int streamInfoListSizeDepth = mUVCCamera.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_DEPTH).length;
            if( mStreamInfoIndexColor >= streamInfoListSizeColor || mStreamInfoIndexColor < 0){
                mStreamInfoIndexColor = 0;
            }
            if( mStreamInfoIndexDepth >= streamInfoListSizeDepth || mStreamInfoIndexDepth < 0){
                mStreamInfoIndexDepth = 0;
            }
            //Set preference
            AppSettings appSettings = AppSettings.getInstance(mContext);
            appSettings.put(AppSettings.ETRON_INDEX,mStreamInfoIndexColor);
            appSettings.put(AppSettings.DEPTH_INDEX,mStreamInfoIndexDepth);
            appSettings.put(AppSettings.SUPPORTED_SIZE,supportedSize);
            appSettings.saveAll();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        mOptionsMenu.add(0, MENU_SETTINGS, 1, R.string.btn_settings)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        updateOptionsMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case MENU_SETTINGS:
                Log.i(TAG, "MENU_SETTINGS...");
                mHandler.sendEmptyMessage(MSG_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateOptionsMenu() {
        if (mOptionsMenu == null) return;
        mOptionsMenu.findItem(MENU_SETTINGS).setVisible(true);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECONNECT_HANDLE:
                    if (mUVCCamera != null) {
                        mUVCCamera.destroy();
                        mUVCCamera = null;
                    }
                    if (mUsbDevice != null)
                        mUSBMonitor.requestPermission(mUsbDevice);
                    break;
                case MSG_SETTINGS:
                    final Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setClass(mContext, SettingsMainActivity.class);
                    mContext.startActivity(intent);
                    break;
                case MSG_PROGRESS_ON:
                    setProgressDialog(true);
                    break;
                case MSG_PROGRESS_OFF:
                    setProgressDialog(false);
                    break;
                case MSG_SHOW_TOAST:
                    Toast.makeText(CameraMainActivity.this, (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;

            }
            super.handleMessage(msg);
       }
    };

    private final OnClickListener mSettingsListener = new OnClickListener() {
        public void onClick(View v) {
            if (mHandler != null)
                mHandler.sendEmptyMessage(MSG_SETTINGS);
        }
    };

    private final OnClickListener mMeasureListener = new OnClickListener() {
        public void onClick(View v) {

            if(mUVCCamera != null ) {
                mShowMeasure = !mShowMeasure;
                setMeasure();
            }
        }
    };

    private final OnClickListener mSavePlyListener = new OnClickListener() {
        public void onClick(View v) {
            if(mUVCCamera != null  && (mEnableStreamColor && mEnableStreamDepth)) {
//                if( Color_W != Depth_W* (mDepthDataType == EtronCamera.DEPTH_DATA_8_BITS? 2:1)
//                        || Color_H != Depth_H){
                if( Color_W != Depth_W || Color_H != Depth_H){
                    Toast.makeText(CameraMainActivity.this, "error:Wrong resolution set...", Toast.LENGTH_LONG).show();
                    return;
                }

                mUVCCamera.setFrameCallback(mFrameGrabber.getCallbackColor(), EtronCamera.PIXEL_FORMAT_RGBX, EtronCamera.CAMERA_COLOR);
                mUVCCamera.setFrameCallback(mFrameGrabber.getCallbackDepth(), EtronCamera.FRAME_FORMAT_YUYV, EtronCamera.CAMERA_DEPTH);

                int deviceType = mUVCCamera.getDeviceType();
                int index = 0;

                switch (deviceType){//0:OTHERS, 1:AXES1, 2:PUMA
                    case 1:
                        if (Depth_H == 480) index = 0;
                        if (Depth_H == 240) index = 1;
                        if (Depth_H == 400) index = 2;
                        break;
                    case 2:
                        if (Depth_H == 720) index = 0; // HD index = 0
                        if (Depth_H == 480) index = 1; // VGA index = 1
                        break;
                }

                final int finalIndex = index;
                new Thread(new Runnable() {
                        @Override
                        public void run() {

                            mHandler.sendEmptyMessage(MSG_PROGRESS_ON);

                            final RectifyLogData rectifyLogData = mUVCCamera.getRectifyLogData(finalIndex);
                            if(rectifyLogData !=null) {
                                Log.i(TAG, "rectifyLogData:" + rectifyLogData.toString());

                                mFrameGrabber.grabLatestFrame();
                                ByteBuffer frameDepth = mFrameGrabber.getLatestFrameDepth();
                                ByteBuffer frameColor = mFrameGrabber.getLatestFrameColor();

                                ArrayList<PlyWriter.CloudPoint> cloudPoints = new ArrayList<>();
                                cloudPoints = PlyWriter.etronFrameTo3D(Color_W, Color_H, frameColor.array(), frameDepth.array(), rectifyLogData.getNormalizationReProjectMat(), 1, true, 0.0f, mZFar, true);
                                String dirPath = Environment.getExternalStorageDirectory() + "/" + "uvccamera/";
                                String filename = getDateTimeString() + ".ply";
                                File dir = new File(dirPath);
                                if (!dir.exists()) {
                                    dir.mkdirs();
                                }
                                final File file = new File(dirPath + filename);

                                PlyWriter.writePly(cloudPoints, file);

                                Message msg = new Message();
                                msg.what = MSG_SHOW_TOAST;
                                msg.obj = "Save ply:" + dirPath + filename;
                                mHandler.sendMessage(msg);

                            } else{
                                Message msg = new Message();
                                msg.what = MSG_SHOW_TOAST;
                                msg.obj = "Fail to get rectifyLogData";
                                mHandler.sendMessage(msg);
                            }

                            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
                    }}).start();
            }
        }
    };

    private void setProgressDialog(boolean show){
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        //View view = getLayoutInflater().inflate(R.layout.progress);
//        builder.setView(R.layout.progress);
//        Dialog mProgressDialog = builder.setCancelable(false).create();
        if (show)mProgressDialog.show();
        else mProgressDialog.dismiss();
    }

    private static final String getDateTimeString() {
        SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }

    public void checkViewOrientation() {

        AppSettings appSettings = AppSettings.getInstance(mContext);

        mReverse = appSettings.get(AppSettings.REVERSE, mReverse);
        mLandscape = appSettings.get(AppSettings.LANDSCAPE, mLandscape);
        mFlip = appSettings.get(AppSettings.FLIP, mFlip);
        mPostProcessingOpencl = appSettings.get(AppSettings.POST_PROCESS_OPENCL,mPostProcessingOpencl);
        mMonitorFrameRate = appSettings.get(AppSettings.MONITOR_FRAMERATE,mMonitorFrameRate);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        if(mLandscape) {
            if (mReverse) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else {
            if (mReverse) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        if(mFlip) {
            mUVCCameraViewL.setScaleX(-1);
            mUVCCameraViewR.setScaleX(-1);
            mUVCCameraViewProcessed.setScaleX(-1);
        } else {
            mUVCCameraViewL.setScaleX(1);
            mUVCCameraViewR.setScaleX(1);
            mUVCCameraViewProcessed.setScaleX(1);
        }
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                case R.id.camera_view_L:
                    final int n = mUSBMonitor.getDeviceCount();
                    if (n > 1)
                        CameraDialog.showDialog(CameraMainActivity.this, mUSBMonitor);
                    break;
                case R.id.camera_view_R:
                    break;
            }
        }
    };
    private void initAll() {
        mUVCCameraViewL = (UVCCameraTextureView)findViewById(R.id.camera_view_L);
//        mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraViewL.setOnClickListener(mOnClickListener);
        mUVCCameraViewR = (UVCCameraTextureView)findViewById(R.id.camera_view_R);
//        mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraViewR.setOnClickListener(mOnClickListener);

        mUVCCameraViewProcessed = (UVCCameraTextureView)findViewById(R.id.camera_view_processed);
//        mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraViewProcessed.setOnClickListener(mOnClickListener);

        mTextViewIR = (TextView)findViewById(R.id.text_ir);
        mTextViewIR.setOnClickListener(mIROnClickListener);
        mTextViewIR.setOnLongClickListener(mIROnLongClickListener);
        mTextViewIR.setTextColor(Color.GRAY);
        mTextViewIR.setAlpha(0.7f);

        mIRSeekBar =(SeekBar)findViewById(R.id.seekbar_IR_value);
        mIRSeekBar.setOnSeekBarChangeListener(mIRValueListener);
        mIRSeekBar.setVisibility(View.GONE);

        mText_IRValue=(TextView)findViewById(R.id.textview_IR_value);
        mText_IRValue.setVisibility(View.GONE);

        mImageViewSavePly = (ImageView) findViewById(R.id.image_save_ply);
        mImageViewSavePly.setVisibility(View.GONE);
        mImageViewSavePly.setOnClickListener(mSavePlyListener);

        mImageViewMeasure = (ImageView) findViewById(R.id.image_measure);
        mImageViewMeasure.setImageResource(R.drawable.measure);
        mImageViewMeasure.setVisibility(View.GONE);
        mImageViewMeasure.setOnClickListener(mMeasureListener);

        mImageViewFive = (ImageView) findViewById(R.id.image_five);
        mImageViewFive.setOnClickListener(mSettingsListener);

        mMainLayoutTop = (LinearLayout)findViewById(R.id.main_layout_top);
        mRelativeLayout_L = (RelativeLayout)findViewById(R.id.camera_layout_L);
        mRelativeLayout_R = (RelativeLayout)findViewById(R.id.camera_layout_R);
        mRelativeLayout_processed = (RelativeLayout)findViewById(R.id.camera_layout_processed);
        mBtnContainer = (LinearLayout)findViewById(R.id.btn_container);

        mImageViewMeasureSpot = (ImageView)findViewById(R.id.measure_spot);
        mImageViewMeasureSpot.setVisibility(View.INVISIBLE);
        if(mMoveMeasureSpot) {
            mImageViewMeasureSpot.setOnTouchListener(mMeasureSpotTouchListener);
        }

        mTextViewMeasure = (TextView)findViewById(R.id.tvMeasure);
        mTextViewMeasure.setVisibility(View.INVISIBLE);

        mTextViewFrameRate = (TextView)findViewById(R.id.tvFrameRate);
        mTextViewFrameRate.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //View view = getLayoutInflater().inflate(R.layout.progress);
        builder.setView(R.layout.progress);
        mProgressDialog = builder.setCancelable(false).create();

        mContext = getApplicationContext();

    }

    private void calculateDisplaySize(boolean isOnResume) {

        LinearLayout.LayoutParams params_vl;
        LinearLayout.LayoutParams params_vr;
        LinearLayout.LayoutParams params_vprocessed;
        if(mLandscape) {
            mMainLayoutTop.setOrientation(LinearLayout.HORIZONTAL);
            params_vl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            params_vr = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            params_vprocessed = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
        } else {
            mMainLayoutTop.setOrientation(LinearLayout.VERTICAL);
            params_vl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
            params_vr = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
            params_vprocessed = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        }

        params_vl.weight = 1;
        params_vr.weight = 1;
        params_vprocessed.weight = 1;
        mRelativeLayout_L.setLayoutParams(params_vl);
        mRelativeLayout_R.setLayoutParams(params_vr);
        mRelativeLayout_processed.setLayoutParams(params_vprocessed);

        if (isOnResume) {
            mUVCCameraViewL.setAspectRatio(4/3.0f);
            mUVCCameraViewR.setAspectRatio(4/3.0f);
            mUVCCameraViewProcessed.setAspectRatio(4/3.0f);
        } else {
            mUVCCameraViewL.setAspectRatio(Color_W / (float) Color_H);
            mUVCCameraViewR.setAspectRatio(Depth_W / (float) Depth_H);
            mUVCCameraViewProcessed.setAspectRatio(Depth_W / (float) Depth_H);
        }

        if (mEnableStreamColor && mEnableStreamDepth && mPostProcessingOpencl) {
            mRelativeLayout_processed.setVisibility(View.VISIBLE);
        } else {
            mRelativeLayout_processed.setVisibility(View.GONE);
        }

        if (mEnableStreamColor) {
            mRelativeLayout_L.setVisibility(View.VISIBLE);
        } else {
            mRelativeLayout_L.setVisibility(View.GONE);
        }
        if (mEnableStreamDepth) {
            mRelativeLayout_R.setVisibility(View.VISIBLE);
        } else {
            mRelativeLayout_R.setVisibility(View.GONE);
        }
        if (mEnableStreamColor && mEnableStreamDepth) {
            mImageViewMeasure.setVisibility(View.VISIBLE);
            mImageViewSavePly.setVisibility(View.VISIBLE);
        } else {
            mImageViewMeasure.setVisibility(View.GONE);
            mImageViewSavePly.setVisibility(View.GONE);
        }

        if (mMonitorFrameRate) {
            mTextViewFrameRate.setVisibility(View.VISIBLE);
            mTextViewFrameRate.setText("");
        } else {
            mTextViewFrameRate.setVisibility(View.GONE);
            mTextViewFrameRate.setText("");
        }
    }
    private void readSettings() {
        AppSettings appSettings = AppSettings.getInstance(mContext);
        mEnableStreamColor = appSettings.get(AppSettings.ENABLE_STREAM_COLOR, mEnableStreamColor);
        mEnableStreamDepth = appSettings.get(AppSettings.ENABLE_STREAM_DEPTH, mEnableStreamDepth);
        mStreamInfoIndexColor = appSettings.get((AppSettings.ETRON_INDEX),0);
        mStreamInfoIndexDepth = appSettings.get((AppSettings.DEPTH_INDEX),0);
        mDepthDataType = appSettings.get((AppSettings.DEPTH_DATA_TYPE), EtronCamera.VideoMode.RECTIFY_8_BITS);
        mAWBStatus = appSettings.get(AppSettings.AWB_STATUS, false);
//        mAEStatus = appSettings.get(AppSettings.EXPOSURE_AUTO_ENABLE, false);
        mCameraSensorChange = appSettings.get(AppSettings.CAMERA_SENSOR_CHANGE, false);
        mPostProcessingOpencl = appSettings.get(AppSettings.POST_PROCESS_OPENCL, false);
        mMonitorFrameRate = appSettings.get(AppSettings.MONITOR_FRAMERATE, false);
        mColorFrameRate = appSettings.get(AppSettings.COLOR_FRAME_RATE, 30);
        mDepthFrameRate = appSettings.get(AppSettings.DEPTH_FRAME_RATE, 30);
        mZFar = appSettings.get(AppSettings.Z_FAR,1000);

        if (DEBUG) {
            Log.i(TAG, "readSettings:");
            Log.i(TAG, ">>>> mEnableStreamColor:" + mEnableStreamColor);
            Log.i(TAG, ">>>> mEnableStreamDepth:" + mEnableStreamDepth);
            Log.i(TAG, ">>>> mAWBStatus:" + mAWBStatus);
            Log.i(TAG, ">>>> mAEStatus:" + mAEStatus);
            Log.i(TAG, ">>>> mPostProcessingOpencl:" + mPostProcessingOpencl);
            Log.i(TAG, ">>>> mMonitorFrameRate:" + mMonitorFrameRate);
            Log.i(TAG, ">>>> mColorFrameRate:" + mColorFrameRate);
            Log.i(TAG, ">>>> mDepthFrameRate:" + mDepthFrameRate);
            Log.i(TAG, ">>>> mDepthDataType:" + mDepthDataType);
            Log.i(TAG, ">>>> mZFar:" + mZFar);
        }
    }
    public void setCameraSensorStatus() {

        AppSettings appSettings = AppSettings.getInstance(mContext);
        if (mCameraSensorChange) {
            mCameraSensorChange = false;
            appSettings.put(AppSettings.CAMERA_SENSOR_CHANGE, mCameraSensorChange);
            appSettings.saveAll();
            if (mUVCCamera != null) {
                if (mAEStatus) {
                    int ret_enableAE = mUVCCamera.setEnableAE();
                    Log.i(TAG, "uvc camera enable AE ret:" + ret_enableAE);
                } else {
                    int ret_diableAE = mUVCCamera.setDisableAE();
                    Log.i(TAG, "uvc camera diable AE:" + ret_diableAE);
                }
                if (mAWBStatus) {
                    int ret_enableAWB = mUVCCamera.setAutoWhiteBalance(true);
                    Log.i(TAG, "uvc camera enable AWB ret:" + ret_enableAWB);
                } else {
                    int ret_diableAWB = mUVCCamera.setAutoWhiteBalance(false);
                    Log.i(TAG, "uvc camera diable AWB ret:" + ret_diableAWB);
                }
            } else {
                Log.i(TAG, "uvc camera do not connect");
                Toast.makeText(CameraMainActivity.this, "uvc camera do not connect", Toast.LENGTH_SHORT).show();
            }
        } else {
            return;
        }
    }


    private final SeekBar.OnSeekBarChangeListener mIRValueListener = new SeekBar.OnSeekBarChangeListener(){
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            Log.i(TAG,"seekbarprogress:"+progress);

            mIRValueCurrent = progress;
            setIRValue(mIRValueCurrent);
            setIRIcon();
        }
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            mText_IRValue.setText(progress + "/" + seekBar.getMax());
        }
    };

    private final OnClickListener mIROnClickListener = new OnClickListener() {
        public void onClick(View v) {

            if (mUVCCamera != null && mHasIR) {

                mIRSwitch = !mIRSwitch;
                if(mIRSwitch) {
                    if(mIRValueCurrent == 0) {
                        mIRValueCurrent = mIRValueDefault;
                    }
                    setIRValue(mIRValueCurrent);
                } else {
                    setIRValue(0);
                    if(mIRSeekBar.isShown()) {
                        mIRSeekBar.setVisibility(View.GONE);
                        mText_IRValue.setVisibility(View.GONE);
                    }
                }

                setIRIcon();
            }

        }
    };

    private final View.OnLongClickListener mIROnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {

            if (mUVCCamera != null && mHasIR && mIRSwitch) {

                if(mIRSeekBar.isShown()) {
                    mIRSeekBar.setVisibility(View.GONE);
                    mText_IRValue.setVisibility(View.GONE);
                    mText_IRValue.setOnClickListener(null);
                } else {
                    mIRSeekBar.setVisibility(View.VISIBLE);
                    mIRSeekBar.setProgress(mIRValueCurrent);
                    mText_IRValue.setText(mIRValueCurrent + "/" + mIRSeekBar.getMax());
                    mText_IRValue.setVisibility(View.VISIBLE);
                    mText_IRValue.setOnClickListener(mIRValueOnClickListener);
                }
            }

            return true;
        }
    };

    private final OnClickListener mIRValueOnClickListener = new OnClickListener() {
        public void onClick(View v) {

            if (mUVCCamera != null && mHasIR && mIRSwitch) {

                int IRMaxValue = getIRMaxValue();
                showSetIRMaxValueDialog(IRMaxValue);
            }

        }
    };

    private void showSetIRMaxValueDialog(final int max) {

        final LayoutInflater mLayoutInflater = LayoutInflater.from(this);
        final View mView = mLayoutInflater.inflate(R.layout.dialog, null);
        final EditText mEditTextMaxValue  = (EditText)mView.findViewById(R.id.editText_intent_event);
        mEditTextMaxValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(this);
        alert_builder.setView(mView);

        mEditTextMaxValue.setText(String.valueOf(mIRValueMax));

        alert_builder.setTitle("Change IR Max");
        alert_builder.setPositiveButton(getString(R.string.dlg_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final String valueStr = mEditTextMaxValue.getText().toString();
                if (!TextUtils.isEmpty(valueStr)) {

                    mIRValueMax = Integer.valueOf(valueStr);
                    if(mIRValueMax > max) {
                        mIRValueMax = max;
                        Toast.makeText(CameraMainActivity.this, "Value is large than device limit, the max value will change to " + max, Toast.LENGTH_SHORT);
                    }
                    mEditTextMaxValue.setText(String.valueOf(mIRValueMax));

                    if(mIRValueCurrent > mIRValueMax) {
                        mIRValueCurrent = mIRValueMax;
                        setIRValue(mIRValueCurrent);
                    }

                    mIRSeekBar.setMax(mIRValueMax);
                    mIRSeekBar.setProgress(mIRValueCurrent);
                }
                dialog.dismiss();
            }
        })
                .setNegativeButton(getString(R.string.dlg_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = alert_builder.create();
        alertDialog.show();
    }

    private void setIRValue(int value) {
        if (mUVCCamera != null) {
            mUVCCamera.setIRCurrentValue(value);
        }
    }

    private void setMeasure() {
        if (mShowMeasure) {
            mImageViewMeasureSpot.setVisibility(View.VISIBLE);
            mTextViewMeasure.setVisibility(View.VISIBLE);
        } else {
            mImageViewMeasureSpot.setVisibility(View.INVISIBLE);
            mTextViewMeasure.setVisibility(View.INVISIBLE);
        }

        setMeasureCallback();
    }

    private void monitorFrameRate() {
        if(mEnableStreamColor)mUVCCamera.setMonitorFrameRate(true,EtronCamera.CAMERA_COLOR);
        if(mEnableStreamDepth)mUVCCamera.setMonitorFrameRate(true,EtronCamera.CAMERA_DEPTH);
        EXECUTER.execute(mRunnableMonitorFrameRate);
    }

    private void setMeasureCallback() {

        if(mUVCCamera != null) {
            if (mShowMeasure) {
                mUVCCamera.setFrameCallback(mDepthMeasureIFrameCallback, EtronCamera.FRAME_FORMAT_YUYV, EtronCamera.CAMERA_DEPTH);
            } else {
                mUVCCamera.setFrameCallback(null, EtronCamera.FRAME_FORMAT_YUYV, EtronCamera.CAMERA_DEPTH);
            }
        }
    }

    private int getMeasureSpotIndex() {

        int index;

        int centerX = (mImageViewMeasureSpot.getLeft() + mImageViewMeasureSpot.getRight())/2;
        int centerY = (mImageViewMeasureSpot.getTop() + mImageViewMeasureSpot.getBottom())/2;

        int realX = centerX * Depth_W / ((View) mImageViewMeasureSpot.getParent()).getWidth();
        int realY = centerY * Depth_H / ((View) mImageViewMeasureSpot.getParent()).getHeight();

        index = realY*Depth_W + realX;

        return index;
    }


    private float mLastX, mLastY;
    private final View.OnTouchListener mMeasureSpotTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    mLastX = x;
                    mLastY = y;

                    break;

                case MotionEvent.ACTION_MOVE:
                    int offX = (int) (x - mLastX);
                    int offY = (int) (y - mLastY);

                    int left = v.getLeft() + offX;
                    int right = v.getRight() + offX;
                    int top = v.getTop() + offY;
                    int bottom = v.getBottom() + offY;

                    int centerX = (left+right)/2;
                    int centerY = (top+bottom)/2;

                    int parentMaxX = ((View) v.getParent()).getWidth();
                    int parentMaxY = ((View) v.getParent()).getHeight();

                    if(centerX >= 0 && centerX <= parentMaxX && centerY >= 0 && centerY <= parentMaxY) {
                        v.layout(v.getLeft() + offX, v.getTop() + offY, v.getRight() + offX, v.getBottom() + offY);
                    }

                    break;
            }

            return true;
        }
    };

    private final IFrameCallback mDepthMeasureIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame, int frameCount) {

            final int index = getMeasureSpotIndex();

            int dValue = 0;
            int zValue;
            final String depthInfo;

            if(mDepthDataType == EtronCamera.VideoMode.RECTIFY_14_BITS || mDepthDataType == EtronCamera.VideoMode.RAW_14_BITS) {

                zValue = (frame.get(index*2+1) & 0xff) * 256 + ((frame.get(index*2+0) & 0xff));
                depthInfo = "Depth z: " + zValue + " mm";
            } else {
                if(mZDBuffer == null) {
                    return;
                }

                if(mDepthDataType == EtronCamera.VideoMode.RECTIFY_11_BITS
                        || mDepthDataType == EtronCamera.VideoMode.RAW_11_BITS) {

                    dValue = (frame.get(index*2+1) & 0xff) * 256 + ((frame.get(index*2+0) & 0xff));
                }
                if(mDepthDataType == EtronCamera.VideoMode.RECTIFY_8_BITS_x80
                            || mDepthDataType == EtronCamera.VideoMode.RAW_8_BITS_x80) {

                    dValue = (frame.get(index * 2 + 1) & 0xff) * 256 + ((frame.get(index * 2 + 0) & 0xff));
                    dValue *= 8;
                }
                if (mDepthDataType == EtronCamera.VideoMode.RECTIFY_8_BITS
                        || mDepthDataType == EtronCamera.VideoMode.RAW_8_BITS) {

                    dValue = frame.get(index) & 0xff;
                    dValue *= 8;
                }

                zValue = mZDBuffer[dValue];
                depthInfo = "Depth z: " + zValue + " mm, d = " + dValue;
            }

            EtronCamera.CurrentFrameRate mCurrentFrameRate = mUVCCamera.getCurrentFrameRate(EtronCamera.CAMERA_COLOR);
            Log.i(TAG, "fps:"+mCurrentFrameRate.mFrameRateUvc);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextViewMeasure.setText(depthInfo);
                }
            });

        }
    };

    private void getZDTableValue() {

        if(mProductVersion.equals(PRODUCT_VERSION_EX8037) ||
                mProductVersion.equals(PRODUCT_VERSION_EX8036)){
            if(Depth_H >= 720) {
                mZDBuffer = mUVCCamera.getZDTableValue(0);
            } else if (Depth_H >= 480) {
                mZDBuffer = mUVCCamera.getZDTableValue(1);
            }
        } else if(mProductVersion.equals(PRODUCT_VERSION_EX8029)){
            if(Depth_H >= 480) {
                mZDBuffer = mUVCCamera.getZDTableValue(0);
            } else if (Depth_H >= 240) {
                mZDBuffer = mUVCCamera.getZDTableValue(1);
            }
        } else {
            mZDBuffer = mUVCCamera.getZDTableValue();
        }

    }


    private void resetIR() {

        mHasIR = false;
        mTextViewIR.setClickable(false);
        mTextViewIR.setTextColor(Color.GRAY);
        mTextViewIR.setAlpha(0.7f);
    }

    private int getIRMaxValue() {

        int value = -1;
        if (mUVCCamera != null) {
            value = mUVCCamera.getIRMaxValue();
            if(value == -1 && mProductVersion.equals(PRODUCT_VERSION_EX8029)) {
                value = 0x10;
            }
        }

        return value;
    }

    private void checkIRValue() {

        if (mUVCCamera != null) {

            int IRMaxValue = getIRMaxValue();

            switch(mProductVersion) {
                case PRODUCT_VERSION_EX8029:
                    mHasIR = true;
                    mIRValueDefault = IR_DEFAULT_VALUE_8029;
                    if(mIRValueMax == 0 || mIRValueMax > IRMaxValue) {
                        mIRValueMax = IR_DEFAULT_VALUE_8029_MAX;
                    }
                    break;

                case PRODUCT_VERSION_EX8036:
                    mHasIR = true;
                    mIRValueDefault = IR_DEFAULT_VALUE_8036;
                    if(mIRValueMax == 0 || mIRValueMax > IRMaxValue) {
                        mIRValueMax = IR_DEFAULT_VALUE_8036_MAX;
                    }
                    break;

                case PRODUCT_VERSION_EX8037:
                    mHasIR = true;
                    mIRValueDefault = IR_DEFAULT_VALUE_8037;
                    if(mIRValueMax == 0 || mIRValueMax > IRMaxValue) {
                        mIRValueMax = IR_DEFAULT_VALUE_8037_MAX;
                    }
                    break;

                default:
                    mHasIR = false;
            }

            if(mHasIR) {
                mIRValueCurrent = mUVCCamera.getIRCurrentValue();
            } else {
                mIRValueCurrent = 0x00;
            }
        }
    }
    private void setIRIcon() {

        if(mHasIR) {

            mTextViewIR.setClickable(true);
            mTextViewIR.setAlpha(1.0f);

            if (mIRSwitch) {
                mTextViewIR.setTextColor(Color.RED);
            } else {
                mTextViewIR.setTextColor(Color.BLACK);
            }

        } else {

            mTextViewIR.setClickable(false);
            mTextViewIR.setTextColor(Color.GRAY);
            mTextViewIR.setAlpha(0.7f);
        }
    }
}
