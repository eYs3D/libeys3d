#ifndef MODECONFIGOPTIONS_H
#define MODECONFIGOPTIONS_H

#include <ModeConfig.h>
#include "eSPDI_def.h"

class ModeConfigOptions
{
public:
    ModeConfigOptions(USB_PORT_TYPE usbType, unsigned short nPID);
    ~ModeConfigOptions() = default;

    int GetModeCount(){ return m_modeConfigs.size(); }
    std::vector<ModeConfig::MODE_CONFIG> GetModes(){ return m_modeConfigs; }

    int SelectCurrentIndex(size_t nIndex){
        if(nIndex >= m_modeConfigs.size() ) return ETronDI_NullPtr;
        m_nCurrentIndex = nIndex;
        return ETronDI_OK;
    }
    int GetCurrentIndex(){ return m_nCurrentIndex; }
    ModeConfig::MODE_CONFIG GetCurrentModeInfo()
    {
        ModeConfig::MODE_CONFIG empty;
        if(EOF == m_nCurrentIndex) return empty;
        return m_modeConfigs[m_nCurrentIndex];
    }

private:
    std::vector< ModeConfig::MODE_CONFIG > m_modeConfigs;
    int m_nCurrentIndex;
};

#endif // MODECONFIGOPTIONS_H
