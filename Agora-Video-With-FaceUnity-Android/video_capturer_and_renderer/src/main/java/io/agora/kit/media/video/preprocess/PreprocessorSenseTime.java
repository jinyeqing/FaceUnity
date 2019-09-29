package io.agora.kit.media.video.preprocess;

import android.content.Context;

import io.agora.kit.media.capture.VideoCaptureFrame;

public class PreprocessorSenseTime implements IPreprocessor {

    public PreprocessorSenseTime(Context context) {
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
