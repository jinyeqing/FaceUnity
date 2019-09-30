package io.agora.kit.media.video.comsumers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

import io.agora.kit.media.video.VideoModule;

public class AgoraSurfaceView extends SurfaceView {
    private SurfaceViewConsumer mConsumer;

    public AgoraSurfaceView(Context context) {
        super(context);
        init();
    }

    public AgoraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mConsumer = new SurfaceViewConsumer(VideoModule.instance());
        mConsumer.setSurfaceView(this);
        getHolder().addCallback(mConsumer);
    }
}
