#include "com_deepblue_logd_DeepBlueNativeLog.h"
/*
 * Class:     com_deepblue_logd_DeepBlueNativeLog
 * Method:    native_init
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_deepblue_logd_DeepBlueNativeLog_native_1init
  (JNIEnv *env, jobject instance, jstring cache_path_, jstring dir_path_, jint max_file){
    const char *dir_path = (*env)->GetStringUTFChars(env, dir_path_, 0);
    const char *cache_path = (*env)->GetStringUTFChars(env, cache_path_, 0);

    jint code = (jint) clogan_init(cache_path, dir_path, max_file);

    (*env)->ReleaseStringUTFChars(env, dir_path_, dir_path);
    (*env)->ReleaseStringUTFChars(env, cache_path_, cache_path);
    return code;
  }

/*
 * Class:     com_deepblue_logd_DeepBlueNativeLog
 * Method:    native_open
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_deepblue_logd_DeepBlueNativeLog_native_1open
  (JNIEnv *env, jobject instance, jstring file_name_) {
    const char *file_name = (*env)->GetStringUTFChars(env, file_name_, 0);

    jint code = (jint) clogan_open(file_name);

    (*env)->ReleaseStringUTFChars(env, file_name_, file_name);
    return code;
  }

/*
 * Class:     com_deepblue_logd_DeepBlueNativeLog
 * Method:    native_write
 * Signature: (ILjava/lang/String;JLjava/lang/String;JI)I
 */
JNIEXPORT jint JNICALL Java_com_deepblue_logd_DeepBlueNativeLog_native_1write
  (JNIEnv *env, jobject instance, jint flag, jstring log_, jlong local_time, jstring thread_name_, jlong thread_id, jint ismain) {

    const char *log = (*env)->GetStringUTFChars(env, log_, 0);
    const char *thread_name = (*env)->GetStringUTFChars(env, thread_name_, 0);

    jint code = (jint) clogan_write(flag, log, local_time, thread_name, thread_id, ismain);

    (*env)->ReleaseStringUTFChars(env, log_, log);
    (*env)->ReleaseStringUTFChars(env, thread_name_, thread_name);
    return code;

  }

/*
 * Class:     com_deepblue_logd_DeepBlueNativeLog
 * Method:    native_flush
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_deepblue_logd_DeepBlueNativeLog_native_1flush
  (JNIEnv *env, jobject instance) {
    clogan_flush();
  }
