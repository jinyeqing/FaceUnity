package io.agora.kit.media.framework.comsumers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

import io.agora.kit.media.framework.VideoModule;
import io.agora.kit.media.framework.channels.ChannelManager;
import io.agora.kit.media.framework.channels.VideoChannel;

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
        VideoModule.instance().setContext(getContext().getApplicationContext(),
                ChannelManager.ChannelID.CAMERA);
        connect();
    }

    public void connect() {
        mConsumer = new SurfaceViewConsumer(VideoModule.instance());
        mConsumer.setSurfaceView(this);
        mConsumer.surfaceCreated(getHolder());
        getHolder().addCallback(mConsumer);
    }

    public void disconnect() {
        // mConsumer.disconnectChannel(ChannelManager.ChannelID.CAMERA);
        mConsumer.surfaceDestroyed(getHolder());
        mConsumer.setSurfaceView(null);
        getHolder().removeCallback(mConsumer);
        mConsumer = null;
    }
}
