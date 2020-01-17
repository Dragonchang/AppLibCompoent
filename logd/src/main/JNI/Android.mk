LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := clogan

LOCAL_CFLAGS := -DANDROID_NDK \
                -DDISABLE_IMPORTGL

LOCAL_SRC_FILES := \
                 base_util.c \
                 cJSON.c \
                 clogan_core.c \
                 construct_data.c \
                 directory_util.c \
                 json_util.c \
                 mmap_util.c \
                 com_deepblue_logd_DeepBlueNativeLog.c

LOCAL_LDLIBS    := -lm -llog -ljnigraphics -landroid $(extra_ldlibs)

include $(BUILD_SHARED_LIBRARY)