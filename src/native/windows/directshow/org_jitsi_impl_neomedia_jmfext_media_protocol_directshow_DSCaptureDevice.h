/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice */

#ifndef _Included_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
#define _Included_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    getBytes
 * Signature: (JJI)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_getBytes
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    connect
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_connect
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    disconnect
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_disconnect
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    getFormat
 * Signature: (J)Lorg/jitsi/impl/neomedia/jmfext/media/protocol/directshow/DSFormat;
 */
JNIEXPORT jobject JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_getFormat
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    getName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_getName
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    getSupportedFormats
 * Signature: (J)[Lorg/jitsi/impl/neomedia/jmfext/media/protocol/directshow/DSFormat;
 */
JNIEXPORT jobjectArray JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_getSupportedFormats
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    setDelegate
 * Signature: (JLorg/jitsi/impl/neomedia/jmfext/media/protocol/directshow/DSCaptureDevice/GrabberDelegate;)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_setDelegate
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    setFormat
 * Signature: (JLorg/jitsi/impl/neomedia/jmfext/media/protocol/directshow/DSFormat;)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_setFormat
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    start
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_start
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice
 * Method:    stop
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_directshow_DSCaptureDevice_stop
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif