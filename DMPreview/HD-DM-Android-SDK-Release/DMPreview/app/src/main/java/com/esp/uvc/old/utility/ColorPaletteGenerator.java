package com.esp.uvc.old.utility;

import android.util.Log;

/**
 * Created by Ivan.Huang on 2018/11/13.
 */

public class ColorPaletteGenerator {

    static float maxHueAngle = 270.0f;
    static int defaultAlpha = 255;
    static String TAG = "ColorPaletteGenerator";

    public static class Color{
        int r;
        int g;
        int b;
        Color(int R,int G,int B){
            r = R;
            g = G;
            b = B;
        }
    }
    public static void generatePalette(byte [] palette, int paletteSize, boolean reverseRedToBlue) {

        generatePalette(palette, paletteSize, 0, paletteSize,reverseRedToBlue);
    }

    public static void generatePalette(byte [] palette, int paletteSize, int controlPoint1, int controlPoint2, boolean reverseRedToBlue) {
        generatePalette(palette, paletteSize, controlPoint1, 0.1f, controlPoint2,0.9f,reverseRedToBlue);
    }

    public static void generatePalette(byte [] palette, int paletteSize, int controlPoint1, float controlPoint1Value, int controlPoint2, float controlPoint2Value, boolean reverseRedToBlue){

        float step;
        int count = 0;

        if(controlPoint1 > controlPoint2 )controlPoint1 = controlPoint2;

        step = (controlPoint1Value - 0.0f) / (float)(controlPoint1 - 0);
        for (int i = 0; i < controlPoint1; i++) {
            Color rgb = getRGB(step * i, reverseRedToBlue);
            palette[count * 4 + 0] = (byte)rgb.r;
            palette[count * 4 + 1] = (byte)rgb.g;
            palette[count * 4 + 2] = (byte)rgb.b;
            palette[count * 4 + 3] = (byte)defaultAlpha;
            //Log.i(TAG,String.format("0colorPalette[%d]:%3d,%3d,%3d",count,palette[i*4+0],palette[i*4+1],palette[i*4+2]));
            count++;
            if(count >= paletteSize)break;
        }

        step = (controlPoint2Value - controlPoint1Value) / (float)(controlPoint2 - controlPoint1);
        for (int i = 0; i< controlPoint2 - controlPoint1; i++) {
            Color rgb = getRGB(step * i + controlPoint1Value, reverseRedToBlue);
            palette[count * 4 + 0] = (byte)rgb.r;
            palette[count * 4 + 1] = (byte)rgb.g;
            palette[count * 4 + 2] = (byte)rgb.b;
            palette[count * 4 + 3] = (byte)defaultAlpha;
            //Log.i(TAG,String.format("1colorPalette[%d]:%3d,%3d,%3d",count,palette[i*4+0],palette[i*4+1],palette[i*4+2]));
            count++;
            if(count >= paletteSize)break;
        }

        step = (1.0f - controlPoint2Value) / (float)(paletteSize - controlPoint2);
        for (int i = 0; i < paletteSize - controlPoint2; i++) {
            Color rgb =getRGB(step * i + controlPoint2Value, reverseRedToBlue);
            palette[count * 4 + 0] = (byte)rgb.r;
            palette[count * 4 + 1] = (byte)rgb.g;
            palette[count * 4 + 2] = (byte)rgb.b;
            palette[count * 4 + 3] = (byte)defaultAlpha;
            //Log.i(TAG,String.format("2colorPalette[%d]:%3d,%3d,%3d",count,palette[i*4+0],palette[i*4+1],palette[i*4+2]));
            count++;
            if(count >= paletteSize)break;
        }

        palette[0] = 0;
        palette[1] = 0;
        palette[2] = 0;
        palette[3] = (byte)defaultAlpha;

        //palette[(paletteSize - 1)*4 + 0] = 255;
        //palette[(paletteSize - 1)*4 + 1] = 255;
        //palette[(paletteSize - 1)*4 + 2] = 255;
        //palette[(paletteSize - 1)*4 + 3] = (byte)defaultAlpha;
    }
    public static void generatePaletteGray(byte [] palette, int paletteSize, boolean reverseGraylevel) {

        generatePalette(palette, paletteSize, 0, paletteSize,reverseGraylevel);
    }

    public static void generatePaletteGray(byte [] palette, int paletteSize, int controlPoint1, int controlPoint2, boolean reverseGraylevel) {
        generatePalette(palette, paletteSize, controlPoint1, 0.1f, controlPoint2, 0.9f,reverseGraylevel);
    }

