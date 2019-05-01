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

#ifndef DRIFTFX_GL_GLDEBUG_H_
#define DRIFTFX_GL_GLDEBUG_H_

#include <GL/glew.h>


#include <iomanip>
#include <string>

#include <utils/Logger.h>

namespace driftfx {
namespace gl {

#if WITH_LOGGING
#define logGLErr(code) code; { GLuint err = glGetError(); if (err != 0) LogError("GL Error: 0x" << std::hex << err << " " << driftfx::gl::TranslateGLError(err) << " ( " << #code << " )" ); }
#define GLERR(code) code; { GLuint err = glGetError(); if (err != 0) LogError("GL Error: 0x" << std::hex << err << " " << driftfx::gl::TranslateGLError(err) << " ( " << #code << " )" ); }
#else
#define logGLErr(code) code; { glGetError(); }
#define GLERR(code) code; { glGetError(); }

#endif
__declspec(dllexport) std::string TranslateGLError(GLuint err);

}
}
#endif /* DRIFTFX_GL_GLDEBUG_H_ */
