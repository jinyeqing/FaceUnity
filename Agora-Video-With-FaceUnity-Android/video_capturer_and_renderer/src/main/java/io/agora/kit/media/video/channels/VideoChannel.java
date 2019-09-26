package io.agora.kit.media.video.channels;

import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.gles.ProgramTexture2d;
import io.agora.kit.media.gles.ProgramTextureOES;
import io.agora.kit.media.gles.core.EglCore;
import io.agora.kit.media.gles.core.GlUtil;
import io.agora.kit.media.video.comsumers.EglContextCore;
import io.agora.kit.media.video.comsumers.IVideoConsumer;
import io.agora.kit.media.video.producers.IVideoProducer;

public class VideoChannel extends HandlerThread {
    public static final int NO_ERROR = 0;
    public static final int INFO_PRODUCER_EXIST = -1;
    public static final int INFO_CONSUMER_EXIST = -2;

    private int mChannelId;
    private boolean mOffScreenMode = true;

    private IVideoProducer mProducer;
    private IVideoConsumer mOnScreenConsumer;
    private List<IVideoConsumer> mOffScreenConsumers = new ArrayList<>();
    private Handler mHandler;

    private EglContextCore mEglContextCore;
    private EGLSurface mEglSurface;

    VideoChannel(int id) {
        super(ChannelManager.ChannelID.toString(id));
        mChannelId = id;
    }

    @Override
    public void run() {
        init();
        super.run();
        releaseGLContext();
    }

    private void init() {
        createGLContext();
    }

    private void createGLContext() {
        Log.i("VideoProducer","createGLContext");
        mEglContextCore = new EglContextCore();
        mEglContextCore.eglCore = new EglCore();

        // Create dummy surface
        mEglSurface = mEglContextCore.eglCore.createOffscreenSurface(1, 1);
        mEglContextCore.eglCore.makeCurrent(mEglSurface);

        mEglContextCore.mFullFrameRectTexture2D = new ProgramTexture2d();
        mEglContextCore.mTextureOES = new ProgramTextureOES();
    }

    private void releaseGLContext() {
        mEglContextCore.mFullFrameRectTexture2D.release();
        mEglContextCore.mTextureOES.release();

        if (mEglSurface != null && mEglSurface != EGL14.EGL_NO_SURFACE) {
            mEglContextCore.eglCore.releaseSurface(mEglSurface);
        }
        mEglContextCore.eglCore.release();
    }

    void startChannel() {
        if (isRunning()) {
            return;
        }
        start();
        mHandler = new Handler(getLooper());
    }

    public Handler getHandler() {
        checkThreadRunningState();
        return mHandler;
    }

    void stopChannel() {
        if (mProducer != null) {
            mProducer.disconnect();
            mProducer = null;
        }

        if (mOnScreenConsumer != null) {
            mOnScreenConsumer.disconnectChannel(mChannelId);
            mOnScreenConsumer = null;
        }

        if (!mOffScreenConsumers.isEmpty()) {
            for (IVideoConsumer consumer : mOffScreenConsumers) {
                consumer.disconnectChannel(mChannelId);
            }
        }

        mOffScreenConsumers.clear();
        quitSafely();
        mHandler = null;
    }

    public boolean isRunning() {
        return isAlive();
    }

    int connectProducer(IVideoProducer producer) {
        checkThreadRunningState();
        if (mProducer != null) {
            return INFO_PRODUCER_EXIST;
        }

        mProducer = producer;
        return NO_ERROR;
    }

    public int disconnectProducer(IVideoProducer producer) {
        checkThreadRunningState();
        mProducer = null;
        return NO_ERROR;
    }

    int connectConsumer(IVideoConsumer consumer, int type) {
        checkThreadRunningState();

        if (type == IVideoConsumer.TYPE_ON_SCREEN) {
            if (mOnScreenConsumer == null) {
                mOnScreenConsumer = consumer;
            }
        } else if (type == IVideoConsumer.TYPE_OFF_SCREEN) {
            if (!mOffScreenConsumers.contains(consumer)) {
                mOffScreenConsumers.add(consumer);
            }
        }

        return NO_ERROR;
    }


    int disconnectConsumer(IVideoConsumer consumer) {
        checkThreadRunningState();
        if (consumer == mOnScreenConsumer) {
            mOnScreenConsumer = null;
            if (!mOffScreenMode) {
                mOffScreenConsumers.clear();
            }
        } else {
            mOffScreenConsumers.remove(consumer);
        }
        return NO_ERROR;
    }

    public void pushVideoFrame(VideoCaptureFrame frame) {
        checkThreadRunningState();

        if (mOnScreenConsumer != null) {
            mOnScreenConsumer.onConsumeFrame(frame, mEglContextCore);
        }

        for (IVideoConsumer consumer : mOffScreenConsumers) {
            consumer.onConsumeFrame(frame, mEglContextCore);
        }
    }

    public EglContextCore getEGLContext() {
        return mEglContextCore;
    }

    private void checkThreadRunningState() {
        if (!isAlive()) {
            throw new IllegalStateException("Video Channel is not alive");
        }
    }

    void enableOffscreenMode(boolean enabled) {
        mOffScreenMode = enabled;
    }
}
