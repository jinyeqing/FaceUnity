package io.agora.kit.media.video;

import io.agora.kit.media.video.channels.ChannelManager;
import io.agora.kit.media.video.channels.VideoChannel;
import io.agora.kit.media.video.comsumers.IVideoConsumer;
import io.agora.kit.media.video.producers.IVideoProducer;

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

    public VideoChannel connectProducer(IVideoProducer producer, int id) {
        return mChannelManager.connectProducer(producer, id);
    }

    public void disconnectProducer(IVideoProducer producer, int id) {
        mChannelManager.disconnectProducer(producer, id);
    }

    public VideoChannel connectConsumer(IVideoConsumer consumer, int id, int type) {
        return mChannelManager.connectConsumer(consumer, id, type);
    }

    public void disconnectConsumer(IVideoConsumer consumer, int id) {
        mChannelManager.disconnectConsumer(consumer, id);
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
