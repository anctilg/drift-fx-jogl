#include "ExternalGLContext.h"

#include <utils/Logger.h>

#include <iomanip>
#include <iostream>
using namespace std;


using namespace driftfx::gl;
using namespace driftfx::internal::gl;

ExternalGLContext::ExternalGLContext(HGLRC handle) {
    hGLRC = handle;
}

GLContext* ExternalGLContext::CreateSharedContext() {
    return nullptr;
}

ExternalGLContext::~ExternalGLContext() {
}

uint64_t ExternalGLContext::GetContextHandle() {
    return reinterpret_cast<uint64_t>(hGLRC);
}

HGLRC ExternalGLContext::GetHandle() {
    return hGLRC;
}


void ExternalGLContext::SetCurrent() {
}

void ExternalGLContext::UnsetCurrent() {
}

