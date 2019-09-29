package io.agora.kit.media.dev;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;

import io.agora.kit.media.capture.VideoCapture;
import io.agora.kit.media.capture.VideoCaptureFactory;
import io.agora.kit.media.constant.Constant;
import io.agora.kit.media.video.channels.ChannelManager;
import io.agora.kit.media.video.comsumers.AgoraSurfaceView;
import io.agora.kit.media.video.preprocess.IPreprocessor;

public class TestActivity extends Activity {
    private VideoCapture mVideoCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewGroup container = (ViewGroup) getWindow().getDecorView();
        AgoraSurfaceView surfaceView = new AgoraSurfaceView(this);
        container.addView(surfaceView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        mVideoCapture = VideoCaptureFactory.createVideoCapture(this);
        mVideoCapture.allocate(1280, 1920, 24, Constant.CAMERA_FACING_FRONT);
        mVideoCapture.addPreprocessor(this, IPreprocessor.TYPE_FACE_UNITY);
        mVideoCapture.startCaptureMaybeAsync(false);
        mVideoCapture.connectChannel(ChannelManager.ChannelID.CAMERA);
    }

    @Override
    public void onDestroy() {
        mVideoCapture.disconnect();
        mVideoCapture.stopCaptureAndBlockUntilStopped();
        mVideoCapture.deallocate();
        super.onDestroy();
    }
}
