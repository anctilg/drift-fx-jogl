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
package dev.anctil.fx.drift.jogl.internal;

import com.sun.prism.Texture;

@SuppressWarnings("restriction")
public class NativeAPI
{
    static
    {
        System.loadLibrary("driftfx-jogl");
        Initialize();
    }

    public static native long nGetD3DTextureHandle(long fxTextureHandle);

    private static native void nUpdateSize(long nativeSurfaceId, int width, int height);

    public static void updateSize(long nativeSurfaceId, int width, int height)
    {
        nUpdateSize(nativeSurfaceId, width, height);
    }

    private static native void nCleanFXSharedTextures(long nativeSurfaceId);

    public static void cleanFXSharedTextures(long nativeSurfaceId)
    {
        nCleanFXSharedTextures(nativeSurfaceId);
    }

    private static native void nDestroySharedTexture(long nativeSurfaceId, long sharedTextureId);

    public static void destroySharedTexture(long nativeSurfaceId, long sharedTextureId)
    {
        nDestroySharedTexture(nativeSurfaceId, sharedTextureId);
    }

    private static native void nInitializeD3DPipeline(long pContext);

    public static void initializeD3DPipeline(long pContext)
    {
        nInitializeD3DPipeline(pContext);
    }

    private static native void nDestroyD3DPipeline();

    public static void destroyD3DPipeline()
    {
        nDestroyD3DPipeline();
    }

    private static native int nD3DRecreateTextureAsShared(Texture fxTexture, long d3dShareHandle);

    public static int d3dRecreateTextureAsShared(Texture fxTexture, long d3dShareHandle)
    {
        return nD3DRecreateTextureAsShared(fxTexture, d3dShareHandle);
    }

    private static native long nD3DRecreateTexture(long textureResourceHandle);

    public static long d3dRecreateTexture(long textureResourceHandle)
    {
        return nD3DRecreateTexture(textureResourceHandle);
    }

    private static native void nInitialize();

    public static void Initialize()
    {
        nInitialize();
    }

    private static native void nDestroy();

    public static void Destroy()
    {
        nDestroy();
    }

    private static native long nCreateNativeSurface(JNINativeSurface surface);

    public static long createNativeSurface(JNINativeSurface surface)
    {
        return nCreateNativeSurface(surface);
    }

    private static native long nDestroyNativeSurface(long nativeSurfaceHandle);

    public static void destroyNativeSurface(long nativeSurfaceHandle)
    {
        nDestroyNativeSurface(nativeSurfaceHandle);
    }

    private static native void nReleaseNativeSurface(long pNativeSurfaceId);

    public static void release(long pNativeSurfaceId)
    {
        nReleaseNativeSurface(pNativeSurfaceId);
    }

    private static native int nAcquireNativeSurface(long pNativeSurfaceId, long pHandle, int pWidth, int pHeight);

    public static int acquire(long pNativeSurfaceId, long pHandle, int pWidth, int pHeight)
    {
        return nAcquireNativeSurface(pNativeSurfaceId, pHandle, pWidth, pHeight);
    }

    private static native void nRender(long pNativeSurfaceId, RenderCallback pCallback);

    public static void render(long pNativeSurfaceId, RenderCallback pCallback)
    {
        nRender(pNativeSurfaceId, pCallback);
    }

    public interface RenderCallback
    {
        void render(int glTexture, int width, int height);
    }
}
