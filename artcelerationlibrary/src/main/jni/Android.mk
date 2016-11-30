LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := edu_asu_msrs_artcelerationlibrary_NativeClass.cpp

LOCAL_LDLIBS += -llog
LOCAL_MODULE := MyLibs
LOCAL_LDFLAGS += -ljnigraphics

include $(BUILD_SHARED_LIBRARY)