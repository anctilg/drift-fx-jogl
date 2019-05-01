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
#ifndef DRIFTFX_INTERNAL_NATIVESURFACE_H_
#define DRIFTFX_INTERNAL_NATIVESURFACE_H_

#include <DriftFX/RenderTarget.h>
#include <DriftFX/DriftFXSurface.h>
#include <DriftFX/GL/GLContext.h>

#include "JNINativeSurface.h"

namespace driftfx {
using namespace driftfx::gl;
namespace internal {

class NativeSurface : public DriftFXSurface {

public:
	virtual ~NativeSurface();

	/*
	 * Initialises this native surface.
	 * Should be called on your render thread.
	 */
	virtual void Initialize(GLContext* context);

	/*
	 * Cleanup this native surface.
	 * Should be called on your render thread.
	 * Releases all pending resources and destroys its contexts.
	 */
	virtual void Cleanup();

	/*
	 * Acquires a RenderTarget with the current width / height.
	 * delegates to Acquire(GetWidth(), GetHeight()).
	 */
	virtual int Acquire();

	/*
	 * Acquires a new RenderTarget with the given size.
	 * Should be called from your render thread.
	 *
	 */
	virtual int Acquire(unsigned int width, unsigned int height);

	/*
	* Prepares the previously acquired render target for rendering.
	* Should be called from your render thread.
	*/
	virtual void Prepare();
	/*
	 * Presents a previously acquired RenderTarget.
	 * Should be called from your render thread.
	 *
	 */
	virtual void Present();

    virtual RenderTarget* GetRenderTarget() const;

	virtual GLContext* GetContext() const;

	/*
	 * returns the current width of the NativeSurface in JavaFX
	 */
	unsigned int GetWidth() const;
	/*
	 * returns the current height of the NativeSurface in JavaFX
	 */
	unsigned int GetHeight() const;

	/**
	 * Internal API.
	 */
	virtual void UpdateSize(int width, int height);

	virtual Context* GetFxContext() const;

protected:
	NativeSurface(JNINativeSurface* api);

	JNINativeSurface *api;

	GLContext* context;

	volatile unsigned int width;
	volatile unsigned int height;

private:

	SharedTexture* renderTarget;
};

}
}

#endif /* DRIFTFX_INTERNAL_NATIVESURFACE_H_ */
