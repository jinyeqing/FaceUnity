package io.agora.kit.media.video.preprocess;

import io.agora.kit.media.capture.VideoCaptureFrame;

public interface IPreprocessor {
    int TYPE_FACE_UNITY = 0;
    int TYPE_SENSE_TIME = 1;
    int TYPE_NONE = 2;

    void onPreProcessFrame(VideoCaptureFrame inFrame, VideoCaptureFrame outFrame);
    int getType();
    void initPreprocessor();
    void recyclePreprocessor();
}
