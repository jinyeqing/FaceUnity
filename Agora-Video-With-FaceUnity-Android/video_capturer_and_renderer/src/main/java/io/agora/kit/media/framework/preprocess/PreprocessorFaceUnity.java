package io.agora.kit.media.framework.preprocess;

import android.content.Context;
import android.opengl.GLES20;
import com.faceunity.FURenderer;
import com.faceunity.entity.CartoonFilter;

import io.agora.kit.media.capture.VideoCaptureFrame;

public class PreprocessorFaceUnity implements IPreprocessor {
    private FURenderer mFURenderer;
    private Context mContext;

    public PreprocessorFaceUnity(Context context) {
        mContext = context;
        initPreprocessor();
    }

    @Override
    public void onPreProcessFrame(VideoCaptureFrame inFrame, VideoCaptureFrame outFrame) {
        outFrame.mTextureId = mFURenderer.onDrawFrame(outFrame.mImage, outFrame.mTextureId,
                outFrame.mFormat.getWidth(), outFrame.mFormat.getHeight());
        outFrame.mFormat.setPixelFormat(GLES20.GL_TEXTURE_2D);
    }

    @Override
    public int getType() {
        return TYPE_FACE_UNITY;
    }

    @Override
    public void initPreprocessor() {
        mFURenderer = new FURenderer
                .Builder(mContext)
                .maxFaces(4)
                .createEGLContext(false)
                .setNeedFaceBeauty(true)
                .inputTextureType(FURenderer.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE)
                .build();
        mFURenderer.onSurfaceCreated();
        mFURenderer.onCartoonFilterSelected(CartoonFilter.PENCIL_PAINTING);
    }

    @Override
    public void releasePreprocessor() {
        mFURenderer.onSurfaceDestroyed();
    }

    @Override
    public Object getEffectRenderer() {
        return mFURenderer;
    }
}
