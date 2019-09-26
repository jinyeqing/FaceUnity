package io.agora.kit.media.video.producers.camera;

import android.content.Context;

import io.agora.kit.media.capture.VideoCapture;
import io.agora.kit.media.capture.VideoCaptureFactory;
import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.video.producers.IVideoProducer;

public class CameraProducer implements IVideoProducer {
    private VideoCapture mVideoCapture;

    public CameraProducer(Context context) {
        mVideoCapture = VideoCaptureFactory.createVideoCapture(context);
    }

    public CameraProducer(Context context, int width, int height, int fps, int facing) {
        this(context);
        mVideoCapture.allocate(width, height, fps, facing);
    }

    @Override
    public void connectChannel(int channelId) {

    }

    @Override
    public void pushVideoFrame(VideoCaptureFrame frame) {

    }

    @Override
    public void disconnect() {

    }
}
