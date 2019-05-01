/*
 * Copyright (c) 2018 BestSolution and Others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

#include <jni.h>

#include "prism/PrismBridge.h"

#include "NativeSurface.h"
#include "JNINativeSurface.h"

#include "NativeSurfaceRegistry.h"

#include "JNINativeSurface.h"
#include "NativeSurface.h"

#include "SharedTexture.h"

#include "DriftFX/DriftFX.h"

#include <iostream>
using namespace std;

#include <utils/JNIHelper.h>
#include <utils/Logger.h>

using namespace driftfx::internal;

namespace {
jclass jRenderCallbackClass = nullptr;
jmethodID jRenderCallbackMethod = nullptr;

void InitCallbackClass() {
    JNIEnv *env = JNIHelper::GetJNIEnv(true);
    jRenderCallbackClass = env->FindClass("dev/anctil/fx/drift/jogl/internal/NativeAPI$RenderCallback");
    if (jRenderCallbackClass != nullptr) {
        jRenderCallbackMethod = env->GetMethodID(jRenderCallbackClass, "render", "(III)V");
    }
}
}

extern "C" JNIEXPORT void JNICALL Java_dev_anctil_fx_drift_jogl_internal_NativeAPI_nInitialize(JNIEnv *env, jclass cls) {
	LogDebug("nInitialize")
	JavaVM* jvm;
	env->GetJavaVM(&jvm);
	JNIHelper::Initialize(jvm);
	JNINativeSurface::Initialize();
    InitCallbackClass();
}

extern "C" JNIEXPORT jlong JNICALL Java_dev_anctil_fx_drift_jogl_internal_NativeAPI_nCreateNativeSurface(JNIEnv *env, jclass cls, jobject surfaceObj) {
	LogDebug("nCreateNativeSurface")
	JNINativeSurface *api = new JNINativeSurface(surfaceObj);
	return NativeSurfaceRegistry::Get()->Create(api);
}

extern "C" JNIEXPORT void JNICALL Java_dev_anctil_fx_drift_jogl_internal_NativeAPI_nDestroyNativeSurface(JNIEnv *env, jclass cls, jlong surfaceId) {
	LogDebug("nDestroyNativeSurface")
	NativeSurfaceRegistry::Get()->Destroy(surfaceId);
}

extern "C" JNIEXPORT int JNICALL Java_dev_anctil_fx_drift_jogl_internal_NativeAPI_nAcquireNativeSurface(JNIEnv *env, jclass cls, jlong pNativeSurfaceId, jlong pHandle, jint pWidth, jint pHeight) {
    NativeSurface* surface = NativeSurfaceRegistry::Get()->Get(pNativeSurfaceId);
    if (surface != nullptr) {
        driftfx::gl::GLContext* lContext = surface->GetContext();
        if (lContext == nullptr || lContext->GetContextHandle() != pHandle) {
            delete lContext;
            surface->Initialize(driftfx::DriftFX::Get()->CreateGLContextFromHandle(pHandle));
        }
        return surface->Acquire(pWidth, pHeight);
    }
    return -1;
}

extern "C" JNIEXPORT void JNICALL Java_dev_anctil_fx_drift_jogl_internal_NativeAPI_nReleaseNativeSurface(JNIEnv *env, jclass cls, jlong pNativeSurfaceId) {
    NativeSurface* surface = NativeSurfaceRegistry::Get()->Get(pNativeSurfaceId);
    if (surface != nullptr) {
        surface->Cleanup();
    }
}

extern "C" JNIEXPORT void JNICALL Java_dev_anctil_fx_drift_jogl_internal_NativeAPI_nUpdateSize(JNIEnv *env, jclass cls, jlong surfaceId, jint width, jint height) {
	NativeSurface* surface = NativeSurfaceRegistry::Get()->Get(surfaceId);
    if (surface != nullptr) {
        surface->UpdateSize((int)width, (int)height);
    }
}

extern "C" JNIEXPORT void JNICALL Java_dev_anctil_fx_drift_jogl_internal_NativeAPI_nRender(JNIEnv *env, jclass cls, jlong surfaceId, jobject pConsumerInterface) {
    NativeSurface* surface = NativeSurfaceRegistry::Get()->Get(surfaceId);
    if (surface != nullptr) {
        driftfx::RenderTarget* rt = surface->GetRenderTarget();
        if (rt != nullptr && jRenderCallbackClass != nullptr && jRenderCallbackMethod != nullptr) {
            JNIEnv *env = JNIHelper::GetJNIEnv(true);
            surface->Prepare();
            env->CallVoidMethod(pConsumerInterface, jRenderCallbackMethod, rt->GetGLTexture(), rt->GetWidth(), rt->GetHeight());
            surface->Present();
        }
    }
}

