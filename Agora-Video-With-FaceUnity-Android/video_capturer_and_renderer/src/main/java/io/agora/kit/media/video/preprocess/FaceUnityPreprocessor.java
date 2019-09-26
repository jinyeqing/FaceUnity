package io.agora.kit.media.video.preprocess;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Environment;

import com.faceunity.FURenderer;
import com.faceunity.entity.CartoonFilter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.agora.kit.media.capture.VideoCaptureFrame;

public class FaceUnityPreprocessor implements IPreprocessor {
    private FURenderer mFURenderer;
    private Context mContext;

    FaceUnityPreprocessor(Context context) {
        mContext = context;
        initPreprocessor();
    }

    @Override
    public void onPreProcessFrame(VideoCaptureFrame inFrame, VideoCaptureFrame outFrame) {
        outFrame.mTextureId = mFURenderer.onDrawFrame(inFrame.mImage, inFrame.mTextureId,
                inFrame.mFormat.getWidth(), inFrame.mFormat.getHeight());
        outFrame.mFormat.setPixelFormat(GLES20.GL_TEXTURE_2D);
        //saveFile(outFrame.mImage);
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
    public void recyclePreprocessor() {
        mFURenderer.onSurfaceDestroyed();
    }

    private void saveFile(final byte[] image) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File root = Environment.getExternalStorageDirectory();
                File folder = new File(root, "frames");
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                File file = new File(folder, System.currentTimeMillis() + ".yuv");
                BufferedOutputStream bos = null;
                try {
                    bos = new BufferedOutputStream(
                            new FileOutputStream(file));
                    bos.write(image);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bos != null) {
                        try {
                            bos.flush();
                            bos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }).start();
    }
}
