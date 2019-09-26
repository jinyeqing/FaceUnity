package io.agora.kit.media.video.comsumers;

import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.faceunity.FURenderer;
import com.faceunity.entity.CartoonFilter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.gles.ProgramTexture2d;
import io.agora.kit.media.gles.ProgramTextureOES;
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

    private EglCore mEglCore;
    private EGLSurface mSurface;
    private float[] mTexMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private boolean mMVPInit;
    private ProgramTexture2d mFullFrameRectTexture2D;
    private ProgramTextureOES mTextureOES;

    private boolean mNeedDraw;
    private SurfaceRenderThread mRenderThread;
    private Handler mHandler;
    // private VideoCaptureFrame mFrame;

//    private FURenderer mFURenderer;

    public SurfaceViewConsumer(VideoModule videoModule) {
        mVideoModule = videoModule;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
//        mFURenderer = new FURenderer
//                .Builder(mSurfaceView.getContext())
//                .maxFaces(4)
//                .createEGLContext(false)
//                .setNeedFaceBeauty(true)
//                .inputTextureType(FURenderer.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE)
//                .build();
//        mFURenderer.onSurfaceCreated();
//        mFURenderer.onCartoonFilterSelected(CartoonFilter.PENCIL_PAINTING);
    }

    @Override
    public void onConsumeFrame(final VideoCaptureFrame frame, EglContextCore context) {
        mNeedDraw = true;
        Log.i("VideoProducer","onConsumeFrame"+ context.eglCore.getEGLContext());
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
                if (!mNeedDraw) {
                    return;
                }

                if (mSurfaceView == null) {
                    return;
                }

                if (mSurface == null || mSurface == EGL14.EGL_NO_SURFACE) {
                   // mSurface = context.eglCore.createWindowSurface(mSurfaceView.getHolder().getSurface());
                    mSurface = context.eglCore.createWindowSurface(mSurfaceView.getHolder().getSurface());
                }

                context.eglCore.makeCurrent(mSurface);

                try {
                    frame.mSurfaceTexture.updateTexImage();
                    frame.mSurfaceTexture.getTransformMatrix(frame.mTexMatrix);
                } catch (Exception e) {
                    Log.e(TAG, "updateTexImage failed, ignore " + Log.getStackTraceString(e));
                    return;
                }

                if (!mMVPInit) {
                    mMVPMatrix = GlUtil.changeMVPMatrix(GlUtil.IDENTITY_MATRIX,
                            mViewWidth, mViewHeight, frame.mFormat.getHeight(),
                            frame.mFormat.getWidth());
                    mMVPInit = true;
                }

                if (frame.mFormat.getPixelFormat() == GLES20.GL_TEXTURE_2D) {
                    context.mFullFrameRectTexture2D.drawFrame(frame.mTextureId, frame.mTexMatrix, mMVPMatrix);
                } else if (frame.mFormat.getPixelFormat() == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
                    context.mTextureOES.drawFrame(frame.mTextureId, frame.mTexMatrix, mMVPMatrix);
                }

                context.eglCore.swapBuffers(mSurface);
//            }
//        });
    }

    @Override
    public void connectChannel(int channelId) {
        mVideoChannel = mVideoModule.connectConsumer(this, channelId, IVideoConsumer.TYPE_ON_SCREEN);
        //startRenderThread();
    }

    private void startRenderThread() {
        mRenderThread = new SurfaceRenderThread(TAG);
        mRenderThread.start();
        mHandler = new Handler(mRenderThread.getLooper());
    }

    @Override
    public void disconnectChannel(int channelId) {
        mVideoModule.disconnectConsumer(this, channelId);
    }

    private void disconnectChannel() {
        disconnectChannel(CHANNEL_ID);
        //mRenderThread.quitSafely();
        mHandler = null;
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
        disconnectChannel();
    }

    private class SurfaceRenderThread extends HandlerThread {
        SurfaceRenderThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            initEgl();
            Log.i(TAG, "Render thread run");
            super.run();
            Log.i(TAG, "Render thread quit");
            releaseEgl();
        }

        private void initEgl() {
            mEglCore = new EglCore(mVideoChannel.getEGLContext().eglCore.getEGLContext(), 0);
            GlUtil.checkGlError("create egl context");

            mSurface = mEglCore.createWindowSurface(mSurfaceView.getHolder().getSurface());
            GlUtil.checkGlError("create window surface");
            mEglCore.makeCurrent(mSurface);

            mFullFrameRectTexture2D = new ProgramTexture2d();
            mTextureOES = new ProgramTextureOES();
        }

        private void releaseEgl() {
            mFullFrameRectTexture2D.release();
            mTextureOES.release();
            mEglCore.releaseSurface(mSurface);
            mEglCore.makeNothingCurrent();
            mEglCore.release();
        }
    }
}
