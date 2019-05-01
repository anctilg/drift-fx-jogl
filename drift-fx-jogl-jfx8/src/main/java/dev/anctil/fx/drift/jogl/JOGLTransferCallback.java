package dev.anctil.fx.drift.jogl;

import com.jogamp.opengl.GL2ES3;

public interface JOGLTransferCallback
{
    /**
     * Called when the canvas has displayed and is ready to transfer images to the
     * DX texture. Within this callback, the current write framebuffer is one that
     * is attached to the DX texture, the user simply has to bind to their read
     * framebuffer glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, ...); then blit to the
     * currently bound write buffer glBlitFramebuffer(0, 0, ..., ..., 0, 0, width,
     * height, GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);
     *
     * @param width  the width of the texture
     * @param height the height of the texture
     */
    void render(GL2ES3 gl, int width, int height);
}