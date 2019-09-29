package io.agora.kit.media.video.channels;

import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.List;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.gles.ProgramTexture2d;
import io.agora.kit.media.gles.ProgramTextureOES;
import io.agora.kit.media.gles.core.EglCore;
import io.agora.kit.media.video.comsumers.IVideoConsumer;
import io.agora.kit.media.video.producers.IVideoProducer;

public class VideoChannel extends HandlerThread {
    private int mChannelId;
    private boolean mOffScreenMode = true;

    private IVideoProducer mProducer;
    private IVideoConsumer mOnScreenConsumer;
    private List<IVideoConsumer> mOffScreenConsumers = new ArrayList<>();
    private Handler mHandler;

    private ChannelContext mContext;
    private EGLSurface mEglSurface;

    VideoChannel(int id) {
        super(ChannelManager.ChannelID.toString(id));
        mChannelId = id;
    }

    @Override
    public void run() {
        init();
        super.run();
        release();
    }

    private void init() {
        EglCore eglCore = new EglCore();
        mContext = new ChannelContext();
        mContext.setEglCore(eglCore);

        mEglSurface = eglCore.createOffscreenSurface(1, 1);
        eglCore.makeCurrent(mEglSurface);

        mContext.setProgram2D(new ProgramTexture2d());
        mContext.setProgramOES(new ProgramTextureOES());
    }

    private void release() {
        mContext.getProgram2D().release();
        mContext.getProgramOES().release();
        mContext.getEglCore().releaseSurface(mEglSurface);
        mContext.getEglCore().release();
        mContext = null;
    }

    public ChannelContext getChannelContext() {
        return mContext;
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
        mHandler = null;
        quitSafely();
    }

    public boolean isRunning() {
        return isAlive();
    }

    void connectProducer(IVideoProducer producer) {
        checkThreadRunningState();
        if (mProducer == null) {
            mProducer = producer;
        }
    }

    public void disconnectProducer() {
        checkThreadRunningState();
        mProducer = null;
    }

    void connectConsumer(IVideoConsumer consumer, int type) {
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
    }

    void disconnectConsumer(IVideoConsumer consumer) {
        checkThreadRunningState();
        if (consumer == mOnScreenConsumer) {
            mOnScreenConsumer = null;
            if (!mOffScreenMode) {
                mOffScreenConsumers.clear();
            }
        } else {
            mOffScreenConsumers.remove(consumer);
        }
    }

    public void pushVideoFrame(VideoCaptureFrame frame) {
        checkThreadRunningState();

        if (mOnScreenConsumer != null) {
            mOnScreenConsumer.onConsumeFrame(frame, mContext);
        }

        for (IVideoConsumer consumer : mOffScreenConsumers) {
            consumer.onConsumeFrame(frame, mContext);
        }
    }

    private void checkThreadRunningState() {
        if (!isAlive()) {
            throw new IllegalStateException("Video Channel is not alive");
        }
    }

    void enableOffscreenMode(boolean enabled) {
        mOffScreenMode = enabled;
    }

    public static class ChannelContext {
        private EglCore mEglCore;
        private ProgramTexture2d mProgram2D;
        private ProgramTextureOES mProgramOES;

        public EglCore getEglCore() {
            return mEglCore;
        }

        private void setEglCore(EglCore mEglCore) {
            this.mEglCore = mEglCore;
        }

        public ProgramTexture2d getProgram2D() {
            return mProgram2D;
        }

        private void setProgram2D(ProgramTexture2d mFullFrameRectTexture2D) {
            this.mProgram2D = mFullFrameRectTexture2D;
        }

        public ProgramTextureOES getProgramOES() {
            return mProgramOES;
        }

        private void setProgramOES(ProgramTextureOES mTextureOES) {
            this.mProgramOES = mTextureOES;
        }
    }
}
