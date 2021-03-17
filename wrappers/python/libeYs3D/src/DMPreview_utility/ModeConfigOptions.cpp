#include "ModeConfigOptions.h"

ModeConfigOptions::ModeConfigOptions(USB_PORT_TYPE usbType, unsigned short nPID):
m_nCurrentIndex(EOF)
{   m_nVecIndex = 0; 
    std::vector<ModeConfig::MODE_CONFIG> allConfigs = ModeConfig::GetModeConfig().GetModeConfigList(nPID);
    for(ModeConfig::MODE_CONFIG config : allConfigs ){
        //if(config.iUSB_Type != usbType) continue;
        m_modeConfigs.push_back(config);
        m_DBVecMap.insert(std::pair<int, int>(config.iMode, m_nVecIndex++));
    }

    if(!m_modeConfigs.empty()){
        m_nCurrentIndex = 0;
    }
}
