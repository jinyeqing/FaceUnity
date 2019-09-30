package io.agora.kit.media.video.producers;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.os.Handler;
import android.util.Log;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.video.VideoModule;
import io.agora.kit.media.video.channels.VideoChannel;
import io.agora.kit.media.video.preprocess.IPreprocessor;
import io.agora.kit.media.video.preprocess.Preprocessor;

public abstract class VideoProducer implements IVideoProducer {
    protected VideoChannel videoChannel;
    private Handler handler;
    private IPreprocessor mPreprocessor;

    private Context mContext;
    private int mPreprocessorType = IPreprocessor.TYPE_NONE;

    @Override
    public void connectChannel(int channelId) {
        videoChannel = VideoModule.instance().connectProducer(this, channelId);
        handler = videoChannel.getHandler();
    }

    @Override
    public void pushVideoFrame(final VideoCaptureFrame frame) {
        if (handler == null) {
            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                frame.mFormat.setPixelFormat(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                final VideoCaptureFrame out =
                        new VideoCaptureFrame(frame);

                if (mPreprocessor == null &&
                        mPreprocessorType != IPreprocessor.TYPE_NONE) {
                    mPreprocessor = Preprocessor.createPreprocessor(mContext, mPreprocessorType);
                }

                if (mPreprocessor != null) {
                    mPreprocessor.onPreProcessFrame(frame, out);
                }

                if (videoChannel != null) {
                    videoChannel.pushVideoFrame(out);
                }
            }
        });
    }

    @Override
    public void disconnect() {
        if (mPreprocessor != null) {
            mPreprocessor.recyclePreprocessor();
        }

        if (videoChannel != null) {
            videoChannel.disconnectProducer();
            videoChannel = null;
            handler = null;
        }
    }

    public void addPreprocessor(Context context, int type) {
        mContext = context;
        mPreprocessorType = type;
    }
}
