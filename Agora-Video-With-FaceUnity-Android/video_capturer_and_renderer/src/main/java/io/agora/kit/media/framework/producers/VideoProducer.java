package io.agora.kit.media.framework.producers;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.util.Log;

import com.sensetime.effects.glutils.GlUtil;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.framework.VideoModule;
import io.agora.kit.media.framework.channels.VideoChannel;
import io.agora.kit.media.framework.preprocess.IPreprocessor;
import io.agora.kit.media.framework.preprocess.Preprocessor;

public abstract class VideoProducer implements IVideoProducer {
    private static final String TAG = VideoProducer.class.getSimpleName();

    protected VideoChannel videoChannel;
    private volatile Handler handler;

    private Context mContext;

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
                //TODO workaround for setting the texture type
                frame.mFormat.setPixelFormat(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

                final VideoCaptureFrame out = new VideoCaptureFrame(frame);
                try {
                    frame.mSurfaceTexture.updateTexImage();
                    frame.mSurfaceTexture.getTransformMatrix(out.mTexMatrix);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                if (videoChannel != null) {
                    videoChannel.pushVideoFrame(out);
                }
            }
        });
    }

    @Override
    public void disconnect() {
        Log.i(TAG, "disconnect");
        handler = null;

        if (videoChannel != null) {
            videoChannel.disconnectProducer();
            videoChannel = null;
        }
    }

    public void setContext(Context context) {
        mContext = context;
    }
}
