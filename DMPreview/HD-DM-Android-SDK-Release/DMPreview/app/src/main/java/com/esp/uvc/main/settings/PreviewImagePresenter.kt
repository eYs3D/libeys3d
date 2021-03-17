package com.esp.uvc.main.settings

import android.content.Context
import android.hardware.usb.UsbDevice
import android.widget.Toast
import com.esp.android.usb.camera.core.EtronCamera
import com.esp.android.usb.camera.core.StreamInfo
import com.esp.android.usb.camera.core.USBMonitor
import com.esp.android.usb.camera.core.UVCCamera
import com.esp.uvc.R
import com.esp.uvc.camera_modes.CameraModeManager
import com.esp.uvc.camera_modes.DepthRange
import com.esp.uvc.camera_modes.PresetMode
import com.esp.uvc.manager.LightSourceManager
import com.esp.uvc.usbcamera.AppSettings
import com.esp.uvc.roi_size.RoiSize
import com.esp.uvc.roi_size.RoiSizeProvider
import com.esp.uvc.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.inject

class PreviewImagePresenter(private val context: Context) : PreviewImageContract.Presenter {

    private var canNavigateFlag = true
    private val defaultDataTypeInManualMode = 1
    private var mUSBMonitor: USBMonitor? = null
    private var mUVCCamera: EtronCamera? = null
    private var mUsbDevice: UsbDevice? = null
    private var mSupportedSize: String? = null

    private var mEnableStreamColor = true
    private var mEnableStreamDepth = false

    private var mEtronIndex = 0
    private var mDepthIndex = 0
    private var mDepthDataType = defaultDataTypeInManualMode
    private var mColorFrameRate = 30
    private var mDepthFrameRate = 30
    private var mZFar = 1000 // mm

    private var mPostProcessPly = AppSettings.POST_PROCESS_PLY_DEFAULT
    private var mMonitorFrameRate = false

    private var mIROverride = false
    private var mLandscapeMode = false
    private var mUpsideDownMode = true //default as request by client
    private var mMirrorMode = false
    private var mInterleaveMode = false

    private var mPresets = ArrayList<PresetMode>()
    private var mCurrentPresetIndex = -1
    private var mIsUsingPresets = true

    private var mColorFpsOptions = ArrayList<Int>()
    private var mDepthFpsOptions = ArrayList<Int>()

    private var mProductVersion: String? = null

    // TODO use map instead
    private val allEntriesDepthDataType = arrayOf<CharSequence>(
        "COLOR_ONLY",                       // video mode 0
        "8_BITS",                           // video mode 1
        "14_BITS",                          // video mode 2
        "8_BITS_x80",                       // video mode 3
        "11_BITS",                          // video mode 4
        "OFF_RECTIFY",                      // video mode 5
        "8_BITS_RAW",                       // video mode 6
        "14_BITS_RAW",                      // video mode 7
        "8_BITS_x80_RAW",                   // video mode 8
        "11_BITS_RAW",                      // video mode 9
        "COLOR_ONLY_INTERLEAVE_MODE",       // video mode 16
        "8_BITS_INTERLEAVE_MODE",           // video mode 17
        "14_BITS_INTERLEAVE_MODE",          // video mode 18
        "8_BITS_x80_INTERLEAVE_MODE",       // video mode 19
        "11_BITS_INTERLEAVE_MODE",          // video mode 20
        "OFF_RECTIFY_INTERLEAVE_MODE",      // video mode 21
        "8_BITS_RAW_INTERLEAVE_MODE",       // video mode 22
        "14_BITS_RAW_INTERLEAVE_MODE",      // video mode 23
        "8_BITS_x80_RAW_INTERLEAVE_MODE",   // video mode 24
        "11_BITS_RAW_INTERLEAVE_MODE"       // video mode 25
    )
    private val mEntriesDepthDataType = allEntriesDepthDataType.toList() as ArrayList
    private var mEntriesColor: ArrayList<CharSequence>? = null
    private var mEntriesDepth: ArrayList<CharSequence>? = null

    private var mRoiSize = 0

    private var mDepthRange = DepthRange.DEPTH_RANGES[DepthRange.NEAR].distance

    private var irExtended = false
    private var irMin = 0
    private var irMax = 6
    private var irValue = 3

