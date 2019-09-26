package io.agora.kit.media.video.comsumers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

import io.agora.kit.media.video.VideoModule;

public class AgoraSurfaceView extends SurfaceView {
    public AgoraSurfaceView(Context context) {
        super(context);
        init();
    }

    public AgoraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        SurfaceViewConsumer consumer = new SurfaceViewConsumer(VideoModule.instance());
        consumer.setSurfaceView(this);
        getHolder().addCallback(consumer);
    }
}
