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

#include <unistd.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <time.h>
#include <limits.h>
#include <libudev.h>
#include <sys/time.h>
#include <libgen.h>

#include "debug.h"
#include "utils.h"

char *string_trim(char *str)    {
    const char *seps = "\t\n\v\f\r ";

    { // ltrim
        size_t totrim = strspn(str, seps);
        if(totrim > 0)    {
            size_t len = strlen(str);
            if(totrim == len) {
                str[0] = '\0';
            } else    {
                memmove(str, str + totrim, len + 1 - totrim);
            }
        }
    }
    { //rtrim
        int i = strlen(str) - 1;
        while(i >= 0 && strchr(seps, str[i]) != NULL)    {
            str[i] = '\0';
            i--;
        }
    }
                    
    return str;
}

int read_fully(int fd, void* data, size_t byte_count, const char *log_tag)    {
    uint8_t* p = (uint8_t*)(data);
    size_t remaining = byte_count;

    while(remaining > 0) {
        ssize_t n = TEMP_FAILURE_RETRY(read(fd, p, remaining));
        if(n == 0)    {
            // if read returns zero, that means the connection has been closed
            LOG_INFO(log_tag, "%s Client has closed connection, fd(%d)", __FUNCTION__, fd);
            return 0;
        } else if(n < 0)    {
            LOG_ERR(log_tag, "Unable to read data...");
            return -errno;
        }

        p += n;
        remaining -= n;
    }

    return byte_count;
 }

int write_fully(int fd, const void* data, size_t byte_count, const char *log_tag)    {
    const uint8_t* p = (const uint8_t*)(data);
    size_t remaining = byte_count;
    while(remaining > 0) {
        ssize_t n = TEMP_FAILURE_RETRY(write(fd, p, remaining));
        if(n < 0)    {
            LOG_ERR(log_tag, "Unable to write data...");
            return n;
        }

        p += n;
        remaining -= n;
    }

    return byte_count;
}

void get_process_name(uint32_t pid, char *procName, int length)    {   
    char path[PATH_MAX];
    char line[PATH_MAX];
    FILE* fp; 
    char *name = nullptr;

    snprintf(path, PATH_MAX, "/proc/%d/cmdline", pid);
    if((fp = fopen(path, "r"))) {
        name = fgets(line, sizeof(line), fp);
        fclose(fp);
    }   

    memset(procName, 0, length);

    if(!name) {
        strncpy(procName, "<unknown>", length);
        return;
    }   

    name = strchr(line, ' ');
    if(name) 
        *name = '\0';

    name = strrchr(line, '/');
    if(name != NULL)
        strncpy(procName, (name + 1), length);
    else
        strncpy(procName, line, length);
}

int get_executable_path(char *path, int length, const char *log_tag)    {
#ifdef __linux__ 
    int bytes = MIN(readlink("/proc/self/exe", path, length), length - 1);
    if(bytes >= 0)    {
        path[bytes] = '\0';
    } else    {
        LOG_ERR(log_tag, "Error getting executable path !");
    }
        
    return bytes;
#elif _WIN32
    int bytes = GetModuleFileName(NULL, path, (size_t)length);
    
    return bytes ? bytes : -1;
#else
    path[0] = '\0';
    
    return 0;
#endif
}

int get_executable_dir(char *dir, int length, const char *log_tag)    {
    char *tmp, path[PATH_MAX];
    int ret = 0;
    
    ret = get_executable_path(path, sizeof(path), log_tag);
    if(ret <= 0)    return ret;
    
    tmp = dirname(path);
    strncpy(dir, tmp, length);
    LOG_INFO(log_tag, "Getting executable dir: %s", tmp);
    
    return strlen(dir);
}


#if 0
int64_t now_in_millisecond()    {
    struct timespec time = {0, 0};
    clock_gettime(CLOCK_MONOTONIC, &time);
    return static_cast<uint64_t>(time.tv_sec) * 1000u +
           static_cast<uint64_t>(time.tv_nsec / 1000000u);
}
#endif

int64_t now_in_microsecond_unix_time()    {
    timeval tv;
    gettimeofday(&tv, nullptr);
    return tv.tv_sec * 1000000LL + tv.tv_usec;
}

int64_t now_in_microsecond_high_res_time()    {
    timespec ts;
    
    //clock_gettime(CLOCK_MONOTONIC, &ts);
    clock_gettime(CLOCK_REALTIME, &ts);
    return ts.tv_sec * 1000000ll + ts.tv_nsec / 1000;
}

void get_time_YYYY_MM_DD_HH_MM_SS(int64_t timeMs, char* buffer, size_t length)    {
    time_t timer = timeMs / 1000;
    struct tm* tm_info;

    tm_info = localtime(&timer);

    strftime(buffer, length, "%Y-%m-%d_%H:%M:%S", tm_info);
}

int get_cpu_core_count()    {
#ifdef _WIN32
    SYSTEM_INFO si = {};
    ::GetSystemInfo(&si);
    return si.dwNumberOfProcessors < 1 ? 1 : si.dwNumberOfProcessors;
#else
    auto res = (int)::sysconf(_SC_NPROCESSORS_ONLN);
    return res < 1 ? 1 : res;
#endif
}

int get_model_name(const char *devPath, char *out, int length, const char *log_tag)    {
    FILE *file;
    char buffer[PATH_MAX];
    int tmp = 0, ret = 0;

    sprintf(buffer, "/sys/class/video4linux/%s/name", &devPath[5]); // 5 = strlen("/dev/")
    file = fopen(buffer, "r");
    if(!file) {
        LOG_ERR(log_tag, "Could not open %s", buffer);
        return -errno;
    }    

    if(NULL == fgets(out, length, file))    {
        LOG_ERR(log_tag, "Could not open %s", buffer);
        ret = -errno;
        goto end;
    }

    tmp = strlen(out);
    if(out[tmp -1] == '\n')    out[tmp - 1] = '\0';

end:
    fclose(file); 

    return ret;
}

USB_PORT_TYPE get_usb_type(const char *devPath)    {
    USB_PORT_TYPE usb_type = USB_PORT_TYPE_UNKNOW;
    struct udev *udev;
    struct udev_enumerate *enumerate;
    struct udev_list_entry *devices, *dev_list_entry;
    struct udev_device *dev;

    udev = udev_new();
    enumerate = udev_enumerate_new(udev);
    udev_enumerate_add_match_subsystem(enumerate, "video4linux");
    udev_enumerate_scan_devices(enumerate);
    devices = udev_enumerate_get_list_entry(enumerate);

    udev_list_entry_foreach(dev_list_entry, devices) {
        const char *path = udev_list_entry_get_name(dev_list_entry);

        // 5 = strlen("/dev/")
        if(strcmp(&path[strlen(path) - strlen(&devPath[5])], &devPath[5]))    continue;

        dev = udev_device_new_from_syspath(udev, path);
        const char *speed = udev_device_get_sysattr_value(dev, "speed");
        struct udev_device *nodeDev = dev;
        while(!speed){
            struct udev_device *parentDev = udev_device_get_parent(dev);
            if(!parentDev) break;
            dev = parentDev;
            speed = udev_device_get_sysattr_value(dev, "speed");
        }

        if(speed)    {
            if(!strcmp(speed, "5000")){
                usb_type = USB_PORT_TYPE_3_0;
             } else if (!strcmp(speed, "480")){
                usb_type = USB_PORT_TYPE_2_0;
            }
        }

        udev_device_unref(nodeDev);
        break;
    }

    udev_enumerate_unref(enumerate);
    udev_unref(udev);

    return usb_type;
}
