package com.esp.uvc.old.utility;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class PlyWriter {
    private static String TAG = "PlyWriter";
    public static class CloudPoint {
        CloudPoint(){}
        CloudPoint(float x,float y,float z,int r,int g ,int b){

        }
        float x;
        float y;
        float z;
        int r;
        int g;
        int b;
    }

    public static ArrayList<CloudPoint> etronFrameTo3D(int width, int height,  byte[] colorArray,byte[] depthArray, float reprojectMat[], int depthBytes,  boolean clipping, float zNear, float zFar, boolean removeINF){
        float centerX = -1.0f*reprojectMat[3];
        float centerY = -1.0f*reprojectMat[7];
        float focalLength = reprojectMat[11];
        float baseline = 1.0f / reprojectMat[14];
        ArrayList<CloudPoint> output = new ArrayList<>();
        output.clear();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float disparity = 0;
                switch (depthBytes)
                {
                    case 1:
                        disparity = 1.0f * (0xff & depthArray[(j*width + i)] );
                        break;
                    case 2:
                        disparity =((depthArray[(j*width + i) * 2 + 1] << 8) +
                                    (depthArray[(j*width + i) * 2 + 0] << 0) )/ 8.0f;
                        break;
                }
                float W = disparity / baseline;
                float X = (i*1.0f - centerX) / W;
                float Y = (j*1.0f - centerY) / W;
                float Z = focalLength / W;

                if (clipping) {
                    if (Z > zFar || Z < zNear) {
                        continue;
                    }
                }
                if (removeINF) {
                    if (W == 0.0f) {
                        continue;
                    }
                }

                CloudPoint point = new CloudPoint();
                point.r = colorArray[(j*width + i) * 4 + 0];
                point.g = colorArray[(j*width + i) * 4 + 1];
                point.b = colorArray[(j*width + i) * 4 + 2];
               //point.a=colorArray[(j*width + i) * 4 + 3];
                point.x = X;
                point.y = Y;
                point.z = Z;

                output.add(point);
                //output[(j*width + i)*3 + 0] = X;
                //output[(j*width + i)*3 + 1] = Y;
                //output[(j*width + i)*3 + 2] = Z;
            }
        }

        return  output;
    }

    public static int writePly(ArrayList<CloudPoint> cloud, File file) {
        try{

            FileOutputStream output = new FileOutputStream(file);
            // Open file

            int vertexSize = cloud.size();

            output.write(generateHeader(false, vertexSize).getBytes());
	        /*Output point*/

            for (int i = 0; i < vertexSize; i++) {

                String point = String.format("%d %d %d %f %f %f \n",
                        (int)0xff & cloud.get(i).r,
                        (int)0xff & cloud.get(i).g,
                        (int)0xff & cloud.get(i).b,
                        cloud.get(i).x,
                        cloud.get(i).y,
                        cloud.get(i).z);

                output.write(point.getBytes());
            }
            output.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        return 0;
    }

    public static String generateHeader(boolean binary,int size)
    {
        StringBuilder output = new StringBuilder();
        boolean endian = true;

        // Begin header
        output.append("ply");
        if (!binary)
            output.append("\nformat ascii 1.0");
        else
        {
            if (endian)
                output.append("\nformat binary_big_endian 1.0");
            else
                output.append("\nformat binary_little_endian 1.0");
        }

        output.append( "\ncomment no face");

        output.append("\nelement vertex " + size);

        output.append("\nproperty uchar red");
        output.append("\nproperty uchar green");
        output.append("\nproperty uchar blue");

        output.append("\nproperty float x");
        output.append("\nproperty float y");
        output.append("\nproperty float z");
        // End header
        output.append( "\nend_header\n");
        return (output.toString());
    }

}
