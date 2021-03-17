#include "RegisterSettings.h"
#include <unistd.h>
#include <iostream>
#include <fstream>
using namespace std;


RegisterSettings::RegisterSettings() {
}

int exchange(int data)
{
	int data_low;
	int data_high;
	data_low = (data & 0xff00) >> 8;
	data_high = (data & 0x00ff) << 8;
	int exchange_data = data_high + data_low;
	return exchange_data;
}

int RegisterSettings::DM_Quality_Register_Setting(void* hEtronDI, PDEVSELINFO pDevSelInfo)
{
    DEVINFORMATION devinfo;
    int nRet = EtronDI_GetDeviceInfo(hEtronDI, pDevSelInfo, &devinfo);
    if (nRet != ETronDI_OK)
    {
        printf("[Error] Cannot get device information.\n");
        return nRet;
    }

    char *modelName = NULL;

    switch (devinfo.wPID)
    {
    case ETronDI_PID_8036:
        modelName = "EX8036";
        break;
    case ETronDI_PID_8037:
        modelName = "EX8037";
        break;
    case ETronDI_PID_8038_M0:
        modelName = "EX8038_BL3cm"; //M0
        break;
    case ETronDI_PID_8040S:
        modelName = "EX8040S";
        break;
    case ETronDI_PID_8052:
        modelName = "EX8052";
        break;
    case ETronDI_PID_8053:
        modelName = "EX8053";
        break;
    case ETronDI_PID_8054:
        modelName = "EX8054";
        break;
    case ETronDI_PID_8059:
        modelName = "YX8059";
        break;
    case ETronDI_PID_8062:
        modelName = "YX8062";
        break;
    default:
        modelName = "Custom";
        break;
    }

    char fileName[256];
    char tmp[255];
    int RegAddress, ValidDataRange, Data;

    sprintf(fileName, "./DM_Quality_Cfg/%s_DM_Quality_Register_Setting.cfg", modelName);

    ifstream in(fileName);

    if (!in) {
        printf("Cannot open cfg file (%s).\n", fileName);
        return -1;
    }

    while (in) {
            in.getline(tmp, 255);  // delim defaults to '\n'
            if (in) {
                unsigned short RegValue;
                unsigned short NotValidDataRange;

                sscanf(tmp, "%x, %x, %x", &RegAddress, &ValidDataRange, &Data);

                EtronDI_GetHWRegister(hEtronDI, pDevSelInfo, RegAddress, &RegValue, FG_Address_2Byte | FG_Value_1Byte);

                NotValidDataRange = ~ValidDataRange;
                RegValue = RegValue & NotValidDataRange;
                RegValue |= Data;

                EtronDI_SetHWRegister(hEtronDI, pDevSelInfo, RegAddress, RegValue, FG_Address_2Byte | FG_Value_1Byte);

                //Sleep(5); // delay time, need fine tune in the feature

                ////////////////////

                unsigned short RegValueCheck = 0;
                EtronDI_GetHWRegister(hEtronDI, pDevSelInfo, RegAddress, &RegValueCheck, FG_Address_2Byte | FG_Value_1Byte);
                if (RegValue != RegValueCheck) {
                    printf("Set config. to Register failed !!");
                    return -1;
                }
            }
    }

    in.close();

    return 0;
}

