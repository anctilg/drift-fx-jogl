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
#include "Common.h"
#include "NativeSurface.h"
#include "SharedTexture.h"

#include "prism/PrismBridge.h"

#include <utils/Logger.h>

#include <iostream>
using namespace std;

using namespace driftfx;
using namespace driftfx::gl;

using namespace driftfx::internal;
using namespace driftfx::internal::prism;

NativeSurface::NativeSurface(JNINativeSurface* api) :
	api(api),
	context(nullptr),
	renderTarget(nullptr) {
	LogDebug("NativeSurface constructor")

}

NativeSurface::~NativeSurface() {
    LogDebug("NativeSurface" << " destructor")
    Cleanup();
	delete api;
	api = nullptr;
}

void NativeSurface::Initialize(GLContext* pContext) {
    LogDebug("init GLContext");
    context = pContext;
}

void NativeSurface::Cleanup() {
	LogDebug("clean textures");
	if (renderTarget != nullptr) {
		delete renderTarget;
        renderTarget = nullptr;
	}

	if (api != nullptr)
	{
		//	// TODO send some kind of signal to tell FX we are going to dispose our textures
		FrameData* frameData = new FrameData();
		frameData->d3dSharedHandle = 0;
		frameData->width = 0;
		frameData->height = 0;
		api->Present(*frameData);
		delete frameData;
		frameData = nullptr;
	}

//
//
	// NOTE: since textures know their context and set it current upon deletion
	// we must ensure that all textures from a context are deleted before the context is deleted!

	LogDebug("clean GLContext");
	delete context;
	context = nullptr;

}

RenderTarget* NativeSurface::GetRenderTarget() const {
    return renderTarget;
}

GLContext* NativeSurface::GetContext() const {
	return context;
}

void NativeSurface::UpdateSize(int width, int height) {
	this->height = height;
	this->width = width;
}

unsigned int NativeSurface::GetWidth() const {
	return width;
}

unsigned int NativeSurface::GetHeight() const {
	return height;
}

int NativeSurface::Acquire() {
	return Acquire(GetWidth(), GetHeight());
}

int NativeSurface::Acquire(unsigned int width, unsigned int height) {
	LogDebug("acquire");
	if (renderTarget != nullptr) {
		delete renderTarget;
        renderTarget = nullptr;
	}

	PrismBridge* bridge = PrismBridge::Get();
	// in case the system was destroyed
	if (bridge == nullptr) {
		LogDebug("Could not acquire RenderTarget. Was the system destroyed?");
		return -1;
	}

    renderTarget = SharedTexture::Create(GetContext(), GetFxContext(), width, height);
    renderTarget->Connect();
    return renderTarget->GetGLTexture();
}

void NativeSurface::Prepare() {
    if (renderTarget == nullptr) {
        LogDebug("Cannot prepare nullptr; doing nothing.");
        return;
    }

    renderTarget->Lock();
}

void NativeSurface::Present() {
	if (renderTarget == nullptr) {
		LogDebug("Cannot present nullptr; doing nothing.");
		return;
	}
    renderTarget->Unlock();

	FrameData* frameData = renderTarget->CreateFrameData();
	LogDebug("PRESENT " << frameData->glTextureName);

	api->Present(*frameData);

	delete frameData;
}

Context* NativeSurface::GetFxContext() const {
	return PrismBridge::Get()->GetFxContext();
}

