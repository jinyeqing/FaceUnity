package io.agora.kit.media.framework.comsumers;

import android.opengl.GLES20;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.framework.VideoModule;
import io.agora.kit.media.framework.channels.VideoChannel;

public class SurfaceViewConsumer extends BaseWindowConsumer implements SurfaceHolder.Callback {
    private static final String TAG = SurfaceViewConsumer.class.getSimpleName();

    private SurfaceView mSurfaceView;

    public SurfaceViewConsumer(VideoModule videoModule) {
        super(videoModule);
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    @Override
    public void onConsumeFrame(VideoCaptureFrame frame, VideoChannel.ChannelContext context) {
        if (mSurfaceView == null) {
            return;
        }

        super.onConsumeFrame(frame, context);
    }

    @Override
    public Object onGetDrawingTarget() {
        return mSurfaceView != null ? mSurfaceView.getHolder().getSurface() : null;
    }

    @Override
    public int onMeasuredWidth() {
        return mSurfaceView.getMeasuredWidth();
    }

    @Override
    public int onMeasuredHeight() {
        return mSurfaceView.getMeasuredHeight();
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        connectChannel(CHANNEL_ID);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        disconnectChannel(CHANNEL_ID);
        destroyed = true;
    }
}
