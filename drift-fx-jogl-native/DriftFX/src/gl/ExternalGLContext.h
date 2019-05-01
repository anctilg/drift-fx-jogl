#ifndef SHAREDSURFACE_WIN32_EXTERNALGLCONTEXT_H_
#define SHAREDSURFACE_WIN32_EXTERNALGLCONTEXT_H_

#include <GL/glew.h>
#include <GL/wglew.h>
#include <DriftFX/GL/GLContext.h>
#include <DriftFX/Texture.h>

namespace driftfx {
    using namespace gl;
    namespace internal {
        namespace gl {
            class ExternalGLContext : public GLContext {

            public:
                ExternalGLContext(HGLRC handle);

                virtual ~ExternalGLContext();
                virtual void SetCurrent();
                virtual void UnsetCurrent();

                virtual uint64_t GetContextHandle();
                virtual HGLRC GetHandle();

                virtual GLContext* CreateSharedContext();

            private:
                HGLRC hGLRC;
            };
        }
    }
}


#endif /* SHAREDSURFACE_WIN32_EXTERNALGLCONTEXT_H_ */
