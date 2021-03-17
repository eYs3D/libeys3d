package com.esp.uvc.main.common;

import com.esp.android.usb.camera.core.EtronCamera;

import java.nio.ByteBuffer;

import kotlin.Pair;

public class JavaCameraUtils {

    /**
     * Max depth as provided by eYs3d was (1<<14)
     * However modified to approximatelyu 8 meters (1<<13)
     */
    private static final int MAX_8038_DEPTH = 8192;

    /**
     * @param frame
     * @param index
     * @param depthDataType
     * @param mZDBuffer
     * @return pair of (zValue, dValue)
     */
    public static Pair<Integer, Integer> calculateDepth(ByteBuffer frame, Integer index, Integer depthDataType, int[] mZDBuffer) {
        int dValue = 0;
        int zValue;
        final String depthInfo;

        if (depthDataType == EtronCamera.VideoMode.RECTIFY_14_BITS || depthDataType == EtronCamera.VideoMode.RAW_14_BITS ||
                depthDataType == EtronCamera.VideoMode.RECTIFY_14_BITS_INTERLEAVE_MODE || depthDataType == EtronCamera.VideoMode.RAW_14_BITS_INTERLEAVE_MODE) {
            zValue = (frame.get(index * 2 + 1) & 0xff) * 256 + ((frame.get(index * 2 + 0) & 0xff));
        } else {
            if (mZDBuffer == null) {
                return new Pair<>(0, 0);
            }

            if (depthDataType == EtronCamera.VideoMode.RECTIFY_11_BITS || depthDataType == EtronCamera.VideoMode.RAW_11_BITS ||
                    depthDataType == EtronCamera.VideoMode.RECTIFY_11_BITS_INTERLEAVE_MODE || depthDataType == EtronCamera.VideoMode.RAW_11_BITS_INTERLEAVE_MODE) {
                dValue = (frame.get(index * 2 + 1) & 0xff) * 256 + ((frame.get(index * 2 + 0) & 0xff));
            }
            if (depthDataType == EtronCamera.VideoMode.RECTIFY_8_BITS_x80 || depthDataType == EtronCamera.VideoMode.RAW_8_BITS_x80 ||
                    depthDataType == EtronCamera.VideoMode.RECTIFY_8_BITS_x80_INTERLEAVE_MODE || depthDataType == EtronCamera.VideoMode.RAW_8_BITS_x80_INTERLEAVE_MODE) {
                dValue = (frame.get(index * 2 + 1) & 0xff) * 256 + ((frame.get(index * 2 + 0) & 0xff));
                dValue *= 8;
            }
            if (depthDataType == EtronCamera.VideoMode.RECTIFY_8_BITS || depthDataType == EtronCamera.VideoMode.RAW_8_BITS ||
                    depthDataType == EtronCamera.VideoMode.RECTIFY_8_BITS_INTERLEAVE_MODE || depthDataType == EtronCamera.VideoMode.RAW_8_BITS_INTERLEAVE_MODE) {
                dValue = frame.get(index) & 0xff;
                dValue *= 8;
            }

            zValue = mZDBuffer[dValue];
        }
        return new Pair(zValue, dValue);
    }

    /**
     * @param frame
     * @param index
     * @param depthDataType
     * @param baseLineDistance
     * @return pair of (zValue, dValue)
     */
    public static Pair<Integer, Integer> calculate8038Depth(ByteBuffer frame, Integer index, Integer depthDataType, Integer baseLineDistance) {
        int dValue;
        int mCamFocus = 800;
        int depth = 0;

        switch (depthDataType.shortValue()) {
            case EtronCamera.VideoMode.RECTIFY_8_BITS_x80:
            case EtronCamera.VideoMode.RAW_8_BITS_x80:
            case EtronCamera.VideoMode.RECTIFY_8_BITS_x80_INTERLEAVE_MODE:
            case EtronCamera.VideoMode.RAW_8_BITS_x80_INTERLEAVE_MODE:
                dValue = (frame.get(index * 2 + 1) & 0xff) * 256 + ((frame.get(index * 2 + 0) & 0xff));
                dValue *= 8;
                break;
            case EtronCamera.VideoMode.RECTIFY_8_BITS:
            case EtronCamera.VideoMode.RAW_8_BITS:
            case EtronCamera.VideoMode.RECTIFY_8_BITS_INTERLEAVE_MODE:
            case EtronCamera.VideoMode.RAW_8_BITS_INTERLEAVE_MODE:
                dValue = frame.get(index) & 0xff;
                dValue *= 8;
                break;
            case EtronCamera.VideoMode.RECTIFY_11_BITS:
            case EtronCamera.VideoMode.RAW_11_BITS:
            case EtronCamera.VideoMode.RECTIFY_11_BITS_INTERLEAVE_MODE:
            case EtronCamera.VideoMode.RAW_11_BITS_INTERLEAVE_MODE:
                dValue = (frame.get(index * 2 + 1) & 0xff) * 256 + ((frame.get(index * 2 + 0) & 0xff));
                break;
            default:
                dValue = 0;
        }

        if (dValue != 0.0) {
            depth = (mCamFocus * baseLineDistance * 10) / dValue;
        }

        if (depth > MAX_8038_DEPTH) {
            depth = 0;
        }

        return new Pair(depth, dValue);
    }
}
