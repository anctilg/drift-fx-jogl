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
package dev.anctil.fx.drift.jogl.impl;

import java.util.concurrent.*;

import com.sun.javafx.sg.prism.NGNode;
import com.sun.prism.*;

import dev.anctil.fx.drift.jogl.internal.*;
import dev.anctil.fx.drift.jogl.internal.JNINativeSurface.FrameData;

// Note: this implementation is against internal JavafX API
@SuppressWarnings("restriction")
public class NGDriftFXSurface extends NGNode
{
    private long nativeSurfaceHandle;

    private float width;
    private float height;

    private ResourceFactory resourceFactory;

    private FrameData currentFrameData;
    private int currentFrameDataHash;
    private Texture currentTexture;

    private boolean isActive;

    public void present(FrameData frame)
    {
        if (frame != null && frame.d3dShareHandle != 0)
        {
            currentFrameData = frame;
        }
    }

    public NGDriftFXSurface(long nativeSurfaceId)
    {
        this.nativeSurfaceHandle = nativeSurfaceId;
        Log.debug("NGNativeSurface got handle: " + this.nativeSurfaceHandle);

        this.resourceFactory = GraphicsPipelineUtil.getResourceFactory();
    }

    private int getWidth()
    {
        return Math.max(1, (int) Math.ceil(width));
    }

    private int getHeight()
    {
        return Math.max(1, (int) Math.ceil(height));
    }

    private Texture createTexture(Graphics g, FrameData data)
    {
        if (data.d3dShareHandle != 0)
        {
            int w = data.width;
            int h = data.height;

            // create fx texture
            Texture texture = resourceFactory.createTexture(PixelFormat.BYTE_BGRA_PRE, Texture.Usage.DYNAMIC,
                    Texture.WrapMode.CLAMP_TO_EDGE, w, h);
            if (texture == null)
            {
                System.err.println(
                        "[J] Allocation of requested texture failed! This is FATAL! requested size was " + w + "x" + h);
                System.err.flush();
                return null;
            }
            texture.makePermanent();

            // recreate shared texture
            // to protect the javafx gl context we change threads here
            int[] createTextureResult = new int[] { -1 };
            CountDownLatch latch = new CountDownLatch(1);
            fixer.execute(() -> {
                try
                {
                    createTextureResult[0] = GraphicsPipelineUtil.onTextureCreated(texture, data);
                }
                catch (Throwable t)
                {
                    System.err.println("[J] Crash while creating D3D texture: " + t.getMessage());
                }
                finally
                {
                    latch.countDown();
                }
            });
            try
            {
                latch.await();
            }
            catch (InterruptedException e)
            {
            }

            int result = createTextureResult[0];
            if (result == 0)
            {
                return texture;
            }
            else
            {
                texture.dispose();
                return null;
            }

        }
        return null;
    }

    static Executor fixer = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(r, "NGDriftFX");
            t.setDaemon(true);
            return t;
        }
    });

    @Override
    protected void renderContent(Graphics g)
    {
        int lWidth = getWidth();
        int lHeight = getHeight();

        // TODO add signal & check it here!
        if (currentFrameData != null && currentFrameData.width != 0 && currentFrameData.height != 0)
        {
            int hash = currentFrameData.hashCode();
            if (hash != currentFrameDataHash)
            {
                currentFrameDataHash = hash;
                Texture texture = createTexture(g, currentFrameData);
                if (texture != null)
                {
                    if (currentTexture != null)
                    {
                        currentTexture.dispose();
                    }
                    currentTexture = texture;
                }
                else
                {
                    Log.debug(
                            "[WARNING] Surface# \"+nativeSurfaceHandle+\": Could not recreate texture, keeping old one.");
                }
            }
        }

        if (currentTexture != null && isActive)
        {
            int lX = 0;
            int lY = 0;
            int textureWidth = currentTexture.getContentWidth();
            int textureHeight = currentTexture.getContentHeight();
            int targetWidth = textureWidth;
            int targetHeight = textureHeight;
            if (lHeight != targetHeight || lWidth != targetWidth)
            {
                double targetRatio = lHeight != 0 ? lWidth / (double) lHeight : 1.0;
                targetHeight = textureHeight;
                targetWidth = (int) Math.ceil(targetHeight * targetRatio);
                lX = (textureWidth - targetWidth) / 2;
                lY = (textureHeight - targetHeight) / 2;
            }
            Log.debug("[Info ] Surface# " + nativeSurfaceHandle + ": Drawing texture "
                    + GraphicsPipelineUtil.getTextureHandle(currentTexture) + " ["
                    + System.identityHashCode(currentTexture) + "]");
            g.drawTexture(currentTexture, 0, lHeight, lWidth, 0, lX, lY, targetWidth + lX, targetHeight + lY);
        }
        else
        {
            Log.debug("[Info ] current Texture == null");
        }
    }

    public void updateSize(float width, float height)
    {
        if (width != -1)
        {
            this.width = width;
        }
        if (height != -1)
        {
            this.height = height;
        }
        fixer.execute(() -> NativeAPI.updateSize(nativeSurfaceHandle, getWidth(), getHeight()));
    }

    @Override
    protected boolean hasOverlappingContents()
    {
        return false;
    }

    public void setActive(boolean isActive)
    {
        this.isActive = isActive;
    }

}
