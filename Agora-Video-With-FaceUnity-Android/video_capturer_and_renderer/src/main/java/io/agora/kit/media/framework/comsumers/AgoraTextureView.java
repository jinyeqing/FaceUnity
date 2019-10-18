package io.agora.kit.media.framework.comsumers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

import io.agora.kit.media.framework.VideoModule;
import io.agora.kit.media.framework.channels.ChannelManager;

public class AgoraTextureView extends TextureView {
    private SurfaceTextureConsumer mConsumer;

    public AgoraTextureView(Context context) {
        super(context);
        init();
    }

    public AgoraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        VideoModule.instance().setContext(getContext().getApplicationContext(),
                ChannelManager.ChannelID.CAMERA);
        mConsumer = new SurfaceTextureConsumer(VideoModule.instance());
        setSurfaceTextureListener(mConsumer);
    }
}
