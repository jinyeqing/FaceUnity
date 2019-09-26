package io.agora.kit.media.video.producers;

import io.agora.kit.media.capture.VideoCaptureFrame;

public interface IVideoProducer {
    void connectChannel(int channelId);
    void pushVideoFrame(VideoCaptureFrame frame);
    void disconnect();
}
