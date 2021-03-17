#include "eSPDI_def.h"
#include "PlyWriter.h"

#ifdef PYTHON_BUILD
#include <pybind11/pybind11.h>
#include <pybind11/numpy.h>
namespace py=pybind11;
#endif //--PYTHON_BUILD

enum STATE{
    CLOSED,
    OPENED,
    STREAMING,
    RECONNECTING,
    RECONNECTED
};

enum STREAM_TYPE{
    STREAM_COLOR = 0,
    STREAM_KOLOR,
    STREAM_TRACK,
    STREAM_DEPTH,
    STREAM_DEPTH_FUSION,
    STREAM_DEPTH_30mm,
    STREAM_DEPTH_60mm,
    STREAM_DEPTH_150mm,
    STREAM_RESERVED,
    STREAM_TYPE_COUNT
};

enum IMAGE_TYPE{
    IMAGE_COLOR,
    IMAGE_DEPTH,
    IMAGE_DEPTH_FUSION
};

enum DEPTH_DATA_TYPE{
    DEPTH_DATA_8BIT,
    DEPTH_DATA_11BIT,
    DEPTH_DATA_14BIT
};

struct DeviceInfo{
    DEVINFORMATION deviceInfomation;
    std::string sFWVersion;
    std::string sSerialNumber;
    std::string sBusInfo;
    std::string sModelName;
};

struct ZDTableInfo{
    unsigned short nIndex = 0;
    unsigned short nTableSize = ETronDI_ZD_TABLE_FILE_SIZE_11_BITS;
    unsigned short nZNear = 0;
    unsigned short nZFar  = 0;
    BYTE ZDTable[ETronDI_ZD_TABLE_FILE_SIZE_11_BITS] = {0};
};

struct ImageData{
    int nWidth = EOF;
    int nHeight = EOF;
    bool bMJPG = false;
    int depthDataType = EOF;
    EtronDIImageType::Value imageDataType = EtronDIImageType::IMAGE_UNKNOWN;
    std::vector<BYTE> imageBuffer;
};

#ifdef PYTHON_BUILD
std::vector<CloudPoint> PyGeneratePointCloud_GPU(
        py::array_t<unsigned char> depthData, unsigned short nDepthWidth, unsigned short nDepthHeight,
        py::array_t<unsigned char> colorData, unsigned short nColorWidth, unsigned short nColorHeight)
std::vector<CloudPoint> PyGeneratePointCloud(
        py::array_t<unsigned char> depthData, unsigned short nDepthWidth, unsigned short nDepthHeight,
        py::array_t<unsigned char> colorData, unsigned short nColorWidth, unsigned short nColorHeight,
        bool bEnableFilter);
#else // Normal
std::vector<CloudPoint> PyGeneratePointCloud_GPU(
        unsigned char* depthData, unsigned short nDepthWidth, unsigned short nDepthHeight,
        unsigned char* colorData, unsigned short nColorWidth, unsigned short nColorHeight);
std::vector<CloudPoint> PyGeneratePointCloud(
        unsigned char* depthData, unsigned short nDepthWidth, unsigned short nDepthHeight,
        unsigned char* colorData, unsigned short nColorWidth, unsigned short nColorHeight,
        bool bEnableFilter);
#endif//--PYTHON_BUILD



std::vector<CloudPoint> GeneratePointCloud(
        STREAM_TYPE depthType,
        std::vector<BYTE> &depthData, unsigned short nDepthWidth, unsigned short nDepthHeight,
        std::vector<BYTE> &colorData, unsigned short nColorWidth, unsigned short nColorHeight,
        bool bEnableFilter);

std::vector<CloudPoint> GeneratePointCloud(std::vector<unsigned char> &depthData,
                                                              std::vector<unsigned char> &colorData,
                                                              unsigned short nDepthWidth,
                                                              unsigned short nDepthHeight,
                                                              unsigned short nColorWidth,
                                                              unsigned short nColorHeight,
                                                              eSPCtrl_RectLogData rectifyLogData,
                                                              EtronDIImageType::Value depthImageType,
                                                              int nZNear, int nZFar,
                                                              bool bUsePlyFilter, std::vector<float> imgFloatBufOut);
int PlyFilterTransform(std::vector<unsigned char> &depthData,
                                          std::vector<unsigned char> &colorData,
                                          unsigned short &nDepthWidth,
                                          unsigned short &nDepthHeight,
                                          unsigned short &nColorWidth,
                                          unsigned short &nColorHeight,
                                          std::vector<float> &imgFloatBufOut,
                                          eSPCtrl_RectLogData &rectifyLogData,
                                          EtronDIImageType::Value depthImageType);

int GetPointCloudInfo(PointCloudInfo &pointCloudInfo);

//eSPCtrl_RectLogData &GetRectifyLogData(STREAM_TYPE depthType);