    override lateinit var view: PreviewImageContract.View

    private val roiSizeProvider: RoiSizeProvider by inject()

    override fun attach(view: PreviewImageContract.View) {
        this.view = view
        initUI()
    }

    override fun onStart() {
        mUSBMonitor = USBMonitor(context, mOnDeviceConnectListener)
        mUSBMonitor?.register()
        mUVCCamera?.destroy()
        mUVCCamera = null
        if (mUSBMonitor == null) {
            view.toast(getString(R.string.error_usb_failure), Toast.LENGTH_LONG)
        }
        readFromPreferences()
        initUI()
        usePreset(mIsUsingPresets)
        initIrSelection()
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onStop() {
        writeToPreferences()
        mUSBMonitor?.unregister()
        mUSBMonitor?.destroy()
        mUSBMonitor = null
        mUVCCamera?.destroy()
        mUVCCamera = null
    }

    private fun updateListEntries() {
        if (mUVCCamera != null) {
            mEntriesColor =
                listToEntries(mUVCCamera!!.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_COLOR))
            mEntriesDepth =
                listToEntries(mUVCCamera!!.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_DEPTH))
        } else {
            mEntriesColor = getEntriesFromSetting(UVCCamera.INTERFACE_NUMBER_COLOR)
            mEntriesDepth = getEntriesFromSetting(UVCCamera.INTERFACE_NUMBER_DEPTH)
        }
        view.updateUILists(mEntriesColor!!, mEntriesDepth!!, mEntriesDepthDataType)

        if (mEntriesColor!!.size == 0) {
            mEtronIndex = -1
        } else if (mEtronIndex >= mEntriesColor!!.size) {
            mEtronIndex = 0
        }

