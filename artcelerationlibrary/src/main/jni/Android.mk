LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := edu_asu_msrs_artcelerationlibrary_NativeClass.cpp.neon

LOCAL_LDLIBS += -llog
LOCAL_MODULE := MyLibs
LOCAL_LDFLAGS += -ljnigraphics
LOCAL_ARM_NEON := true
include $(BUILD_SHARED_LIBRARY)