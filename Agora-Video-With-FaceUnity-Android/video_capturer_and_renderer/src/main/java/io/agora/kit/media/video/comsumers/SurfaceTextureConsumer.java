package io.agora.kit.media.video.comsumers;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.video.VideoModule;
import io.agora.kit.media.video.channels.ChannelManager;

public class SurfaceTextureConsumer implements IVideoConsumer, TextureView.SurfaceTextureListener {
    private static final String TAG = SurfaceTextureConsumer.class.getSimpleName();
    private static final int CHANNEL_ID = ChannelManager.ChannelID.CAMERA;

    private VideoModule mVideoModule;

    public SurfaceTextureConsumer(VideoModule videoModule) {
        mVideoModule = videoModule;
    }

    @Override
    public void onConsumeFrame(VideoCaptureFrame frame, EglContextCore context) {

    }

    @Override
    public void connectChannel(int channelId) {
        mVideoModule.connectConsumer(this, channelId, IVideoConsumer.TYPE_ON_SCREEN);
    }

    @Override
    public void disconnectChannel(int channelId) {
        mVideoModule.disconnectConsumer(this, channelId);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable");
        connectChannel(CHANNEL_ID);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i(TAG, "onSurfaceTextureDestroyed");
        disconnectChannel(CHANNEL_ID);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.i(TAG, "onSurfaceTextureUpdated");
    }
}
