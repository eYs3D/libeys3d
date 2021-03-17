#include "ModeConfigOptions.h"

ModeConfigOptions::ModeConfigOptions(USB_PORT_TYPE usbType, unsigned short nPID):
m_nCurrentIndex(EOF)
{    
    std::vector<ModeConfig::MODE_CONFIG> allConfigs = ModeConfig::GetModeConfig().GetModeConfigList(nPID);
    for(ModeConfig::MODE_CONFIG config : allConfigs ){
        if(config.iUSB_Type != usbType) continue;
        m_modeConfigs.push_back(config);
    }

    if(!m_modeConfigs.empty()){
        m_nCurrentIndex = 0;
    }
}
