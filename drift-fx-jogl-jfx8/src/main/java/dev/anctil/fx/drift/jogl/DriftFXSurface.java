/*
 * Copyright (c) 2018 BestSolution and Others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v. 2.0, which is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath Exception, which is available at https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package dev.anctil.fx.drift.jogl;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.Node;

import com.jogamp.opengl.*;
import com.sun.javafx.geom.*;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.*;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.Toolkit;

import dev.anctil.fx.drift.jogl.impl.NGDriftFXSurface;
import dev.anctil.fx.drift.jogl.internal.*;
import dev.anctil.fx.drift.jogl.internal.JNINativeSurface.FrameData;

// Note: this implementation is against internal JavafX API
@SuppressWarnings({ "restriction", "deprecation" })
public class DriftFXSurface extends Node
{
    /** The delay before a resize operation is actually handled and JOGL drawable is asked to resize, removing redraws and creation/deletion of surfaces while resizing */
    private static final int RESIZE_DELAY_MS = 60;
    /** Load and initialize pipeline only once */
    private static boolean initialized = false;
    private JOGLTransferCallback transferCallback;
    private boolean isActive;
    private Executor glExecutor = new PrivateExecutorService();
    private final DriftFXSurfaceNativeResource nativeResource;
    /** The ms timestamp at which a resize operation should be handled */
    private long resizeWaitTimestamp;

    public DriftFXSurface()
    {
        initialize();
        JNINativeSurface jni = new JNINativeSurface(mCreateFramePresenter(this));
        nativeResource = new DriftFXSurfaceNativeResource(this, jni);
    }

    /**
     * It is the responsibility of the caller to destroy the canvas once it is no longer needed
     */
    public void setGLCanvas(GLOffscreenAutoDrawable newCanvas)
    {
        if (nativeResource.canvas != null && nativeResource.canvasListener != null)
        {
            nativeResource.canvas.disposeGLEventListener(nativeResource.canvasListener, true);
            nativeResource.canvasListener = null;
        }
        nativeResource.canvas = newCanvas;
        if (nativeResource.canvas != null && nativeResource.nativeSurfaceId >= 0)
        {
            nativeResource.canvasListener = new DriftFXSurfaceGLListener(this);
            nativeResource.canvas.addGLEventListener(nativeResource.canvasListener);
        }
    }

    public void setGLTransferCallback(JOGLTransferCallback callback)
    {
        this.transferCallback = callback;
    }

    /**
     * This executor is used to delegate the canvas resize operations when the node is resized. By default a daemon thread per DriftFXSurface is created for this purpose. Once a new executor is set, the
     * daemon thread is destroyed.
     */
    public void setGLThreadExecutor(Executor executor)
    {
        if (executor != null)
        {
            if (glExecutor instanceof PrivateExecutorService)
            {
                ((PrivateExecutorService) glExecutor).shutdown();
            }
            glExecutor = executor;
        }
        else
        {
            glExecutor = new PrivateExecutorService();
        }
    }

    public GLOffscreenAutoDrawable getGLCanvas()
    {
        return nativeResource.canvas;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public void setActive(boolean active)
    {
        if (isActive != active)
        {
            impl_geomChanged();
            impl_layoutBoundsChanged();
            impl_markDirty(DirtyBits.NODE_GEOMETRY);
            impl_markDirty(DirtyBits.NODE_CONTENTS);
            resizeWaitTimestamp = 0;
        }
        isActive = active;
    }

    @Override
    protected NGNode impl_createPeer()
    {
        NGDriftFXSurface peer = new NGDriftFXSurface(nativeResource.nativeSurfaceId);
        return peer;
    }

    @Override
    public double minHeight(double width)
    {
        return 0;
    }

    @Override
    public double minWidth(double height)
    {
        return 0;
    }

    @Override
    public double prefWidth(double height)
    {
        return 1;
    }

    @Override
    public double prefHeight(double width)
    {
        return 1;
    }

    @Override
    public double maxWidth(double height)
    {
        return Double.MAX_VALUE;
    }

    @Override
    public double maxHeight(double width)
    {
        return Double.MAX_VALUE;
    }

    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx)
    {
        bounds = new RectBounds(0f, 0f, (float) getWidth(), (float) getHeight());
        bounds = tx.transform(bounds, bounds);
        return bounds;
    }

    @Override
    protected boolean impl_computeContains(double localX, double localY)
    {
        double w = getWidth();
        double h = getHeight();
        return (w > 0 && h > 0 && localX >= 0 && localY >= 0 && localX < w && localY < h);
    }

    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void widthChanged(double value)
    {
        if (value != _width)
        {
            _width = value;

            impl_layoutBoundsChanged();
            impl_geomChanged();
            impl_markDirty(DirtyBits.NODE_GEOMETRY);
            setResizeWaitTimestamp();
        }
    }

    private double _width;
    private ReadOnlyDoubleWrapper width;

    public final double getWidth()
    {
        if (isActive)
        {
            return width == null ? _width : width.get();
        }
        else
        {
            return 1;
        }
    }

    public final ReadOnlyDoubleProperty widthProperty()
    {
        if (width == null)
        {
            width = new ReadOnlyDoubleWrapper(_width)
            {
                @Override
                protected void invalidated()
                {
                    widthChanged(get());
                }

                @Override
                public Object getBean()
                {
                    return DriftFXSurface.this;
                }

                @Override
                public String getName()
                {
                    return "width";
                }
            };
        }
        return width.getReadOnlyProperty();
    }

    protected void setWidth(double value)
    {
        if (width == null)
        {
            widthChanged(value);
        }
        else
        {
            width.set(value);
        }
    }

    private void heightChanged(double value)
    {
        if (_height != value)
        {
            _height = value;

            impl_geomChanged();
            impl_layoutBoundsChanged();
            impl_markDirty(DirtyBits.NODE_GEOMETRY);
            setResizeWaitTimestamp();
        }
    }

    private double _height;
    private ReadOnlyDoubleWrapper height;

    public final double getHeight()
    {
        if (isActive)
        {
            return height == null ? _height : height.get();
        }
        else
        {
            return 1;
        }
    }

    public final ReadOnlyDoubleProperty heightProperty()
    {
        if (height == null)
        {
            height = new ReadOnlyDoubleWrapper(_height)
            {
                @Override
                protected void invalidated()
                {
                    heightChanged(get());
                }

                @Override
                public Object getBean()
                {
                    return DriftFXSurface.this;
                }

                @Override
                public String getName()
                {
                    return "height";
                }
            };
        }
        return height.getReadOnlyProperty();
    }

    protected void setHeight(double value)
    {
        if (height == null)
        {
            heightChanged(value);
        }
        else
        {
            height.set(value);
        }
    }

    public static synchronized void initialize()
    {
        if (initialized)
        {
            return;
        }

        Log.debug("Initializing NativeSurface system");
        try
        {
            GraphicsPipelineUtil.initialize();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        if (GraphicsPipelineUtil.isD3D())
        {
            Log.debug(" * D3D Prism Pipeline active");
        }

        Toolkit.getToolkit().addShutdownHook(DriftFXSurface::destroy);
        initialized = true;
    }

    public static void destroy()
    {
        if (!initialized)
        {
            return;
        }
        Log.debug("Destroying NativeSurface system");
        GraphicsPipelineUtil.destroy();
    }

    @Deprecated
    @Override
    public void impl_updatePeer()
    {
        super.impl_updatePeer();
        NGDriftFXSurface peer = impl_getPeer();

        peer.setActive(isActive);
        if (isResizing())
        {
            peer.markDirty();
            peer.updateSize((float) getWidth(), (float) getHeight());
            Platform.runLater(() -> impl_markDirty(DirtyBits.NODE_GEOMETRY));
            Toolkit.getToolkit().requestNextPulse();
        }
        else
        {
            if (impl_isDirty(DirtyBits.NODE_GEOMETRY))
            {
                peer.markDirty();
                glExecutor.execute(() -> {
                    if (nativeResource.canvas != null)
                    {
                        nativeResource.canvas.setSurfaceSize((int) getWidth(), (int) getHeight());
                    }
                });
            }
        }

        if (impl_isDirty(DirtyBits.NODE_CONTENTS))
        {
            peer.markDirty();
        }
    }

    /**
     * Destroys native resources created for this render surface
     */
    public void close()
    {
        nativeResource.close();
    }

    @Override
    public boolean isResizable()
    {
        return true;
    }

    private void setResizeWaitTimestamp()
    {
        resizeWaitTimestamp = System.currentTimeMillis() + RESIZE_DELAY_MS;
    }

    private boolean isResizing()
    {
        return System.currentTimeMillis() < resizeWaitTimestamp;
    }

    @Override
    public void resize(double width, double height)
    {
        setWidth(width);
        setHeight(height);
        impl_markDirty(DirtyBits.NODE_GEOMETRY);
        setResizeWaitTimestamp();
    }

    private void dirty()
    {
        impl_markDirty(DirtyBits.NODE_CONTENTS);
        Toolkit.getToolkit().requestNextPulse();
    }

    private static Consumer<JNINativeSurface.FrameData> mCreateFramePresenter(DriftFXSurface pSurface)
    {
        AtomicReference<JNINativeSurface.FrameData> lCurrentFrame = new AtomicReference<>();
        WeakReference<DriftFXSurface> surfaceRef = new WeakReference<>(pSurface);
        return (frame) -> {
            DriftFXSurface surface = surfaceRef.get();
            if (surface != null && frame.width > 0 && frame.height > 0)
            {
                JNINativeSurface.FrameData lOldData = lCurrentFrame.getAndSet(frame);
                if (lOldData == null)
                {
                    Platform.runLater(() -> {
                        FrameData lFrame = lCurrentFrame.getAndSet(null);
                        NGDriftFXSurface ngSurface = surface.impl_getPeer();
                        ngSurface.present(lFrame);
                        surface.impl_markDirty(DirtyBits.NODE_CONTENTS);
                    });
                }
            }
        };
    }

    private static class DriftFXSurfaceGLListener implements GLEventListener
    {
        private int surfaceWidth;
        private int surfaceHeight;
        private int frameBuffer;
        private int glTexture;
        private final WeakReference<DriftFXSurface> surfaceRef;
        private GLAutoDrawable drawable;

        public DriftFXSurfaceGLListener(DriftFXSurface surface)
        {
            frameBuffer = -1;
            surfaceRef = new WeakReference<>(surface);
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
        {
            this.drawable = drawable;
            DriftFXSurface surface = surfaceRef.get();
            if (surface != null && surface.isActive && (surfaceWidth != width || surfaceHeight != height))
            {
                surfaceWidth = width;
                surfaceHeight = height;
                long nativeSurfaceId = surface.nativeResource.nativeSurfaceId;
                if (nativeSurfaceId >= 0)
                {
                    NativeAPI.release(nativeSurfaceId);
                    glTexture = NativeAPI.acquire(nativeSurfaceId, drawable.getContext().getHandle(), width, height);
                    if (glTexture >= 0 && frameBuffer >= 0)
                    {
                        int[] drawBuffer = new int[1];
                        drawable.getGL().glGetIntegerv(GL.GL_DRAW_FRAMEBUFFER_BINDING, drawBuffer, 0);
                        drawable.getGL().glBindFramebuffer(GL.GL_DRAW_FRAMEBUFFER, frameBuffer);
                        drawable.getGL().getGL3().glFramebufferTexture(GL.GL_DRAW_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
                                glTexture, 0);
                        drawable.getGL().glBindFramebuffer(GL.GL_DRAW_FRAMEBUFFER, drawBuffer[0]);
                    }
                }
            }
        }

        private void render(int textureId, int width, int height)
        {
            DriftFXSurface surf = surfaceRef.get();
            if (surf != null && surf.transferCallback != null && frameBuffer >= 0 && glTexture >= 0 && drawable != null)
            {
                drawable.getGL().glBindFramebuffer(GL.GL_DRAW_FRAMEBUFFER, frameBuffer);
                surf.transferCallback.render(drawable.getGL().getGL2ES3(), width, height);
                drawable.getGL().glFlush();
                Platform.runLater(surf::dirty);
            }
        }

        @Override
        public void init(GLAutoDrawable drawable)
        {
            int[] lFramebuffers = new int[1];
            drawable.getGL().glGenFramebuffers(1, lFramebuffers, 0);
            frameBuffer = lFramebuffers[0];
        }

        @Override
        public void dispose(GLAutoDrawable drawable)
        {
            if (frameBuffer >= 0)
            {
                drawable.getGL().glDeleteFramebuffers(1, new int[] { frameBuffer }, 0);
                frameBuffer = -1;
            }
            drawable = null;
        }

        @Override
        public void display(GLAutoDrawable drawable)
        {
            this.drawable = drawable;
            DriftFXSurface surface = surfaceRef.get();
            if (surface != null && surface.isActive && !surface.isResizing())
            {
                if (drawable.getSurfaceWidth() != surfaceWidth || drawable.getSurfaceHeight() != surfaceHeight)
                {
                    reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
                }
                long nativeSurfaceId = surface.nativeResource.nativeSurfaceId;
                if (nativeSurfaceId >= 0)
                {
                    NativeAPI.render(nativeSurfaceId, this::render);
                }
            }
        }
    }

    /**
     * A default executor in which to execute the opengl display calls
     */
    private static class PrivateExecutorService implements Executor
    {
        private static final ExecutorService service = Executors.newSingleThreadExecutor(run -> {
            Thread t = new Thread(run, "FX-DX-GL-SurfaceUpdater");
            t.setDaemon(true);
            return t;
        });

        @Override
        public void execute(Runnable command)
        {
            service.execute(command);
        }

        public void shutdown()
        {
            service.shutdown();
        }
    }

    /**
     * Holds all the required references to correctly release native resources when drift surface is garbage collected.
     * Implemented as a WeakReference to the surface to be notified when garbage collected, but we keep a hard reference
     * to the required cleanup resources from within the weak reference.
     */
    private static class DriftFXSurfaceNativeResource extends WeakReference<DriftFXSurface>
    {
        private static final ReferenceQueue<DriftFXSurface> refQueue = new ReferenceQueue<>();
        private static final Set<DriftFXSurfaceNativeResource> nativeResources = new HashSet<>();
        private long nativeSurfaceId;
        private GLOffscreenAutoDrawable canvas;
        private DriftFXSurfaceGLListener canvasListener;

        static
        {
            // Make sure to close any instance that were not closed correctly before being garbage collected
            Thread driftFXCleanupThread = new Thread(() -> {
                try
                {
                    while (true)
                    {
                        DriftFXSurfaceNativeResource removed = (DriftFXSurfaceNativeResource) refQueue.remove();
                        removed.close();
                    }
                }
                catch (InterruptedException e)
                {
                    // We need to end
                }
            }, "DriftFX-Cleanup-Thread");
            driftFXCleanupThread.setDaemon(true);
            driftFXCleanupThread.start();
        }

        private DriftFXSurfaceNativeResource(DriftFXSurface surface, JNINativeSurface jniNativeSurface)
        {
            super(surface, refQueue);
            nativeSurfaceId = NativeAPI.createNativeSurface(jniNativeSurface);
            nativeResources.add(this);
        }

        private void close()
        {
            if (nativeSurfaceId >= 0)
            {
                if (canvas != null)
                {
                    if (canvasListener != null)
                    {
                        canvas.disposeGLEventListener(canvasListener, true);
                    }
                    final long releaseId = nativeSurfaceId;
                    canvas.invoke(false, drawable -> {
                        NativeAPI.release(releaseId);
                        NativeAPI.destroyNativeSurface(releaseId);
                        return true;
                    });
                }
                nativeSurfaceId = -1;
            }

            canvas = null;
            canvasListener = null;
            nativeResources.remove(this);
        }
    }
}
