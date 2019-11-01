package io.agora.kit.media.dev;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import io.agora.kit.media.R;
import io.agora.kit.media.capture.VideoCapture;
import io.agora.kit.media.capture.VideoCaptureFactory;
import io.agora.kit.media.constant.Constant;
import io.agora.kit.media.framework.VideoModule;
import io.agora.kit.media.framework.channels.ChannelManager;
import io.agora.kit.media.framework.preprocess.IPreprocessor;
import io.agora.kit.media.framework.preprocess.PreprocessorSenseTime;
import views.effects.EffectOptionsLayout;

public class TestActivity extends Activity {
    private static final String TAG = TestActivity.class.getSimpleName();

    private RelativeLayout mLayout;

    private VideoCapture mVideoCapture;
    private IPreprocessor mPreprocessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test_layout);


        VideoModule.instance().init(this);
        //final AgoraSurfaceView surfaceView = findViewById(R.id.main_surface_view);
        //AgoraTextureView textureView = findViewById(R.id.main_texture_view);

        mVideoCapture = VideoCaptureFactory.createVideoCapture(this);
        mVideoCapture.allocate(640, 480, 24, Constant.CAMERA_FACING_FRONT);
        mVideoCapture.setContext(this);
        mVideoCapture.startCaptureMaybeAsync(false);
        mVideoCapture.connectChannel(ChannelManager.ChannelID.CAMERA);

        mLayout = findViewById(R.id.app_layout);
        EffectOptionsLayout optionsLayout = new EffectOptionsLayout(this);
        PreprocessorSenseTime preprocessor = (PreprocessorSenseTime) VideoModule.
                instance().getPreprocessor(ChannelManager.ChannelID.CAMERA);
        // The Preprocessor responds to the option selection
        optionsLayout.setOnBeautyParamSelectedListener(preprocessor);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

        mLayout.addView(optionsLayout, params);
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
