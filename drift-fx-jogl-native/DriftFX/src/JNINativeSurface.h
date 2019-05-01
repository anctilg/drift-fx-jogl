#ifndef JNINATIVESURFACE_H_
#define JNINATIVESURFACE_H_

#include <jni.h>
#include "SharedTexture.h"

namespace driftfx {
namespace internal {

class JNINativeSurface {

public:
	JNINativeSurface(jobject javaNativeSurface);
	virtual ~JNINativeSurface();


	void Present(FrameData frameData);

	static void Initialize();

private:

	jobject jNativeSurfaceInstance;

	static jclass jNativeSurfaceClass;
	static jmethodID jNativeSurface_PresentMethod;
};

}
}


#endif /* JNINATIVESURFACE_H_ */
