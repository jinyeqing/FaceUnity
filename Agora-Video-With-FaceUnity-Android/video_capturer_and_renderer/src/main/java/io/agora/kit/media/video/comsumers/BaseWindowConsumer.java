package io.agora.kit.media.video.comsumers;

import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.gles.core.EglCore;
import io.agora.kit.media.gles.core.GlUtil;
import io.agora.kit.media.video.VideoModule;
import io.agora.kit.media.video.channels.ChannelManager;
import io.agora.kit.media.video.channels.VideoChannel;
import io.agora.kit.media.video.channels.VideoChannel.ChannelContext;

public abstract class BaseWindowConsumer implements IVideoConsumer {
    protected static final int CHANNEL_ID = ChannelManager.ChannelID.CAMERA;
    public static boolean DEBUG = false;

    protected VideoModule videoModule;
    protected VideoChannel videoChannel;

    private EGLSurface drawingEglSurface;
    protected volatile boolean shouldCreateDrawingSurface = true;
    private float[] mMVPMatrix = new float[16];
    private boolean mMVPInit;

    public BaseWindowConsumer(VideoModule videoModule) {
        this.videoModule = videoModule;
    }

    @Override
    public void connectChannel(int channelId) {
        videoChannel = videoModule.connectConsumer(this, channelId, IVideoConsumer.TYPE_ON_SCREEN);
    }

    @Override
    public void disconnectChannel(int channelId) {
        videoModule.disconnectConsumer(this, channelId);
    }

    @Override
    public void onConsumeFrame(VideoCaptureFrame frame, VideoChannel.ChannelContext context) {
        drawFrame(frame, context);
    }

    private void drawFrame(VideoCaptureFrame frame, ChannelContext context) {
        EglCore eglCore = context.getEglCore();

        if (shouldCreateDrawingSurface) {
            if (drawingEglSurface != null) {
                eglCore.releaseSurface(drawingEglSurface);
                eglCore.makeNothingCurrent();
                drawingEglSurface = null;
            }

            drawingEglSurface = eglCore.createWindowSurface(onGetDrawingTarget());
            shouldCreateDrawingSurface = false;
        }

        if (!eglCore.isCurrent(drawingEglSurface)) {
            eglCore.makeCurrent(drawingEglSurface);
        }

        int width = onMeasuredWidth();
        int height = onMeasuredHeight();
        GLES20.glViewport(0, 0, width, height);

        if (!mMVPInit) {
            mMVPMatrix = GlUtil.changeMVPMatrix(GlUtil.IDENTITY_MATRIX,
                    width, height, frame.mFormat.getHeight(),
                    frame.mFormat.getWidth());
            mMVPInit = true;
        }

        if (frame.mFormat.getPixelFormat() == GLES20.GL_TEXTURE_2D) {
            context.getProgram2D().drawFrame(
                    frame.mTextureId, frame.mTexMatrix, mMVPMatrix);
        } else if (frame.mFormat.getPixelFormat() == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
            context.getProgramOES().drawFrame(
                    frame.mTextureId, frame.mTexMatrix, mMVPMatrix);
        }

        eglCore.swapBuffers(drawingEglSurface);
    }
}
