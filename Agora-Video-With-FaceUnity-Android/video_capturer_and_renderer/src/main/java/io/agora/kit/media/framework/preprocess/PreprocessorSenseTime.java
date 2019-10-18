package io.agora.kit.media.framework.preprocess;

import android.content.Context;
import android.opengl.GLES20;

import com.sensetime.effects.STRenderer;

import io.agora.kit.media.capture.VideoCaptureFrame;

public class PreprocessorSenseTime implements IPreprocessor {
    private static final String TAG = PreprocessorSenseTime.class.getSimpleName();

    private STRenderer mSTRenderer;
    private Context mContext;

    public PreprocessorSenseTime(Context context) {
        mContext = context;
        initPreprocessor();
    }

    @Override
    public void initPreprocessor() {
        STRenderer.Builder builder = new STRenderer.Builder();
        builder.setContext(mContext)
                .enableSticker(true)
                .enableBeauty(true)
                .enableFilter(true);
        mSTRenderer = builder.build();
    }

    @Override
    public void releasePreprocessor() {
        if (mSTRenderer != null) {
            mSTRenderer.release();
        }
    }

    @Override
    public void onPreProcessFrame(VideoCaptureFrame inFrame, VideoCaptureFrame outFrame) {
        if (mSTRenderer == null) {
            return;
        }

        int textureId = mSTRenderer.preProcess(outFrame.mTextureId,
                outFrame.mSurfaceTexture,
                outFrame.mFormat.getWidth(),
                outFrame.mFormat.getHeight());

        if (textureId > 0) {
            // the input frame is processed by sense time
            outFrame.mTextureId = textureId;
            outFrame.mFormat.setPixelFormat(GLES20.GL_TEXTURE_2D);
        }
    }

    @Override
    public int getType() {
        return TYPE_SENSE_TIME;
    }
}
