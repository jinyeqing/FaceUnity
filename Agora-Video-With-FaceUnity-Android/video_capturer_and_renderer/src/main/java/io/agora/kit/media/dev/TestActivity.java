package io.agora.kit.media.dev;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import io.agora.kit.media.R;
import io.agora.kit.media.capture.VideoCapture;
import io.agora.kit.media.capture.VideoCaptureFactory;
import io.agora.kit.media.constant.Constant;
import io.agora.kit.media.framework.VideoModule;
import io.agora.kit.media.framework.channels.ChannelManager;
import io.agora.kit.media.framework.comsumers.AgoraSurfaceView;
import io.agora.kit.media.framework.comsumers.BaseWindowConsumer;
import io.agora.kit.media.framework.preprocess.IPreprocessor;

public class TestActivity extends Activity {
    private VideoCapture mVideoCapture;
    private boolean mConnected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test_layout);

        //final AgoraSurfaceView surfaceView = findViewById(R.id.main_surface_view);
        //AgoraTextureView textureView = findViewById(R.id.main_texture_view);

        mVideoCapture = VideoCaptureFactory.createVideoCapture(this);
        mVideoCapture.allocate(640, 480, 24, Constant.CAMERA_FACING_FRONT);
        mVideoCapture.setContext(this);
        mVideoCapture.startCaptureMaybeAsync(false);
        mVideoCapture.connectChannel(ChannelManager.ChannelID.CAMERA);

        Log.i("TestActivity", "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.i("TestActivity", "onDestroy");
        mVideoCapture.disconnect();
        mVideoCapture.stopCaptureAndBlockUntilStopped();
        mVideoCapture.deallocate();
        //VideoModule.instance().stopChannel(ChannelManager.ChannelID.CAMERA);
        super.onDestroy();
    }
}
