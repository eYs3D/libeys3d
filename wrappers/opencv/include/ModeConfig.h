#pragma once
#include <vector>
//#include <QString>
#include <map>
#include <string.h>

struct sqlite3;
class ModeConfig
{
public:

    struct MODE_CONFIG
    {
        enum DECODE_TYPE
        {
            YUYV,
            MJPEG
        };
        struct RESOLUTION
        {
            int Width;
            int Height;

            RESOLUTION() : Width( 0 ), Height( 0 ) {}
        };       
        int                iMode;
        int                iUSB_Type;
        int                iInterLeaveModeFPS;
        bool               bRectifyMode;
        DECODE_TYPE        eDecodeType_L;
        DECODE_TYPE        eDecodeType_K;
        DECODE_TYPE        eDecodeType_T;
        RESOLUTION         L_Resolution;
        RESOLUTION         D_Resolution;
        RESOLUTION         K_Resolution;
        RESOLUTION         T_Resolution;      
        std::vector< int > vecDepthType;   
        std::vector< int > vecColorFps;
        std::vector< int > vecDepthFps;
        //QString            csModeDesc;
        std::string        csModeDesc;

        MODE_CONFIG() : eDecodeType_L( YUYV ), eDecodeType_K( YUYV ),  eDecodeType_T( YUYV ), iInterLeaveModeFPS( 0 ) {}
    };
    ModeConfig();
    ~ModeConfig();
    const std::vector< MODE_CONFIG >& GetModeConfigList( const int iPID );

    static ModeConfig& GetModeConfig() { return m_ModeConfig; }

private:

    struct PID_TABLE
    {
        //QString csTableName;
        std::string csTableName;

        std::vector< MODE_CONFIG > vecModeConfig;
    };
    //ModeConfig();
    //~ModeConfig();

    static ModeConfig m_ModeConfig;

    sqlite3* m_sq3;
    std::map< int, PID_TABLE > m_mapDeviceTable;

    void ReadModeConfig();
};

extern ModeConfig& g_ModeConfig;
