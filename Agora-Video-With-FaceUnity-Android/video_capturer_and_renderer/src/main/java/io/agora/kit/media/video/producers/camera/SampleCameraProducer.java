package io.agora.kit.media.video.producers.camera;


import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.util.Log;

import java.io.IOException;
import java.security.Policy;

import io.agora.kit.media.capture.VideoCaptureFormat;
import io.agora.kit.media.capture.VideoCaptureFrame;
import io.agora.kit.media.gles.core.EglCore;
import io.agora.kit.media.gles.core.GlUtil;
import io.agora.kit.media.video.producers.VideoProducer;

public class SampleCameraProducer extends VideoProducer implements Camera.PreviewCallback {
    private static final String TAG = SampleCameraProducer.class.getSimpleName();

    private Camera mCamera;

    private EglCore mEglCore;
    private EGLSurface mSurface;
    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private float[] mTexMatrix = new float[16];
    private byte[] mBuffer = new byte[640 * 480 * 3 / 2];
    private VideoCaptureFormat mFormat = new VideoCaptureFormat(
            640, 480, 24, GLES11Ext.GL_TEXTURE_EXTERNAL_OES
    );

    @Override
    public void connectChannel(int channelId) {
        super.connectChannel(channelId);
        openCamera();
    }

    private void openCamera() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                Camera.Parameters params = mCamera.getParameters();
                params.setPreviewSize(640, 480);
                mCamera.setParameters(params);
                startPreview();
            }
        }).start();
    }

    private void startPreview() {
        initEgl();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.setPreviewCallbackWithBuffer(this);
        mCamera.addCallbackBuffer(mBuffer);
        mCamera.startPreview();
    }

    @Override
    public void disconnect() {
        closeCamera();
        releaseEgl();
        super.disconnect();
    }

    private void initEgl() {
        Log.i("VideoProducer","initEgl:"+videoChannel.getEGLContext().eglCore.getEGLContext());
        mEglCore = new EglCore(videoChannel.getEGLContext().eglCore.getEGLContext(), 0);
        mSurface = mEglCore.createOffscreenSurface(1, 1);
        mEglCore.makeCurrent(mSurface);
        mTextureId = GlUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
    }

    private void releaseEgl() {
        mEglCore.releaseSurface(mSurface);
        mEglCore.release();

    }

    private void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mCamera == null) {
            return;
        }

        VideoCaptureFrame frame = new VideoCaptureFrame(
                mFormat, mSurfaceTexture, mTextureId, data,
                mTexMatrix, System.currentTimeMillis(),
                0, false);

        Log.i(TAG, "VideoProducer " + Thread.currentThread().getName());

        pushVideoFrame(frame);
        mCamera.addCallbackBuffer(mBuffer);
    }
}
