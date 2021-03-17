#pragma once
#include <string>
#include "eSPDI.h"

class RegisterSettings {
	RegisterSettings();
public:
    static int DM_Quality_Register_Setting(void* hEtronDI, PDEVSELINFO pDevSelInfo);
};
