#ifndef JNIHELPER_H_
#define JNIHELPER_H_

#include <jni.h>

class JNIHelper {

public:
    __declspec(dllexport) static void Initialize(JavaVM* jvm);

    __declspec(dllexport) static bool IsThreadAttached();
    __declspec(dllexport) static void AttachThread();
    __declspec(dllexport) static void ReleaseThread();

    __declspec(dllexport) static JNIEnv* GetJNIEnv(bool autoAttach);

private:
	static JavaVM* jvm;
	static thread_local JNIEnv* jniEnv;

};


#endif /* JNIHELPER_H_ */
