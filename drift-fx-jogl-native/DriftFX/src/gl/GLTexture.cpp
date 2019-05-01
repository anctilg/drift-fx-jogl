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

#include "../Common.h"
#include <GL/glew.h>
#include <iomanip>

#include <DriftFX/GL/GLTexture.h>
#include <DriftFX/Texture.h>

#include <DriftFX/GL/GLDebug.h>

#include "InternalGLContext.h"

using namespace driftfx;
using namespace driftfx::gl;

using namespace driftfx::internal::gl;

GLTexture::GLTexture(GLContext *context, int width, int height) : Texture(width, height),
	context(context),
	textureName(0) {

	context->SetCurrent();

	logGLErr(InternalGLContext::glGenTextures(1, &textureName));

	// TODO check for error & if textureName is valid
}

GLTexture::~GLTexture() {

	context->SetCurrent();

	glDeleteTextures(1, &textureName);
}

GLuint GLTexture::Name() {
	return textureName;
}
