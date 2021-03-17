//
// Created by alanlin on 2020-10-16.
//

#ifndef EYS3D_UNITY_WRAPPER_GENERALWRAPPER_H
#define EYS3D_UNITY_WRAPPER_GENERALWRAPPER_H
#include "eSPDI.h"
#include "ColorPaletteGenerator.h"

#ifndef RETRY_ETRON_API
#define RETRY_COUNT (5)
#define RETRY_ETRON_API(ret, func) \
    do{ \
        int retryCount = RETRY_COUNT; \
        while (retryCount > 0){ \
            ret = func; \
            if (ETronDI_OK == ret) break; \
            --retryCount; \
        } \
    }while (false) \

#endif
#define DEPTH_PIXEL_BYTE_D11_Z14 (2)
#define DEPTH_PIXEL_BYTE_D8 (1)
#define VALIDATE_NOT_NULL(ARG) if(!(ARG)) throw std::runtime_error("null pointer passed for argument \"" #ARG "\"");
#define VALIDATE_NULL(ARG) if((ARG)) throw std::runtime_error("non null pointer passed for argument \"" #ARG "\"");
#define BEGIN_API_CALL try
#define HANDLE_EXCEPTIONS_AND_RETURN(R, ...) catch(...) { LOGE(__FUNCTION__); return R; }
#define NOARGS_HANDLE_EXCEPTIONS_AND_RETURN(R) catch(...) { printf("Exception at %s", __FUNCTION__);return R; }

inline bool is8bits(int depthDataType);
inline size_t zdTableBytes();
int fillZDIndexByProductInfos(unsigned short pid, int depthHeight,
                              int colorHeight, bool isUSB3);
extern "C" {
int convert_yuv_to_rgb_pixel(int y, int u, int v);
int convert_yuv_to_rgb_buffer(unsigned char *yuv, unsigned char *rgb, unsigned int width, unsigned int height);
int save_file(unsigned char *buf, int size, int width, int height, int type, bool saveRawData);
int get_product_name(char *path, char *out);
void print_etron_error(int error);
static int error_msg(int error);
static void setupDepth(void);
static void *pfunc_thread_close(void *arg);
static long long calcByGetTimeOfDay();
/**
 *
 * @param dataInOut Buffer ready to fill in depth data.
 * @param bufInSize Buffer allocated bytes count.
 * @param type 0 is RGB color palette 3 bytes per px, 1 is raw data 8(1byte) 11(2bytes) 14(2bytes)
 * @return 0 if success
 */
int get_depth_frame(BYTE* dataInOut, size_t bufInSize, int type);
int get_usb_type();
bool regenerate_palette(unsigned short zMin, unsigned short zFar);
void reset_palette();
unsigned short getDefaultZFar();
unsigned short getDefaultZNear();
int tjpeg2yuv(unsigned char *jpeg_buffer, int jpeg_size, unsigned char **yuv_buffer, int *yuv_size, int *yuv_type);
int tyuv2rgb(unsigned char *yuv_buffer, int yuv_size, int width, int height, int subsample, unsigned char **rgb_buffer, int *rgb_size);
int getZDtable(DEVINFORMATION *pDevInfo, DEVSELINFO DevSelInfo, int depthHeight, int colorHeight, int usbType);
void saveDepth2rgb(unsigned char *m_pDepthImgBuf, unsigned char *m_pRGBBuf, unsigned int m_nImageWidth, unsigned int m_nImageHeight);
void setHypatiaVideoMode(int mode);
int setupIR(unsigned short value);
void UpdateD8bitsDisplayImage_DIB24(RGBQUAD *pColorPalette, unsigned char *pDepth, unsigned char *pRGB, int width, int height);
void UpdateD11DisplayImage_DIB24(const RGBQUAD *pColorPalette, const unsigned char *pDepth, unsigned char *pRGB, int width, int height);
void UpdateZ14DisplayImage_DIB24(RGBQUAD *pColorPaletteZ14, unsigned char *pDepthZ14, unsigned char *pDepthDIB24, int cx, int cy);

typedef struct {
    int colorFormat;
    int colorWidth;
    int colorHeight;
    int fps;
    int depthWidth;
    int depthHeight;
    int videoMode;
} camera_open_config;

int init_device(void);
int open_device(camera_open_config config);
int set_depth_datatype(int DepthDataType);
/**
 * @param imageInOut
 * @return 0 if success.
 */
int get_color_frame(BYTE* imageInOut);
void close_device(void);
void release_device(void);

void set_FW_register(unsigned char, unsigned char);
unsigned char get_FW_register(unsigned char addr);
int get_cached_zd_table(unsigned short* buffer, size_t bufLen);
WORD get_depth_by_coordinate(int, int);

int set_IR_max_value(unsigned short m_nIRmax);
unsigned char get_IR_max_value();
int set_IR_min_value(unsigned short m_nIRmin);
unsigned char get_IR_min_value();
int set_current_IR_value(unsigned short m_nIRvalue);
unsigned short get_current_IR_value();

int enable_AE();
int disable_AE();
int enable_AWB();
int disable_AWB();
int get_AE_status();
int get_AWB_status();

ushort get_current_depth_bytes();

void* GetEYs3D(void);
DEVSELINFO GetDevSelInfo(void);
int GetZNear();
int GetZFar();
unsigned short GetDepthDataType(void);
eSPCtrl_RectLogData &GetRectifyLogData();
float GetColorImageDataHeight();

#ifdef PYTHON_BUILD
py::dict get_rectify_mat_log_data(int nRectifyLogIndex)
#else
eSPCtrl_RectLogData get_rectify_mat_log_data(int nRectifyLogIndex);
#endif

int eys3d_camera_native_create();
int eys3d_camera_native_release();

int eys3d_camera_open(camera_open_config cfg, bool isDepthRaw);
int eys3d_camera_close();

int eys3d_camera_start_stream();
int eys3d_camera_stop_stream();

bool eys3d_camera_dequeue_color_frame(BYTE** frameOut);
bool eys3d_camera_dequeue_depth_frame(BYTE** frameOut);

int eys3d_camera_recycle_color_frame(BYTE** frame_unused);
int eys3d_camera_recycle_depth_frame(BYTE** frame_unused);

/**
 * C sharp version generate point cloud by GPU
 * @param colorData
 * @param depthData
 * @param colorOut
 * @param colorCapacity the capacity bytes of colorOut
 * @param depthOut
 * @param depthCapacity the capacity bytes of depthOut
 * @return status code 0 if compute successfully.
 */
int generate_point_cloud_gpu(unsigned char *colorData, unsigned char *depthData,
                             unsigned char *colorOut, int* colorCapacity, float* depthOut, int* depthCapacity);
camera_open_config get_current_config();

}// end of extern "C"
#endif //EYS3D_UNITY_WRAPPER_GENERALWRAPPER_H
