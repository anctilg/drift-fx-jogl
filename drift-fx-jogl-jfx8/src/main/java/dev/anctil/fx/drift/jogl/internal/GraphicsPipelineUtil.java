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

import java.lang.reflect.*;

import com.sun.prism.*;

import dev.anctil.fx.drift.jogl.internal.JNINativeSurface.FrameData;

public class GraphicsPipelineUtil
{
    private static Class<?> cGraphicsPipeline;
    private static Method mGraphicsPipelineGetDefaultResourceFactory;

    private static Object iDefaultResourceFactory;

    private static boolean isD3D;
    private static Object iD3DContext;
    private static long contextHandleD3D;

    static class D3D
    {

        private static Class<?> cD3DResourceFactory;
        private static Method mD3DResourceFactoryGetContext;

        private static Class<?> cD3DContext;
        private static Method mD3DContextGetContextHandle;

        static void initialize() throws ClassNotFoundException, NoSuchMethodException, SecurityException
        {
            cD3DResourceFactory = Class.forName("com.sun.prism.d3d.D3DResourceFactory");
            mD3DResourceFactoryGetContext = cD3DResourceFactory.getDeclaredMethod("getContext");
            mD3DResourceFactoryGetContext.setAccessible(true);

            cD3DContext = Class.forName("com.sun.prism.d3d.D3DContext");
            mD3DContextGetContextHandle = cD3DContext.getDeclaredMethod("getContextHandle");
            mD3DContextGetContextHandle.setAccessible(true);
        }

        static Object getD3DContext(Object iResourceFactory) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
        {
            return mD3DResourceFactoryGetContext.invoke(iResourceFactory);
        }

        static long getContextHandle(Object iD3DContext) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
        {
            return (Long) mD3DContextGetContextHandle.invoke(iD3DContext);
        }
    }

    public static void initialize() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IllegalArgumentException
    {
        cGraphicsPipeline = Class.forName("com.sun.prism.GraphicsPipeline");
        mGraphicsPipelineGetDefaultResourceFactory = cGraphicsPipeline.getMethod("getDefaultResourceFactory");
        iDefaultResourceFactory = mGraphicsPipelineGetDefaultResourceFactory.invoke(null);

        String name = iDefaultResourceFactory.getClass().getName();
        isD3D = "com.sun.prism.d3d.D3DResourceFactory".equals(name);

        if (isD3D)
        {
            D3D.initialize();
            iD3DContext = D3D.getD3DContext(iDefaultResourceFactory);
            contextHandleD3D = D3D.getContextHandle(iD3DContext);
            Log.debug(" * D3D Context handle = " + contextHandleD3D);

            NativeAPI.initializeD3DPipeline(contextHandleD3D);
        }
        else
        {
            throw new UnsupportedOperationException("Unknown JavaFX pipeline!");
        }

    }

    public static boolean isD3D()
    {
        return isD3D;
    }

    public static void destroy()
    {
        NativeAPI.destroyD3DPipeline();
    }

    @SuppressWarnings("restriction")
    public static long getTextureHandle(Texture texture)
    {
        if (isD3D())
        {
            try
            {
                // TODO move class and method to members
                Class<?> d3dTexture = Class.forName("com.sun.prism.d3d.D3DTexture");
                Method mD3DTextureGetNativeSourceHandle = d3dTexture.getMethod("getNativeSourceHandle");
                mD3DTextureGetNativeSourceHandle.setAccessible(true);
                return (long) mD3DTextureGetNativeSourceHandle.invoke(texture);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        throw new UnsupportedOperationException("Not implemented");
    }

    @SuppressWarnings("restriction")
    public static ResourceFactory getResourceFactory()
    {
        return (ResourceFactory) iDefaultResourceFactory;
    }

    @SuppressWarnings("restriction")
    public static int onTextureCreated(Texture texture, FrameData currentFrameData)
    {
        if (isD3D())
        {
            int lD3dRecreateTextureAsShared = NativeAPI.d3dRecreateTextureAsShared(texture, currentFrameData.d3dShareHandle);
            return lD3dRecreateTextureAsShared;
        }
        return -1;
    }

}
