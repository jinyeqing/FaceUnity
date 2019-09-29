package io.agora.kit.media.video.comsumers;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.video.channels.VideoChannel.ChannelContext;

public interface IVideoConsumer {
    int TYPE_ON_SCREEN = 0;
    int TYPE_OFF_SCREEN = 1;

    void onConsumeFrame(VideoCaptureFrame frame, ChannelContext context);
    void connectChannel(int channelId);
    void disconnectChannel(int channelId);
}
