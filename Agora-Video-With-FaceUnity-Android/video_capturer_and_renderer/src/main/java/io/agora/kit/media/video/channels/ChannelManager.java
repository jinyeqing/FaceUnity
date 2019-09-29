package io.agora.kit.media.video.channels;

import io.agora.kit.media.video.comsumers.IVideoConsumer;
import io.agora.kit.media.video.producers.IVideoProducer;

public class ChannelManager {
    public static final String TAG = ChannelManager.class.getSimpleName();
    private static final int CHANNEL_COUNT = 3;

    public static class ChannelID {
        public static final int CAMERA = 0;
        public static final int SCREEN_SHARE = 1;
        public static final int CUSTOM = 2;

        public static String toString(int id) {
            switch (id) {
                case CAMERA: return "camera_channel";
                case SCREEN_SHARE: return "ScreenShare_channel";
                case CUSTOM: return "custom_channel";
                default: return "undefined_channel";
            }
        }
    }

    private VideoChannel[] mChannels = new VideoChannel[CHANNEL_COUNT];

    public VideoChannel connectProducer(IVideoProducer producer, int id) {
        ensureChannelState(id);
        mChannels[id].connectProducer(producer);
        return mChannels[id];
    }

    public void disconnectProducer(int id) {
        ensureChannelState(id);
        mChannels[id].disconnectProducer();
    }

    public VideoChannel connectConsumer(IVideoConsumer consumer, int id, int type) {
        ensureChannelState(id);
        mChannels[id].connectConsumer(consumer, type);
        return mChannels[id];
    }

    public void disconnectConsumer(IVideoConsumer consumer, int id) {
        ensureChannelState(id);
        mChannels[id].disconnectConsumer(consumer);
    }

    private void ensureChannelState(int channelId) {
        if (channelId < ChannelID.CAMERA || channelId > ChannelID.CUSTOM) {
            throw new IllegalArgumentException(
                    "[ChannelManager] wrong argument: Undefined channel id");
        }

        if (mChannels[channelId] == null) {
            mChannels[channelId] = new VideoChannel(channelId);
        }

        if (!mChannels[channelId].isRunning()) {
            mChannels[channelId].startChannel();
        }
    }

    public void stopChannel(int channelId) {
        if (channelId < ChannelID.CAMERA || channelId > ChannelID.CUSTOM) {
            throw new IllegalArgumentException(
                    "[ChannelManager] wrong argument: Undefined channel id");
        }

        mChannels[channelId].stopChannel();
    }

    public void enableOffscreenMode(int channelId, boolean enable) {
        ensureChannelState(channelId);
        mChannels[channelId].enableOffscreenMode(enable);
    }
}
