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
import java.util.function.Consumer;

import com.jogamp.opengl.*;
import com.sun.javafx.geom.*;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.*;
import com.sun.javafx.scene.NodeHelper.NodeAccessor;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.Toolkit;

import dev.anctil.fx.drift.jogl.impl.NGDriftFXSurface;
import dev.anctil.fx.drift.jogl.internal.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.Node;

public class DriftFXSurface extends Node
{

    private JOGLTransferCallback transferCallback;
    private boolean isActive;
    private Executor glExecutor = new PrivateExecutorService();
    private final DriftFXSurfaceNativeResource nativeResource;

    static
    {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        DriftFXSurfaceHelper.setDriftFXSurfaceAccessor(new DriftFXSurfaceHelper.DriftFXSurfaceAcessor()
        {
            @Override
            public NGNode doCreatePeer(Node node)
            {
                return ((DriftFXSurface) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node)
            {
                ((DriftFXSurface) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx)
            {
                return ((DriftFXSurface) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY)
            {
                return ((DriftFXSurface) node).doComputeContains(localX, localY);
            }
        });
    }

    {
        // To initialize the class helper at the beginning each constructor of this class
        DriftFXSurfaceHelper.initHelper(this);
    }

    public DriftFXSurface()
    {
        initialize();
        JNINativeSurface jni = new JNINativeSurface(mCreateFramePresenter(this));
        nativeResource = new DriftFXSurfaceNativeResource(this, jni);
    }

    private static Consumer<JNINativeSurface.FrameData> mCreateFramePresenter(DriftFXSurface pSurface)
    {
        WeakReference<DriftFXSurface> surfaceRef = new WeakReference<>(pSurface);
        return (frame) -> {
            DriftFXSurface surface = surfaceRef.get();
            if (surface != null)
            {
                NGDriftFXSurface ngSurface = (NGDriftFXSurface) DriftFXSurfaceHelper.getPeer(surface);
                ngSurface.present(frame);
                DriftFXSurfaceHelper.markDirty(surface, DirtyBits.NODE_CONTENTS);
            }
        };
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
        isActive = active;
    }

    private NGDriftFXSurface doCreatePeer()
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

    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx)
    {
        bounds = new RectBounds(0f, 0f, (float) getWidth(), (float) getHeight());
        bounds = tx.transform(bounds, bounds);
        return bounds;
    }

    private boolean doComputeContains(double localX, double localY)
    {
        double w = getWidth();
        double h = getHeight();
        return (w > 0 && h > 0 && localX >= 0 && localY >= 0 && localX < w && localY < h);
    }

    private void widthChanged(double value)
    {
        if (value != _width)
        {
            _width = value;

            NodeAccessor nodeAccessor = DriftFXSurfaceHelper.getNodeAccessor();
            nodeAccessor.doGeomChanged(this);
            nodeAccessor.doMarkDirty(this, DirtyBits.NODE_GEOMETRY);
        }
    }

    private double _width;
    private ReadOnlyDoubleWrapper width;

    public final double getWidth()
    {
        return width == null ? _width : width.get();
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

            NodeAccessor nodeAccessor = DriftFXSurfaceHelper.getNodeAccessor();
            nodeAccessor.doGeomChanged(this);
            nodeAccessor.doMarkDirty(this, DirtyBits.NODE_GEOMETRY);
        }
    }

    private double _height;
    private ReadOnlyDoubleWrapper height;

    public final double getHeight()
    {
        return height == null ? _height : height.get();
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

    private static boolean initialized = false;

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

    private void doUpdatePeer()
    {
        final NGDriftFXSurface peer = NodeHelper.getPeer(this);
        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY))
        {
            peer.updateSize((float) getWidth(), (float) getHeight());
            peer.markDirty();
            glExecutor.execute(() -> {
                if (nativeResource.canvas != null)
                {
                    nativeResource.canvas.setSurfaceSize((int) getWidth(), (int) getHeight());
                }
            });
        }
        if (NodeHelper.isDirty(this, DirtyBits.NODE_CONTENTS))
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

    @Override
    public void resize(double width, double height)
    {
        setWidth(width);
        setHeight(height);
        NodeAccessor nodeAccessor = DriftFXSurfaceHelper.getNodeAccessor();
        nodeAccessor.doMarkDirty(this, DirtyBits.NODE_GEOMETRY);
    }

    public void dirty()
    {
        NodeAccessor nodeAccessor = DriftFXSurfaceHelper.getNodeAccessor();
        nodeAccessor.doMarkDirty(this, DirtyBits.NODE_CONTENTS);
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
            if (surface != null && surface.isActive)
            {
                if (drawable.getSurfaceWidth() != surfaceWidth || drawable.getSurfaceHeight() != surfaceHeight)
                {
                    reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
                }
                long nativeSurfaceId = surface.nativeResource.nativeSurfaceId;
                if (nativeSurfaceId >= 0)
                {
                    NativeAPI.render(nativeSurfaceId, this::render);
                    Platform.runLater(surface::dirty);
                }
            }
        }
    }

    private static class PrivateExecutorService implements Executor
    {
        private ExecutorService service = Executors.newSingleThreadExecutor(run -> {
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
                NativeAPI.release(nativeSurfaceId);
                NativeAPI.destroyNativeSurface(nativeSurfaceId);
                nativeSurfaceId = -1;
            }
            if (canvas != null && canvasListener != null)
            {
                canvas.disposeGLEventListener(canvasListener, true);
            }
            canvas = null;
            canvasListener = null;
            nativeResources.remove(this);
        }
    }
}