    public static void generatePaletteGray(byte [] palette, int paletteSize, int controlPoint1, float controlPoint1Value, int controlPoint2, float controlPoint2Value, boolean reverseGraylevel ) {
        if (controlPoint1 > controlPoint2)controlPoint1 = controlPoint2;
        float step;
        step = (controlPoint1Value - 0.0f) / (float)(controlPoint1 - 0);

        int count = 0;
        for (int i = 0; i < controlPoint1; i++) {
            int grayValue = (int) ( 255 * (reverseGraylevel ? 1.0f - (step*i) : (step*i)) );
            palette[count * 4 + 0] = (byte)grayValue;
            palette[count * 4 + 1] = (byte)grayValue;
            palette[count * 4 + 2] = (byte)grayValue;
            palette[count * 4 + 3] = (byte)defaultAlpha;
            count++;
        }

        step = (controlPoint2Value - controlPoint1Value) / (float)(controlPoint2 - controlPoint1);
        for (int i = 0; i< controlPoint2 - controlPoint1; i++) {
            int grayValue = (int) (255 * (reverseGraylevel ? 1.0f - ((step * i + controlPoint1Value)) : ((step * i + controlPoint1Value))));
            palette[count * 4 + 0] = (byte)grayValue;
            palette[count * 4 + 1] = (byte)grayValue;
            palette[count * 4 + 2] = (byte)grayValue;
            palette[count * 4 + 3] = (byte)defaultAlpha;
            count++;
        }

        step = (1.0f - controlPoint2Value) / (float)(paletteSize - controlPoint2);
        for (int i = 0; i < paletteSize - controlPoint2; i++) {
            int grayValue =  (int) ( 255 * (reverseGraylevel ? 1.0f - ((step * i + controlPoint2Value)) : ((step * i + controlPoint2Value))) );
            palette[count * 4 + 0] = (byte)grayValue;
            palette[count * 4 + 1] = (byte)grayValue;
            palette[count * 4 + 2] = (byte)grayValue;
            palette[count * 4 + 3] = (byte)defaultAlpha;
            count++;
        }

        palette[0] = 0;
        palette[1] = 0;
        palette[2] = 0;
        palette[3] = (byte)defaultAlpha;
        //palette[(paletteSize - 1)*4 + 0] = 255;
        //palette[(paletteSize - 1)*4 + 1] = 255;
        //palette[(paletteSize - 1)*4 + 2] = 255;
        //palette[(paletteSize - 1)*4 + 3] = (byte)defaultAlpha;
    }
    static Color getRGB(float value,  boolean reverseRedToBlue ) {
        if (reverseRedToBlue) value = 1.0f - value;
        return HSV_to_RGB(value * maxHueAngle, 1.0f,1.0f);
    }

    static Color HSV_to_RGB(float H, float S, float V){
        int R,G,B;
        float nMax, nMin;
        float fDet;
        //
        while (H<0.0) H += 360.0f;
        while (H >= 360.0) H -= 360.0f;
        H /= 60.0;
        if (V<0.0) V = 0.0f;
        if (V>1.0) V = 1.0f;
        V *= 255.0;
        if (S<0.0) S = 0.0f;
        if (S>1.0) S = 1.0f;
        //
        if (V == 0.0) {
            R = G = B = 0;
        }
        else {
            fDet = S * V;
            nMax = (V);
            nMin = (V - fDet);
            if (H <= 1.0) { //R>=G>=B, H=(G-B)/fDet
                R = (int)nMax;
                B = (int)nMin;
                G = (int)(H*fDet + B);
            }
            else if (H <= 2.0) { //G>=R>=B, H=2+(B-R)/fDet
                G = (int)nMax;
                B = (int)nMin;
                R = (int)((2.0 - H)*fDet + B);
            }
            else if (H <= 3.0) { //G>=B>=R, H=2+(B-R)/fDet
                G = (int)nMax;
                R = (int)nMin;
                B = (int)((H - 2.0)*fDet + R);
            }
            else if (H <= 4.0) { //B>=G>=R, H=4+(R-G)/fDet
                B = (int)nMax;
                R = (int)nMin;
                G = (int)((4.0 - H)*fDet + R);
            }
            else if (H <= 5.0) { //B>=R>=G, H=4+(R-G)/fDet
                B = (int)nMax;
                G = (int)nMin;
                R = (int)((H - 4.0)*fDet + G);
            }
            else { // if(H<6.0) //R>=B>=G, H=(G-B)/fDet+6
                R = (int)nMax;
                G = (int)nMin;
                B = (int)((6.0 - H)*fDet + G);
            }
        }
        return new Color(R,G,B);
    }
}
