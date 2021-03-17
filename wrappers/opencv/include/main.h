//
// Created by terry on 2020-11-17.
//
#include <string>

#ifndef EYS3D_OPENCV_MAIN_H
#define EYS3D_OPENCV_MAIN_H
using namespace std;

const string VERSION="2021010401";
const string FACE_CASCADE = "haarcascades/haarcascade_frontalface_alt.xml";
const string EYES_CASCADE = "haarcascades/haarcascade_eye_tree_eyeglasses.xml";
const string NOSE_CASCADE = "asset/haarcascade_mcs_nose.xml";
const string MOUTH_CASCADE = "asset/haarcascade_mcs_mouth.xml";
const string SETTINGS_IR = "IR settings";
const string SETTINGS_AE = "AE settings";
const string SETTINGS_AWB = "AWB settings";


struct window_display {
    const char *id;
    int width;
    int height;
    int x;
    int y;
};

//method
void show_menu();
void preview_color_depth();
void preview_all();
void face_detect();
void face_mask_detect();
void show_settings_content(window_display &settings_wd);
void point_cloud_view();
void point_cloud_view_with_opengl();

#endif //EYS3D_OPENCV_MAIN_H