        if (mEntriesDepth!!.size == 0) {
            mDepthIndex = -1
        } else if (mDepthIndex >= mEntriesDepth!!.size) {
            mDepthIndex = 0
        }
    }

    private fun getDepthDataTypeIndex(): Int {
        val dataType = allEntriesDepthDataType[mDepthDataType]
        return mEntriesDepthDataType.indexOfFirst { it == dataType }
    }

    override fun onDepthDataTypeSelected(index: Int) {
        val depthType = mEntriesDepthDataType[index]
        mDepthDataType = allEntriesDepthDataType.indexOfFirst { it == depthType }
        if (mEntriesDepthDataType[index] == "COLOR_ONLY" ||
            mEntriesDepthDataType[index] == "OFF_RECTIFY" ||
            mEntriesDepthDataType[index] == "COLOR_ONLY_INTERLEAVE_MODE" ||
            mEntriesDepthDataType[index] == "OFF_RECTIFY_INTERLEAVE_MODE" ||
            (mIsUsingPresets && mPresets.isNotEmpty() && mPresets[mCurrentPresetIndex].dResolution == null)
        ) {
            view.enableDepthStreamCheckbox(false)
            mEnableStreamDepth = false
            view.enableDepthResolutionSelection(false)
            view.enableDepthFrameRateInputSelection(false)
            view.setDepthStreamSelection(mEnableStreamDepth)
        } else {
            if (!mIsUsingPresets) {
                view.enableDepthStreamCheckbox(true)
                view.enableDepthResolutionSelection(true)
            }
            view.enableDepthFrameRateInputSelection(true)
        }
    }

    override fun usePreset(usePreset: Boolean) {
        mIsUsingPresets = usePreset
        view.enablePresetSelection(mIsUsingPresets)
        view.enableManualColorFpsInput(!mIsUsingPresets)
        view.enableManualDepthFpsInput(!mIsUsingPresets)
        view.enableColorResolutionSelection(!mIsUsingPresets)
        view.enableDepthResolutionSelection(!mIsUsingPresets)

        if (!mIsUsingPresets) {
            val appSettings = AppSettings.getInstance(context)
            appSettings.put(AppSettings.KEY_PRESET_MODE, "")
            mEntriesDepthDataType.clear()
            view.enableDepthStreamCheckbox(allEntriesDepthDataType[mDepthDataType] != "COLOR_ONLY" && allEntriesDepthDataType[mDepthDataType] != "OFF_RECTIFY" && allEntriesDepthDataType[mDepthDataType] != "COLOR_ONLY_INTERLEAVE_MODE" && allEntriesDepthDataType[mDepthDataType] != "OFF_RECTIFY_INTERLEAVE_MODE")
            view.enableColorStreamCheckbox(true)
            mEntriesDepthDataType.addAll(allEntriesDepthDataType)
            view.updateDepthDataTypeList(mEntriesDepthDataType)
            view.updateUISelection(
                mEnableStreamColor,
                mEnableStreamDepth,
                mEtronIndex,
                mDepthIndex,
                getDepthDataTypeIndex(),
                mColorFrameRate,
                mDepthFrameRate,
                mPostProcessPly,
                mMonitorFrameRate,
                mZFar,
                mIROverride,
                mMirrorMode,
                mLandscapeMode,
                mUpsideDownMode,
                mRoiSize
            )
        } else onPresetSelected(mCurrentPresetIndex)
    }

    override fun canNavigate(): Boolean {
        return canNavigateFlag
    }

    override fun onPresetSelected(index: Int) {
        if (!mIsUsingPresets) return
        mCurrentPresetIndex = index
        if (mPresets.isEmpty()) return //Presets not yet read from file, nothing to do for now
        val preset = mPresets[index]
        logi("onPresetSelected : $preset")
        val appSettings = AppSettings.getInstance(context)
        appSettings.put(AppSettings.KEY_PRESET_MODE, preset.mode)
        mEnableStreamColor = preset.lResolution != null
        mEnableStreamDepth = preset.dResolution != null
        mColorFpsOptions =
            if (preset.colorFPS != null) preset.colorFPS as ArrayList<Int> else ArrayList()
        mDepthFpsOptions =
            if (preset.depthFPS != null) preset.depthFPS as ArrayList<Int> else ArrayList()
        view.updateFpsOptions(mColorFpsOptions.map { "$it ${if (it == preset.interLeaveModeFPS) " (interleave)" else ""}" } as ArrayList,
            mDepthFpsOptions.map { "$it ${if (it == preset.interLeaveModeFPS) " (interleave)" else ""}" } as ArrayList)
        val values = view.getViewValues()
        mColorFrameRate = values[5].toString().toInt()
        mDepthFrameRate = values[6].toString().toInt()
        mMonitorFrameRate = values[7].toString().toBoolean()
        mIROverride = values[9].toString().toBoolean()

        val colorIndex = mColorFpsOptions.indexOfFirst { it == mColorFrameRate }
        val depthIndex = mDepthFpsOptions.indexOfFirst { it == mDepthFrameRate }

        view.updateFpsSelection(colorIndex, depthIndex)

        populateDepthDataTypeList(preset)

        mEtronIndex = if (preset.lResolution == null) -1
        else mEntriesColor!!.indexOfFirst { it.toString() == preset.lResolution }

        mDepthIndex = if (preset.dResolution == null) -1
        else mEntriesDepth!!.indexOfFirst { it.toString() == preset.dResolution }

        view.updateDepthDataTypeList(mEntriesDepthDataType)

        view.updateUISelection(
            mEnableStreamColor,
            mEnableStreamDepth,
            mEtronIndex,
            mDepthIndex,
            getDepthDataTypeIndex(),
            mColorFrameRate,
            mDepthFrameRate,
            mPostProcessPly,
            mMonitorFrameRate,
            mZFar,
            mIROverride,
            mMirrorMode,
            mLandscapeMode,
            mUpsideDownMode,
            mRoiSize
        )

        view.enableDepthStreamCheckbox(false)
        view.enableColorStreamCheckbox(false)
    }

    //onColorFpsSelected and  onDepthFpsSelected are almost identical. They are not m,erged into one function because the lists of supported frame rates can be different
    override fun onColorFpsSelected(index: Int) {
        logi("onColorFpsSelection: $index")
        mColorFrameRate = mColorFpsOptions[index]
        mDepthFrameRate = mColorFrameRate
        view.updateFpsSelection(index, mDepthFpsOptions.indexOf(mDepthFrameRate))
        view.updateFpsValues(mColorFrameRate, mDepthFrameRate)
        val preset = mPresets[mCurrentPresetIndex]
        mInterleaveMode = preset.interLeaveModeFPS == mDepthFrameRate
    }

    override fun onDepthFpsSelected(index: Int) {
        logi("onDepthFpsSelection: $index")
        mDepthFrameRate = mDepthFpsOptions[index]
        mColorFrameRate = mDepthFrameRate
        view.updateFpsSelection(mColorFpsOptions.indexOf(mColorFrameRate), index)
        view.updateFpsValues(mColorFrameRate, mDepthFrameRate)
        val preset = mPresets[mCurrentPresetIndex]
        mInterleaveMode = preset.interLeaveModeFPS == mDepthFrameRate
    }

    private fun populateDepthDataTypeList(preset: PresetMode) {
        mEntriesDepthDataType.clear()
        if (preset.depthTypes == null) {
            if (preset.description.contains("interleave mode")) {
                if (preset.rectifyMode == 1) {
                    mEntriesDepthDataType.add("OFF_RECTIFY_INTERLEAVE_MODE")
                } else {
                    mEntriesDepthDataType.add("COLOR_ONLY_INTERLEAVE_MODE")
                }
            } else {
                if (preset.rectifyMode == 1) {
                    mEntriesDepthDataType.add("OFF_RECTIFY")
                } else {
                    mEntriesDepthDataType.add("COLOR_ONLY")
                }
            }
        } else {
            if (preset.description.contains("interleave mode")) {
                preset.depthTypes.forEach {
                    if (preset.rectifyMode == 1 || preset.lResolution == null) when (it) {
                        8 -> mEntriesDepthDataType.add("8_BITS_INTERLEAVE_MODE")
                        11 -> mEntriesDepthDataType.add("11_BITS_INTERLEAVE_MODE")
                        14 -> mEntriesDepthDataType.add("14_BITS_INTERLEAVE_MODE")
                    }
                    else when (it) {
                        8 -> mEntriesDepthDataType.add("8_BITS_RAW_INTERLEAVE_MODE")
                        11 -> mEntriesDepthDataType.add("11_BITS_RAW_INTERLEAVE_MODE")
                        14 -> mEntriesDepthDataType.add("14_BITS_RAW_INTERLEAVE_MODE")
                    }
                }
            } else {
                preset.depthTypes.forEach {
                    if (preset.rectifyMode == 1 || preset.lResolution == null) when (it) {
                        8 -> mEntriesDepthDataType.add("8_BITS")
                        11 -> mEntriesDepthDataType.add("11_BITS")
                        14 -> mEntriesDepthDataType.add("14_BITS")
                    }
                    else when (it) {
                        8 -> mEntriesDepthDataType.add("8_BITS_RAW")
                        11 -> mEntriesDepthDataType.add("11_BITS_RAW")
                        14 -> mEntriesDepthDataType.add("14_BITS_RAW")
                    }
                }
            }
        }
    }

    private fun readFromPreferences() {
        val appSettings = AppSettings.getInstance(context)
        mEnableStreamColor = appSettings.get(AppSettings.ENABLE_STREAM_COLOR, mEnableStreamColor)
        mEnableStreamDepth = appSettings.get(AppSettings.ENABLE_STREAM_DEPTH, mEnableStreamDepth)
        mEtronIndex = appSettings.get(AppSettings.ETRON_INDEX, mEtronIndex)
        mDepthIndex = appSettings.get(AppSettings.DEPTH_INDEX, mDepthIndex)
        mDepthDataType = appSettings.get(AppSettings.DEPTH_DATA_TYPE, mDepthDataType)
        mColorFrameRate = appSettings.get(AppSettings.COLOR_FRAME_RATE, mColorFrameRate)
        mDepthFrameRate = appSettings.get(AppSettings.DEPTH_FRAME_RATE, mDepthFrameRate)
        mSupportedSize = appSettings.get(AppSettings.SUPPORTED_SIZE, "")
        mPostProcessPly = appSettings.get(AppSettings.KEY_POST_PROCESS_PLY, mPostProcessPly)
        mMonitorFrameRate = appSettings.get(AppSettings.MONITOR_FRAMERATE, mMonitorFrameRate)
        mZFar = appSettings.get(AppSettings.Z_FAR, mZFar)
        mIROverride = appSettings.get(AppSettings.IR_OVERRIDE, mIROverride)

        mIsUsingPresets = appSettings.get(AppSettings.IS_USING_PRESET, mIsUsingPresets)
        mCurrentPresetIndex = appSettings.get(AppSettings.PRESET_NUMBER, mCurrentPresetIndex)

        mLandscapeMode = appSettings.get(AppSettings.LANDSCAPE_MODE, mLandscapeMode)
        mUpsideDownMode = appSettings.get(AppSettings.UPSIDEDOWN_MODE, mUpsideDownMode)
        mMirrorMode = appSettings.get(AppSettings.MIRROR_MODE, mMirrorMode)
        mInterleaveMode = appSettings.get(AppSettings.INTERLEAVE_FPS_CHOSEN, mInterleaveMode)

        mDepthRange = appSettings.get(AppSettings.DEPTH_RANGE, mDepthRange)

        mRoiSize = roiSizeProvider.getRoiSize().ordinal

        if (appSettings.get(AppSettings.CAMERA_VERSION, "").contains("YX8059")) {
            if (mDepthDataType > 9) {
                // 8059 PIF interleave mode is not an independent mode.
                // So 8059 UI (allEntriesDepthDataType index) need < 10 (real video mode -16 for UI)
                mDepthDataType -= 16
            }
        } else if (mDepthDataType > 9) {
            // Now, video mode is between 0-9, 16-25
            // -6 for UI (allEntriesDepthDataType index)
            mDepthDataType -= 6
        }
    }

    private fun applyDefaultValues() {
        mEnableStreamColor = true
        mEnableStreamDepth = false
        mEtronIndex = 0
        mDepthIndex = 0
        mDepthDataType = defaultDataTypeInManualMode
        mColorFrameRate = 30
        mDepthFrameRate = 30
        mSupportedSize = null
        mPostProcessPly = AppSettings.POST_PROCESS_PLY_DEFAULT
        mMonitorFrameRate = false
        mZFar = 1000
        mIROverride = false

        mIsUsingPresets = true
        mCurrentPresetIndex = -1
        mInterleaveMode = false

        mDepthRange = DepthRange.DEPTH_RANGES[DepthRange.NEAR].distance
    }

    private fun writeToPreferences() {

        val roiIndex = view.getRoiSizeValue()
        roiSizeProvider.storeRoiSize(RoiSize.values()[roiIndex])

        if (mUVCCamera == null) {
            return
        }

        val usbType =
            if (mUVCCamera!!.isUSB3) AppSettings.VALUE_USB_TYPE_3 else AppSettings.VALUE_USB_TYPE_2

        var (depthDataIndex, isColorStream, isDepthStream, colorStreamIndex, depthStreamIndex, colorFPS, depthFPS, showFps, zfar, irOverride, landscape, upsidedown, mirror, depthRange) = view.getViewValues()
        val appSettings = AppSettings.getInstance(context)

        appSettings.put(AppSettings.CAMERA_VERSION, mProductVersion)
        appSettings.put(AppSettings.KEY_USB_TYPE, usbType)

        appSettings.put(AppSettings.ENABLE_STREAM_COLOR, isColorStream)
        appSettings.put(AppSettings.ENABLE_STREAM_DEPTH, isDepthStream)

        appSettings.put(AppSettings.ETRON_INDEX, colorStreamIndex)
        appSettings.put(AppSettings.DEPTH_INDEX, depthStreamIndex)

        depthDataIndex =
            allEntriesDepthDataType.indexOfFirst { it.toString() == mEntriesDepthDataType[depthDataIndex as Int] }
        if (depthDataIndex > 9) {
            // Now, video mode is between 0-9, 16-25, but allEntriesDepthDataType is 0-19
            // +6 for real video mode
            depthDataIndex += 6
        }
        appSettings.put(AppSettings.DEPTH_DATA_TYPE, depthDataIndex)


        try {
            appSettings.put(AppSettings.COLOR_FRAME_RATE, colorFPS.toString().toInt())
        } catch (nfe: java.lang.NumberFormatException) {
            loge("Wrong number format of color frame rate")
        }
        try {
            appSettings.put(AppSettings.DEPTH_FRAME_RATE, depthFPS.toString().toInt())
        } catch (nfe: NumberFormatException) {
            loge("Wrong number format of depth frame rate.")
        }
        try {
            appSettings.put(AppSettings.Z_FAR, zfar.toString().toInt())
        } catch (nfe: java.lang.NumberFormatException) {
            loge("Wrong number format of Z far value")
        }

        appSettings.put(AppSettings.SUPPORTED_SIZE, mSupportedSize)
        appSettings.put(AppSettings.KEY_POST_PROCESS_PLY, mPostProcessPly)

        appSettings.put(AppSettings.IS_USING_PRESET, mIsUsingPresets)
        appSettings.put(AppSettings.PRESET_NUMBER, mCurrentPresetIndex)

        appSettings.put(AppSettings.MONITOR_FRAMERATE, showFps)
        appSettings.put(AppSettings.IR_OVERRIDE, irOverride)

        appSettings.put(AppSettings.LANDSCAPE_MODE, landscape)
        appSettings.put(AppSettings.UPSIDEDOWN_MODE, upsidedown)
        appSettings.put(AppSettings.MIRROR_MODE, mirror)

        if (mIsUsingPresets) {
            appSettings.put(AppSettings.INTERLEAVE_FPS_CHOSEN, mInterleaveMode)
        } else {
            mInterleaveMode = interleaveSupporting(
                colorStreamIndex as Int,
                colorFPS.toString().toInt(),
                depthStreamIndex as Int,
                depthFPS.toString().toInt()
            )
            appSettings.put(AppSettings.INTERLEAVE_FPS_CHOSEN, mInterleaveMode)
        }

        if (mInterleaveMode) {
            LightSourceManager.setSharedPrefs(
                LightSourceManager.INDEX_LOW_LIGHT_COMPENSATION,
                EtronCamera.LOW_LIGHT_COMPENSATION_OFF
            )
            // Cover the "different design" PIF (8059/8062)
            // 8062 interleave mode is an independent mode, but 8059 is not, so 8059 index is always < 10, +16 for real video mode
            if (mProductVersion!!.contains("YX8059")) {
                depthDataIndex += 16 // ex : COLOR_ONLY (0) +16 -> COLOR_ONLY_INTERLEAVE_MODE (16)
                appSettings.put(AppSettings.DEPTH_DATA_TYPE, depthDataIndex)
            }
        }
        appSettings.put(
            AppSettings.DEPTH_RANGE,
            DepthRange.DEPTH_RANGES[depthRange as Int].distance
        )

        appSettings.put(AppSettings.IR_VALUE, irValue)
        loge("[ir_ext] IR_EXTENDED @1 $irExtended")
        appSettings.put(AppSettings.IR_EXTENDED, irExtended)
        appSettings.put(AppSettings.IR_MIN, irMin)
        appSettings.put(AppSettings.IR_MAX, irMax)

        appSettings.saveAll()
    }

    private fun interleaveSupporting(
        colorStreamIndex: Int,
        colorFPS: Int,
        depthStreamIndex: Int,
        depthFPS: Int
    ): Boolean {
        if (CameraModeManager.isSupportedInterLeaveMode(
                context,
                mUVCCamera!!.pid,
                if (mUVCCamera!!.isUSB3) "3" else "2",
                mProductVersion!!.contains("_L")
            )
        ) {
            if (!mIsUsingPresets) {
                if (mUVCCamera != null) {
                    return CameraModeManager.isMatchInterLeaveMode(
                        context,
                        mUVCCamera!!.pid,
                        if (mUVCCamera!!.isUSB3) "3" else "2",
                        mEntriesColor?.get(colorStreamIndex).toString(),
                        colorFPS,
                        mEntriesDepth?.get(depthStreamIndex).toString(),
                        depthFPS,
                        mProductVersion!!.contains("_L")
                    )
                }
            }
        }
        return false
    }

    private fun getEntriesFromSetting(interfaceNumber: Int): ArrayList<CharSequence> {
        val list = EtronCamera.getSupportedSizeList(mSupportedSize, interfaceNumber)
        val entries = arrayListOf<CharSequence>()
        if (interfaceNumber == UVCCamera.INTERFACE_NUMBER_COLOR) {
            list.forEach {
                val type = if (it.type == 6) "MJPEG" else "YUYV"
                entries.add(String.format("%dx%d_%s", it.width, it.height, type))
            }
        } else {
            list.forEach {
                entries.add(String.format("%dx%d", it.width, it.height))
            }
        }
        return entries
    }

    private fun listToEntries(streamInfoList: Array<StreamInfo>): ArrayList<CharSequence> {
        val entries = arrayListOf<CharSequence>()
        streamInfoList.forEach {
            if (it.interfaceNumber == UVCCamera.INTERFACE_NUMBER_COLOR) {
                val type = if (it.bIsFormatMJPEG) "MJPEG" else "YUYV"
                entries.add(String.format("%dx%d_%s", it.width, it.height, type))
            } else {
                entries.add(String.format("%dx%d", it.width, it.height))
            }
        }
        return entries
    }

    private fun initUI() {
        updateListEntries()
        view.updateIrRange(irMin, irMax)
        view.enableColorResolutionSelection(!mIsUsingPresets)
        view.enableDepthResolutionSelection(!mIsUsingPresets)
        view.updateRoiValues(RoiSize.values().map { it.toString() }.toTypedArray())
        view.updateUsingPresetSelection(mIsUsingPresets)
        view.updateUISelection(
            mEnableStreamColor,
            mEnableStreamDepth,
            mEtronIndex,
            mDepthIndex,
            -1,
            mColorFrameRate,
            mDepthFrameRate,
            mPostProcessPly,
            mMonitorFrameRate,
            mZFar,
            mIROverride,
            mMirrorMode,
            mLandscapeMode,
            mUpsideDownMode,
            mRoiSize
        )
    }

    override fun onExtendedSelectionChanged(selected: Boolean) {
        irExtended = selected
        irMax = if (selected) 15 else 6
        view.updateIrRange(irMin, irMax)
        irValue = irValue.coerceIn(irMin, irMax)
        view.updateIrValue(irValue)
    }

    override fun onPostProcessChanged(enabled: Boolean) {
        mPostProcessPly = enabled
        AppSettings.getInstance(context).put(AppSettings.KEY_POST_PROCESS_PLY, mPostProcessPly)
        logi("esp_filter PLY filter : $mPostProcessPly")
    }

    override fun onIrValueChanged(value: Int) {
        irValue = value
    }

    fun initIrSelection() {
        when (mUVCCamera) {
            null -> {
                irMin = 0
                irMax = 6
                irExtended = false
                view.showExtendedCheckbox(true)
            }
            else -> {
                irMin = 0
                irExtended =
                    AppSettings.getInstance(context).get(AppSettings.IR_EXTENDED, irExtended)
                irMax = if (irExtended) 15 else 6
                irValue = AppSettings.getInstance(context).get(AppSettings.IR_VALUE, irValue)
                view.showExtendedCheckbox(true)
            }
        }
        irValue = irValue.coerceIn(irMin, irMax)
        view.updateExtendedSelection(irExtended)
        view.updateIrRange(irMin, irMax)
        view.updateIrValue(irValue)
    }

    private fun initPresets() {
        mIsUsingPresets =
            AppSettings.getInstance(context).get(AppSettings.IS_USING_PRESET, mIsUsingPresets)
        view.switchPresetMode(mIsUsingPresets)
        val pid = mUVCCamera?.pid ?: -1
        val usbType = if (mUVCCamera?.isUSB3!!) "3" else "2"
        mPresets =
            CameraModeManager.getPresetMode(context, pid, usbType, mProductVersion!!.contains("_L"))
                ?: ArrayList()
        view.enablePresetMode(mPresets.isNotEmpty())
        if (mCurrentPresetIndex == -1) {
            mCurrentPresetIndex = mPresets.indexOfFirst {
                it.mode.toInt() == CameraModeManager.getDefaultMode(
                    pid,
                    mUVCCamera?.isUSB3!!,
                    mProductVersion!!.contains("_L")
                )!!.mode
            }
            mDepthDataType = CameraModeManager.getDefaultMode(
                pid,
                mUVCCamera?.isUSB3!!,
                mProductVersion!!.contains("_L")
            )!!.videoMode
            if (mDepthDataType > 9) { // don't use interleave mode as default mode (For 8059 U2)
                mDepthDataType -= 16
            }
        }
        view.updatePresetList(mPresets.map { it.toString() } as ArrayList<String>,
            mCurrentPresetIndex)
        usePreset(mIsUsingPresets)
        if (mIsUsingPresets) onPresetSelected(mCurrentPresetIndex)
    }

    private fun initDepthRanges() {
        val depthRanges = CameraModeManager.getDepthRanges(mUVCCamera?.serialNumberValue ?: "")
        val depthOrdinal = DepthRange.DEPTH_RANGES.indexOfFirst { it.distance == mDepthRange }
        view.updateDepthRangeValues(depthRanges.map { it.toString() }.toTypedArray(), depthOrdinal)
    }

    private fun clearPresets() {
        mPresets.clear()
        view.updatePresetList(mPresets.map { it.toString() } as ArrayList<String>, -1)
        usePreset(false)
        mCurrentPresetIndex = -1
        view.updateUsingPresetSelection(mIsUsingPresets)
        view.enablePresetMode(false)
    }

    override fun unattach() {
        mUVCCamera?.destroy()
        mUVCCamera = null
        mUSBMonitor?.destroy()
        mUSBMonitor = null
    }

    private val mOnDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice?) {
            if (device != null && CameraModeManager.SUPPORTED_PID_LIST.contains(device.productId)) {
                mUSBMonitor!!.requestPermission(device)
            } else {
                mUSBMonitor!!.deviceList.forEach {
                    if (CameraModeManager.SUPPORTED_PID_LIST.contains(it!!.productId)) {
                        mUSBMonitor!!.requestPermission(it)
                    }
                }
            }
        }

        override fun onConnect(
            device: UsbDevice,
            ctrlBlock: USBMonitor.UsbControlBlock,
            createNew: Boolean
        ) {
            if (ctrlBlock.isIMU || mUVCCamera != null)
                return
            ::canNavigateFlag.toggleTimeout()
            mUVCCamera = EtronCamera()
            GlobalScope.launch {
                if (mUVCCamera!!.open(ctrlBlock) != EtronCamera.EYS_OK) {
                    view.toast(getString(R.string.camera_error_open_failed))
                    loge(
                        "Failed to open camera",
                        t = PreviewImagePresenter::javaClass.name
                    ) // else it is in the logs as StandAloneCoroutine
                } else {
                    mSupportedSize = mUVCCamera!!.supportedSize
                    mProductVersion = mUVCCamera!!.productVersion
                    if (mProductVersion!!.contains("8036")) {
                        val lowSwitch = IntArray(1)
                        mUVCCamera!!.getFWRegisterValue(lowSwitch, 0xE5)
                        if (lowSwitch[0] == 1) {
                            mProductVersion += "_L"
                        }
                    }
                    val usbType =
                        if (mUVCCamera!!.isUSB3) AppSettings.VALUE_USB_TYPE_3 else AppSettings.VALUE_USB_TYPE_2
                    val productVersionFromSavedState =
                        AppSettings.getInstance(context).get(AppSettings.CAMERA_VERSION, "")
                    val usbTypeFromSavedState =
                        AppSettings.getInstance(context).get(AppSettings.KEY_USB_TYPE, "")
                    if (mProductVersion != productVersionFromSavedState || usbType != usbTypeFromSavedState) {
                        loge("settings were saved for different camera module, setting default values")
                        AppSettings.getInstance(context).clear()
                        applyDefaultValues()
                    }
                    initUI()
                    initPresets()
                    initDepthRanges()
                    initIrSelection()
                }
            }
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            if (mUVCCamera != null && device == mUVCCamera?.device) {
                mUVCCamera?.close()
                mUVCCamera?.destroy()
                mUVCCamera = null
            }
            canNavigateFlag = true
        }

        override fun onDetach(device: UsbDevice) {
            view.toast(getString(R.string.usb_device_detached))
            mEntriesColor?.clear()
            mEntriesDepth?.clear()
            view.updateUILists(mEntriesColor!!, mEntriesDepth!!, mEntriesDepthDataType)
            clearPresets()
            initDepthRanges()
            initIrSelection()
        }

        override fun onCancel() = loge("OnCancel")
    }

    private fun getString(resourceID: Int): String {
        return context.getString(resourceID)
    }
}
