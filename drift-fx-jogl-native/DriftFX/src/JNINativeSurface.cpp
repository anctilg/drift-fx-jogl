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

#include <utils/Logger.h>
#include <utils/JNIHelper.h>

#include "JNINativeSurface.h"

using namespace driftfx::internal;

jclass JNINativeSurface::jNativeSurfaceClass = nullptr;
jmethodID JNINativeSurface::jNativeSurface_PresentMethod = nullptr;

jclass ResolveClass(JNIEnv* env, const char* name) {
	jclass cls = env->FindClass(name);
	if (cls == nullptr) {
		LogError("Failed to resolve class " << name)
	}
	return cls;
}
jmethodID ResolveMethod(JNIEnv* env, jclass cls, const char* name, const char* sig) {
	jmethodID method = env->GetMethodID(cls, name, sig);
	if (method == nullptr) {
		LogError("Failed to resolve Method " << name << " ( " << sig << ")")
	}
	return method;
}
jfieldID ResolveField(JNIEnv* env, jclass cls, const char* name, const char* sig) {
	jfieldID field = env->GetFieldID(cls, name, sig);
	if (field == nullptr) {
		LogError("Failed to resolve Field " << name << " ( " << sig << " )")
	}
	return field;
}


void JNINativeSurface::Initialize() {
	LogDebug("Initialize")

	JNIEnv *env = JNIHelper::GetJNIEnv(true);
	jNativeSurfaceClass = ResolveClass(env, "dev/anctil/fx/drift/jogl/internal/JNINativeSurface");
	jNativeSurface_PresentMethod = ResolveMethod(env, jNativeSurfaceClass, "present", "(JIII)V");

	LogDebug("initialization complete")
}

JNINativeSurface::JNINativeSurface(jobject javaNativeSurface) {
	JNIEnv *env = JNIHelper::GetJNIEnv(true);
	// protect the references from garbage collection
	LogDebug("registering global ref to surface " << javaNativeSurface)
	jNativeSurfaceInstance = reinterpret_cast<jobject>(env->NewGlobalRef(javaNativeSurface));
}

JNINativeSurface::~JNINativeSurface() {
	JNIEnv *env = JNIHelper::GetJNIEnv(true);
	env->DeleteGlobalRef(jNativeSurfaceInstance);
}


void JNINativeSurface::Present(FrameData frameData) {
	LogDebug("going to call present")
	JNIEnv *env = JNIHelper::GetJNIEnv(true);
	env->CallVoidMethod(jNativeSurfaceInstance, jNativeSurface_PresentMethod, frameData.d3dSharedHandle, frameData.glTextureName, frameData.width, frameData.height);
}

