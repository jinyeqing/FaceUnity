package io.agora.kit.media.video.comsumers;

import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.gles.core.EglCore;
import io.agora.kit.media.gles.core.GlUtil;
import io.agora.kit.media.video.VideoModule;
import io.agora.kit.media.video.channels.ChannelManager;
import io.agora.kit.media.video.channels.VideoChannel;

public class SurfaceViewConsumer implements IVideoConsumer, SurfaceHolder.Callback {
    private static final String TAG = SurfaceViewConsumer.class.getSimpleName();
    private static final int CHANNEL_ID = ChannelManager.ChannelID.CAMERA;

    private VideoModule mVideoModule;
    private VideoChannel mVideoChannel;
    private SurfaceView mSurfaceView;
    private int mViewWidth;
    private int mViewHeight;

    private EGLSurface mSurface;
    private float[] mMVPMatrix = new float[16];
    private boolean mMVPInit;

    public SurfaceViewConsumer(VideoModule videoModule) {
        mVideoModule = videoModule;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    @Override
    public void onConsumeFrame(final VideoCaptureFrame frame, VideoChannel.ChannelContext context) {
        if (mSurfaceView == null) {
            return;
        }

        EglCore eglCore = context.getEglCore();

        if (mSurface == null || mSurface == EGL14.EGL_NO_SURFACE) {
            mSurface = eglCore.createWindowSurface(mSurfaceView.getHolder().getSurface());
        }

        eglCore.makeCurrent(mSurface);
        GLES20.glViewport(0, 0, mSurfaceView.getMeasuredWidth(), mSurfaceView.getMeasuredHeight());

        if (!mMVPInit) {
            mMVPMatrix = GlUtil.changeMVPMatrix(GlUtil.IDENTITY_MATRIX,
                    mViewWidth, mViewHeight, frame.mFormat.getHeight(),
                    frame.mFormat.getWidth());
            mMVPInit = true;
        }

        if (frame.mFormat.getPixelFormat() == GLES20.GL_TEXTURE_2D) {
            context.getProgram2D().drawFrame(frame.mTextureId, frame.mTexMatrix, mMVPMatrix);
        } else if (frame.mFormat.getPixelFormat() == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
            context.getProgramOES().drawFrame(frame.mTextureId, frame.mTexMatrix, mMVPMatrix);
        }

        eglCore.swapBuffers(mSurface);
    }

    @Override
    public void connectChannel(int channelId) {
        mVideoChannel = mVideoModule.connectConsumer(this, channelId, IVideoConsumer.TYPE_ON_SCREEN);
    }

    @Override
    public void disconnectChannel(int channelId) {
        mVideoModule.disconnectConsumer(this, channelId);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        connectChannel(CHANNEL_ID);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mViewWidth = width;
        mViewHeight = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        disconnectChannel(CHANNEL_ID);
    }
}
