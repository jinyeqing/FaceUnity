package io.agora.kit.media.framework.preprocess;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.sensetime.effects.STRenderer;

import io.agora.kit.media.capture.VideoCaptureFrame;
import views.effects.EffectOptionsLayout;

public class PreprocessorSenseTime implements IPreprocessor,
        EffectOptionsLayout.OnSenseTimeEffectListener {
    private static final String TAG = PreprocessorSenseTime.class.getSimpleName();

    private STRenderer mSTRenderer;
    private Context mContext;

    public PreprocessorSenseTime(Context context) {
        mContext = context;
        initPreprocessor();
    }

    @Override
    public void initPreprocessor() {
        Log.i(TAG, "initPreprocessor");
        STRenderer.Builder builder = new STRenderer.Builder();
        builder.setContext(mContext)
                .enableSticker(true)
                .enableBeauty(true)
                .enableFilter(true)
                .enableMakeup(true);
        mSTRenderer = builder.build();
    }

    @Override
    public void releasePreprocessor() {
        Log.i(TAG, "releasePreprocessor");
        if (mSTRenderer != null) {
            mSTRenderer.release();
            mSTRenderer = null;
        }
    }

    @Override
    public Object getEffectRenderer() {
        return mSTRenderer;
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

    @Override
    public void onBeautyParamSelected(int param, float value) {
        if (mSTRenderer != null) {
            mSTRenderer.setBeautyParam(param, value);
        }
    }

    @Override
    public void onFilterSelected(String filterPath, float strength) {
        if (mSTRenderer != null) {
            mSTRenderer.setFilterStyle(filterPath, strength);
        }
    }

    @Override
    public void onMakeupSelected(int group, int type, String path) {
        if (mSTRenderer != null) {
            if (path != null) {
                mSTRenderer.setMakeupType(type, path);
                mSTRenderer.setMakeupStrength(type, 1.0f);
            } else {
                mSTRenderer.removeMakeupByType(type);
            }
        }
    }

    @Override
    public void onStickerSelected(String path) {
        if (mSTRenderer != null) {
            mSTRenderer.changeSticker(path);
        }
    }

    @Override
    public float onGetBeautyParamValue(int param) {
        return mSTRenderer == null ? 0 :
                mSTRenderer.getSTEffectParameters().getBeautyParam(param);
    }

    @Override
    public String onGetCurrentFilterPath() {
        return mSTRenderer == null ? null :
                mSTRenderer.getSTEffectParameters().getFilter();
    }

    @Override
    public float onGetCurrentFilterStrength() {
        return mSTRenderer == null ? 0 :
                mSTRenderer.getSTEffectParameters().getFilterStrength();
    }

    @Override
    public String onGetCurrentGroupMakeup(String index) {
        return mSTRenderer == null ? null :
                mSTRenderer.getSTEffectParameters().getMakeupPath(index);
    }
}
