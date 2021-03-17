package com.esp.uvc.roi_size

enum class RoiSize(val size: Int) {

    Roi1(1),
    Roi10(10),
    Roi20(20),
    Roi30(30),
    Roi40(40);

    override fun toString(): String {
        return "${size}x$size"
    }
}
