/*
 * Copyright (C) 2015-2019 ICL/ITRI
 * All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of ICL/ITRI and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to ICL/ITRI and its suppliers and
 * may be covered by Taiwan and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from ICL/ITRI.
 */

#include "devices/model/IRProperty.h"

#include <stdio.h>

namespace libeYs3D    {
namespace devices    {

IRProperty::IRProperty(uint16_t initValue, bool extendIR)
    : mIRValue(initValue), mExtendIREnabled(extendIR)    {
}

int IRProperty::toString(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- Device IR Property. ----\n"
                    "        mExtendIREnabled: %s\n"
                    "        mIRValue:         %d\n"
                    "        mIRMax:           %d\n"
                    "        mIRMin:           %d",
                    mExtendIREnabled ? "true" : "false",
                    (int)mIRValue, (int)mIRMax, (int)mIRMin);
}

bool IRProperty::operator==(const IRProperty &rhs) const    {
    if(mExtendIREnabled == rhs.mExtendIREnabled && mIRValue == rhs.mIRValue)
        return true;

    return false;
}

} // end of namespace devices
} // end of namespace libeYs3D
