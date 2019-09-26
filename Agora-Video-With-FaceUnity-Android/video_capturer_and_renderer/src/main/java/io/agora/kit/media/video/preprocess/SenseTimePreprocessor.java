package io.agora.kit.media.video.preprocess;

import android.content.Context;

import io.agora.kit.media.capture.VideoCaptureFrame;

public class SenseTimePreprocessor implements IPreprocessor {

    public SenseTimePreprocessor(Context context) {
        initPreprocessor();
    }

    @Override
    public void onPreProcessFrame(VideoCaptureFrame inFrame, VideoCaptureFrame outFrame) {

    }

    @Override
    public int getType() {
        return TYPE_SENSE_TIME;
    }

    @Override
    public void initPreprocessor() {

    }

    @Override
    public void recyclePreprocessor() {

    }
}
