package io.agora.kit.media.framework;

import android.content.Context;

import io.agora.kit.media.framework.channels.ChannelManager;
import io.agora.kit.media.framework.channels.VideoChannel;
import io.agora.kit.media.framework.comsumers.IVideoConsumer;
import io.agora.kit.media.framework.producers.IVideoProducer;

public class VideoModule {
    private static final String TAG = VideoModule.class.getSimpleName();

    private static VideoModule mSelf;
    private ChannelManager mChannelManager = new ChannelManager();

    public static VideoModule instance() {
        if (mSelf == null) {
            mSelf = new VideoModule();
        }

        return mSelf;
    }

    private VideoModule() {

    }

    public void setContext(Context context, int channelId) {
        mChannelManager.setContext(context, channelId);
    }

    public VideoChannel connectProducer(IVideoProducer producer, int id) {
        return mChannelManager.connectProducer(producer, id);
    }

    public VideoChannel connectConsumer(IVideoConsumer consumer, int id, int type) {
        return mChannelManager.connectConsumer(consumer, id, type);
    }

    public void disconnectProducer(IVideoProducer producer, int id) {
        mChannelManager.disconnectProducer(id);
    }

    public void disconnectConsumer(IVideoConsumer consumer, int id) {
        mChannelManager.disconnectConsumer(consumer, id);
    }

    public void stopChannel(int channelId) {
        mChannelManager.stopChannel(channelId);
    }

    /**
     * Enable the channel to capture video frames and do
     * offscreen rendering without the onscreen consumer
     * (Like SurfaceView or TextureView).
     * The default is true.
     * @param channelId
     * @param enabled
     */
    public void enableOffscreenMode(int channelId, boolean enabled) {
        mChannelManager.enableOffscreenMode(channelId, enabled);
    }
}
