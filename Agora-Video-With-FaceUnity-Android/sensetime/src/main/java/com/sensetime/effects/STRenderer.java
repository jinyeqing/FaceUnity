package com.sensetime.effects;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.SensorEvent;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.sensetime.effects.display.STGLRender;
import com.sensetime.effects.glutils.GlUtil;
import com.sensetime.effects.utils.FileUtils;
import com.sensetime.effects.utils.LogUtils;
import com.sensetime.effects.utils.STLicenceUtils;
import com.sensetime.stmobile.STBeautifyNative;
import com.sensetime.stmobile.STCommon;
import com.sensetime.stmobile.STHumanActionParamsType;
import com.sensetime.stmobile.STMobileHumanActionNative;
import com.sensetime.stmobile.STMobileObjectTrackNative;
import com.sensetime.stmobile.STMobileStickerNative;
import com.sensetime.stmobile.STMobileStreamFilterNative;
import com.sensetime.stmobile.model.STBeautyParamsType;
import com.sensetime.stmobile.model.STFilterParamsType;
import com.sensetime.stmobile.model.STHumanAction;
import com.sensetime.stmobile.model.STRect;
import com.sensetime.stmobile.model.STRotateType;
import com.sensetime.stmobile.model.STStickerInputParams;

import java.nio.ByteBuffer;
import java.util.TreeMap;

import javax.microedition.khronos.opengles.GL10;

public class STRenderer {
    private static final String TAG = STRenderer.class.getSimpleName();

    private Context mContext;

    private final Object mHumanActionHandleLock = new Object();

    private int mHumanActionCreateConfig = STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO;
    private boolean mIsCreateHumanActionHandleSucceeded = false;
    private STMobileHumanActionNative mSTHumanActionNative = new STMobileHumanActionNative();
    private STHumanAction mHumanActionBeautyOutput = new STHumanAction();
    private STBeautifyNative mStBeautifyNative = new STBeautifyNative();
    private STMobileStickerNative mStStickerNative = new STMobileStickerNative();
    private STMobileStreamFilterNative mSTMobileStreamFilterNative = new STMobileStreamFilterNative();
    private STMobileObjectTrackNative mSTMobileObjectTrackNative = new STMobileObjectTrackNative();

    private long mDetectConfig = 0;

    // Handler threads
    private HandlerThread mSubModelsManagerThread;
    private Handler mSubModelsManagerHandler;

    private HandlerThread mChangeStickerManagerThread;
    private Handler mChangeStickerManagerHandler;

    // Messages for submodule and sticker handler threads
    private static final int MESSAGE_ADD_SUB_MODEL = 1001;
    private static final int MESSAGE_REMOVE_SUB_MODEL = 1002;
    private static final int MESSAGE_NEED_CHANGE_STICKER = 1003;
    private static final int MESSAGE_NEED_REMOVE_STICKER = 1004;
    private static final int MESSAGE_NEED_REMOVE_ALL_STICKERS = 1005;
    private static final int MESSAGE_NEED_ADD_STICKER = 1006;

    private boolean mAuthorized;

    // switches of effects
    private boolean mNeedObject = false;
    private boolean mNeedSetObjectTarget = false;
    private boolean mNeedDistance = false;
    private boolean mNeedSticker = false;
    private boolean mNeedBeautify = false;
    private boolean mNeedFilter = false;

    // Object tracking states
    private Rect mTargetRect = new Rect();
    private boolean mIsObjectTracking = false;
    private boolean mNeedShowRect = false;
    private float mFaceDistance = 0f;

    // Beautification states
    private float[] mBeautifyParams = {
            0.36f, 0.74f, 0.02f, 0.13f, 0.11f, 0.1f, 0f, 0f, 0f,
            0f,    0f,    0f,    0f,    0f,   0f, 0f, 0f, 0f,
            0f,    0f,    0f,    0f,    0f,   0f, 0f, 0f
    };

    // Sticker states
    private String mCurrentSticker;
    private TreeMap<Integer, String> mCurrentStickerMaps = new TreeMap<>();

    // Filter states
    private String mCurrentFilterStyle;
    private float mCurrentFilterStrength = 0.65f;  // ranges of [0,1]
    private float mFilterStrength = 0.65f;
    private String mFilterStyle;

    // States changing
    private boolean mIsPaused = false;
    private boolean mCameraChanging = false;

    private STGLRender mGLRender;
    private ByteBuffer mRGBABuffer;
    private int[] mBeautifyTextureId;
    private int[] mTextureOutId;
    private int[] mFilterTextureOutId;
    private int mImageWidth;
    private int mImageHeight;

    private int mCustomEvent = 0;
    private SensorEvent mSensorEvent;

    public STRenderer(Context context) {
        mContext = context;
    }

    private void init() {
        checkLicense();
        initGLRender();
        initHumanAction();
        initHandlerManager();
        initBeauty();
        initSticker();
        initFilter();
    }

    private void checkLicense() {
        mAuthorized = STLicenceUtils.checkLicense(mContext);
    }

    private void initGLRender() {
        mGLRender = new STGLRender();

        // Pre-processing module does not concern the image
        // transformation as if the images are already what
        // they should be before input into the preprocessor.
        mGLRender.adjustTextureBuffer(0, false, false);
    }

    private void initHumanAction() {
        // The initialization of HumanAction interface needs to be
        // loaded asynchronously, cause it needs to read files
        // and time-consuming.
        // Core modules can be loaded using a worker thread,
        // then sub modules can be loaded later when needed.
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mHumanActionHandleLock) {
                    // Read resource file into memory from assets folder, then use low-level api
                    // st_mobile_human_action_create_from_buffer to create the handle
                    int result = mSTHumanActionNative.createInstanceFromAssetFile(
                            FileUtils.getActionModelName(), mHumanActionCreateConfig, mContext.getAssets());
                    LogUtils.i(TAG, "the result for createInstance for human_action is %d", result);

                    if (result == 0) {
                        result = mSTHumanActionNative.addSubModelFromAssetFile(FileUtils.MODEL_NAME_HAND, mContext.getAssets());
                        LogUtils.i(TAG, "add hand model result: %d", result);
                        result = mSTHumanActionNative.addSubModelFromAssetFile(FileUtils.MODEL_NAME_SEGMENT, mContext.getAssets());
                        LogUtils.i(TAG, "add figure segment model result: %d", result);

                        mIsCreateHumanActionHandleSucceeded = true;
                        mSTHumanActionNative.setParam(STHumanActionParamsType.ST_HUMAN_ACTION_PARAM_BACKGROUND_BLUR_STRENGTH, 0.35f);

                        //for test face morph
                        result = mSTHumanActionNative.addSubModelFromAssetFile(FileUtils.MODEL_NAME_FACE_EXTRA, mContext.getAssets());
                        mDetectConfig |= STMobileHumanActionNative.ST_MOBILE_DETECT_EXTRA_FACE_POINTS;
                        LogUtils.i(TAG, "add face extra model result: %d", result);

                        //for test avatar
                        result = mSTHumanActionNative.addSubModelFromAssetFile(FileUtils.MODEL_NAME_EYEBALL_CONTOUR, mContext.getAssets());
                        LogUtils.i(TAG, "add eyeball contour model result: %d", result);
                    }
                }
            }
        }).start();
    }

    private void initHandlerManager() {
        mSubModelsManagerThread = new HandlerThread("SubModelManagerThread");
        mSubModelsManagerThread.start();
        mSubModelsManagerHandler = new Handler(mSubModelsManagerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (!mIsPaused && !mCameraChanging &&
                        mIsCreateHumanActionHandleSucceeded) {
                    switch (msg.what) {
                        case MESSAGE_ADD_SUB_MODEL:
                            String modelName = (String) msg.obj;
                            if (modelName != null) {
                                addSubModel(modelName);
                            }
                            break;
                        case MESSAGE_REMOVE_SUB_MODEL:
                            int config = (int) msg.obj;
                            if (config != 0) {
                                removeSubModel(config);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        };

        mChangeStickerManagerThread = new HandlerThread("ChangeStickerManagerThread");
        mChangeStickerManagerThread.start();
        mChangeStickerManagerHandler = new Handler(mChangeStickerManagerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (!mIsPaused && !mCameraChanging) {
                    switch (msg.what) {
                        case MESSAGE_NEED_CHANGE_STICKER:
                            mCurrentSticker = (String) msg.obj;
                            int result = mStStickerNative.changeSticker(mCurrentSticker);
                            LogUtils.i(TAG, "change sticker result: %d", result);
                            setHumanActionDetectConfig(mNeedBeautify, mStStickerNative.getTriggerAction());
                            break;
                        case MESSAGE_NEED_REMOVE_ALL_STICKERS:
                            mStStickerNative.removeAllStickers();
                            if (mCurrentStickerMaps != null) {
                                mCurrentStickerMaps.clear();
                            }
                            setHumanActionDetectConfig(mNeedBeautify, mStStickerNative.getTriggerAction());
                            break;
                        default:
                            break;
                    }
                }
            }
        };
    }

    private void addSubModel(final String modelName) {
        synchronized (mHumanActionHandleLock) {
            int result = mSTHumanActionNative.addSubModelFromAssetFile(modelName, mContext.getAssets());
            LogUtils.i(TAG, "add sub model result: %d", result);

            if (result == 0) {
                if (modelName.equals(FileUtils.MODEL_NAME_BODY_FOURTEEN)) {
                    mDetectConfig |= STMobileHumanActionNative.ST_MOBILE_BODY_KEYPOINTS;
                    mSTHumanActionNative.setParam(STHumanActionParamsType.ST_HUMAN_ACTION_PARAM_BODY_LIMIT, 3.0f);
                } else if (modelName.equals(FileUtils.MODEL_NAME_FACE_EXTRA)) {
                    mDetectConfig |= STMobileHumanActionNative.ST_MOBILE_DETECT_EXTRA_FACE_POINTS;
                } else if (modelName.equals(FileUtils.MODEL_NAME_EYEBALL_CONTOUR)) {
                    mDetectConfig |= STMobileHumanActionNative.ST_MOBILE_DETECT_EYEBALL_CONTOUR |
                            STMobileHumanActionNative.ST_MOBILE_DETECT_EYEBALL_CENTER;
                } else if (modelName.equals(FileUtils.MODEL_NAME_HAND)) {
                    mDetectConfig |= STMobileHumanActionNative.ST_MOBILE_HAND_DETECT_FULL;
                }
            }
        }
    }

    private void removeSubModel(final int config) {
        synchronized (mHumanActionHandleLock) {
            int result = mSTHumanActionNative.removeSubModelByConfig(config);
            LogUtils.i(TAG, "remove sub model result: %d", result);

            if (config == STMobileHumanActionNative.ST_MOBILE_ENABLE_BODY_KEYPOINTS) {
                mDetectConfig &= ~STMobileHumanActionNative.ST_MOBILE_BODY_KEYPOINTS;
            } else if (config == STMobileHumanActionNative.ST_MOBILE_ENABLE_FACE_EXTRA_DETECT) {
                mDetectConfig &= ~STMobileHumanActionNative.ST_MOBILE_DETECT_EXTRA_FACE_POINTS;
            } else if (config == STMobileHumanActionNative.ST_MOBILE_ENABLE_EYEBALL_CONTOUR_DETECT) {
                mDetectConfig &= ~(STMobileHumanActionNative.ST_MOBILE_DETECT_EYEBALL_CONTOUR |
                        STMobileHumanActionNative.ST_MOBILE_DETECT_EYEBALL_CENTER);
            } else if (config == STMobileHumanActionNative.ST_MOBILE_ENABLE_HAND_DETECT) {
                mDetectConfig &= ~STMobileHumanActionNative.ST_MOBILE_HAND_DETECT_FULL;
            }
        }
    }

    /**
     * Options of human action detection
     * @param needFaceDetect the same as whether use face beauty or not.
     * @param config  here use STStickerNative.getTriggerAction()
     */
    private void setHumanActionDetectConfig(boolean needFaceDetect, long config) {
        if (!mNeedSticker || mCurrentSticker == null) {
            config = 0;
        }

        if (needFaceDetect) {
            mDetectConfig = (config | STMobileHumanActionNative.ST_MOBILE_FACE_DETECT);
        } else {
            mDetectConfig = config;
        }
    }

    private void initBeauty() {
        int result = mStBeautifyNative.createInstance();
        LogUtils.i(TAG, "the result is for initBeautify " + result);

        if (result == 0) {
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH, mBeautifyParams[0]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH, mBeautifyParams[1]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH, mBeautifyParams[2]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO, mBeautifyParams[3]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO, mBeautifyParams[4]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO, mBeautifyParams[5]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_CONSTRACT_STRENGTH, mBeautifyParams[6]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SATURATION_STRENGTH, mBeautifyParams[7]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_DEHIGHLIGHT_STRENGTH, mBeautifyParams[8]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_NARROW_FACE_STRENGTH, mBeautifyParams[9]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_NARROW_NOSE_RATIO, mBeautifyParams[10]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_NOSE_LENGTH_RATIO, mBeautifyParams[11]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_CHIN_LENGTH_RATIO, mBeautifyParams[12]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_MOUTH_SIZE_RATIO, mBeautifyParams[13]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_PHILTRUM_LENGTH_RATIO, mBeautifyParams[14]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_HAIRLINE_HEIGHT_RATIO, mBeautifyParams[15]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_THIN_FACE_SHAPE_RATIO, mBeautifyParams[16]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_EYE_DISTANCE_RATIO, mBeautifyParams[17]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_EYE_ANGLE_RATIO, mBeautifyParams[18]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_OPEN_CANTHUS_RATIO, mBeautifyParams[19]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_PROFILE_RHINOPLASTY_RATIO, mBeautifyParams[20]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_BRIGHT_EYE_RATIO, mBeautifyParams[21]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_REMOVE_DARK_CIRCLES_RATIO, mBeautifyParams[22]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_REMOVE_NASOLABIAL_FOLDS_RATIO, mBeautifyParams[23]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_WHITE_TEETH_RATIO, mBeautifyParams[24]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_3D_APPLE_MUSLE_RATIO, mBeautifyParams[25]);
        }
    }

    private void initSticker() {
        int result = mStStickerNative.createInstance(mContext);
        if (mNeedSticker && mCurrentStickerMaps.size() == 0) {
            mStStickerNative.changeSticker(mCurrentSticker);
        }

        if (mNeedSticker && mCurrentStickerMaps != null) {
            TreeMap<Integer, String> currentStickerMap = new TreeMap<>();

            for (Integer index : mCurrentStickerMaps.keySet()) {
                String sticker = mCurrentStickerMaps.get(index);//得到每个key多对用value的值
                int packageId = mStStickerNative.addSticker(sticker);
                currentStickerMap.put(packageId, sticker);
                // Notify to UI here
            }

            mCurrentStickerMaps.clear();
            mCurrentStickerMaps.putAll(currentStickerMap);
        }

        //从资源文件加载Avatar模型
        mStStickerNative.loadAvatarModelFromAssetFile(FileUtils.MODEL_NAME_AVATAR_CORE, mContext.getAssets());
        setHumanActionDetectConfig(mNeedBeautify, mStStickerNative.getTriggerAction());
        LogUtils.i(TAG, "the result for createInstance for human_action is %d", result);
    }

    private void initFilter() {
        mSTMobileStreamFilterNative.createInstance();
        mSTMobileStreamFilterNative.setStyle(mCurrentFilterStyle);
        mCurrentFilterStrength = mFilterStrength;

        // Currently there is only 1 parameter for filters
        mSTMobileStreamFilterNative.setParam(
                STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
    }

    private void initBuffers() {
        if (mRGBABuffer == null) {
            mRGBABuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
        }

        if (mBeautifyTextureId == null) {
            mBeautifyTextureId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mBeautifyTextureId, GLES20.GL_TEXTURE_2D);
        }

        if (mTextureOutId == null) {
            mTextureOutId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mTextureOutId, GLES20.GL_TEXTURE_2D);
        }
    }

    private void adjustImageSize(int width, int height) {
        if (mImageWidth != width || mImageHeight != height) {
            mImageWidth = width;
            mImageHeight = height;

            // SenseTime renderer does not need to know
            // the surface size, thus without concerning
            // flipping and resizing.
            mGLRender.init(mImageWidth, mImageHeight);
            mGLRender.calculateVertexBuffer(mImageWidth,
                    mImageHeight, mImageWidth, mImageHeight);
        }
    }

    private int getCurrentOrientation() {
        // TODO provide a callback to initialize the accelerometer
        //int dir = Accelerometer.getDirection();
        //int orientation = dir - 1;
        //if (orientation < 0) {
        //    orientation = dir ^ 3;
        //}

        //return orientation;
        return 270;
    }

    public void enableBeautify(boolean needBeautify) {
        mNeedBeautify = needBeautify;
        setHumanActionDetectConfig(mNeedBeautify, mStStickerNative.getTriggerAction());
    }

    public void enableSticker(boolean needSticker) {
        mNeedSticker = needSticker;
        if (!needSticker) {
            setHumanActionDetectConfig(mNeedBeautify, mStStickerNative.getTriggerAction());
        }
    }

    public void enableFilter(boolean needFilter){
        mNeedFilter = needFilter;
    }

    public void release() {
        Log.e(TAG, "recycle preprocessor");
        synchronized (mHumanActionHandleLock) {
            mSTHumanActionNative.destroyInstance();
        }

        mSTMobileObjectTrackNative.destroyInstance();

        if (mCurrentStickerMaps != null) {
            mCurrentStickerMaps.clear();
            mCurrentStickerMaps = null;
        }

        mSTHumanActionNative.reset();

        mStBeautifyNative.destroyBeautify();
        mStStickerNative.removeAvatarModel();
        mStStickerNative.destroyInstance();
        mSTMobileStreamFilterNative.destroyInstance();
        mRGBABuffer = null;
        deleteTextures();
        mGLRender.destroyFrameBuffers();
        mGLRender.destroyPrograms();
    }

    private void deleteTextures() {
        LogUtils.i(TAG, "delete textures");
        deleteInternalTextures();
    }

    private void deleteInternalTextures() {
        if (mBeautifyTextureId != null) {
            GLES20.glDeleteTextures(1, mBeautifyTextureId, 0);
            mBeautifyTextureId = null;
        }

        if (mTextureOutId != null) {
            GLES20.glDeleteTextures(1, mTextureOutId, 0);
            mTextureOutId = null;
        }

        if (mFilterTextureOutId != null) {
            GLES20.glDeleteTextures(1, mFilterTextureOutId, 0);
            mFilterTextureOutId = null;
        }
    }

    public int preProcess(int textureId, SurfaceTexture surfaceTexture, int width, int height) {
        if (!mAuthorized || mIsPaused ||
                mCameraChanging || surfaceTexture == null) {
            return 0;
        }

        adjustImageSize(width, height);
        // init buffers here cause we need the image size info
        initBuffers();

        GLES20.glEnable(GL10.GL_DITHER);
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mRGBABuffer.rewind();
        int processedTextureId = mGLRender.preProcess(textureId, mRGBABuffer);

        int result;

        // Begins effect rendering
        if (mNeedObject) {
            if (mNeedSetObjectTarget) {
                STRect inputRect = new STRect(mTargetRect.left, mTargetRect.top,
                        mTargetRect.right, mTargetRect.bottom);
                mSTMobileObjectTrackNative.setTarget(mRGBABuffer.array(),
                        STCommon.ST_PIX_FMT_RGBA8888, mImageWidth, mImageHeight, inputRect);
                mNeedSetObjectTarget = false;
                mIsObjectTracking = true;
            }

            Rect rect = new Rect(0, 0, 0, 0);

            if (mIsObjectTracking) {
                float[] score = new float[1];
                STRect outputRect = mSTMobileObjectTrackNative.objectTrack(mRGBABuffer.array(),
                        STCommon.ST_PIX_FMT_RGBA8888, mImageWidth, mImageHeight,score);
                //if (outputRect != null && score != null && score.length > 0) {
                //    rect = STUtils.adjustToScreenRectMin(outputRect.getRect(), mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight);
                //}
                //mIndexRect = rect;
            } else {
//                if (mNeedShowRect) {
//                    Message msg = mHandler.obtainMessage(CameraActivity.MSG_DRAW_OBJECT_IMAGE_AND_RECT);
//                    msg.obj = mIndexRect;
//                    mHandler.sendMessage(msg);
//                } else {
//                    Message msg = mHandler.obtainMessage(CameraActivity.MSG_DRAW_OBJECT_IMAGE);
//                    msg.obj = rect;
//                    mHandler.sendMessage(msg);
//                    mIndexRect = rect;
//                }
            }
        } else {
            //if (!mNeedObject || !(mNeedBeautify || mNeedSticker)) {
            //    Message msg = mHandler.obtainMessage(CameraActivity.MSG_CLEAR_OBJECT);
            //    mHandler.sendMessage(msg);
            //}
        }

        //TODO Enable beautification here
        //if (!mNeedBeautify) {
        //    enableBeautify(true);
        //}

        if (mNeedBeautify || mNeedSticker) {
            STHumanAction humanAction = null;

            if (mIsCreateHumanActionHandleSucceeded) {
                humanAction = mSTHumanActionNative.humanActionDetect(mRGBABuffer.array(),
                        STCommon.ST_PIX_FMT_RGBA8888, mDetectConfig,
                        getCurrentOrientation(), mImageWidth, mImageHeight);

                if (mNeedDistance) {
                    if (humanAction != null && humanAction.faceCount > 0) {
                        mFaceDistance = mSTHumanActionNative.getFaceDistance(humanAction.faces[0],
                                getCurrentOrientation(), mImageWidth, mImageHeight,
                                // TODO workaround here, and obtain this parameter
                                // mCameraProxy.getCamera().getParameters().getVerticalViewAngle()
                                270
                        );
                    } else {
                        // no face info found
                        mFaceDistance = 0f;
                    }
                }

                int orientation = getCurrentOrientation();

                if (mNeedBeautify) {
                    result = mStBeautifyNative.processTexture(processedTextureId, mImageWidth,
                            mImageHeight, orientation, humanAction,
                            mBeautifyTextureId[0], mHumanActionBeautyOutput);
                    if (result == 0) {
                        processedTextureId = mBeautifyTextureId[0];
                        humanAction = mHumanActionBeautyOutput;
                        // LogUtils.i(TAG, "replace enlarge eye and shrink face action");
                    }
                }

                if (mNeedSticker) {
                    /**
                     * 1.在切换贴纸时，调用STMobileStickerNative的changeSticker函数，传入贴纸路径(参考setShowSticker函数的使用)
                     * 2.切换贴纸后，使用STMobileStickerNative的getTriggerAction函数获取当前贴纸支持的手势和前后背景等信息，返回值为int类型
                     * 3.根据getTriggerAction函数返回值，重新配置humanActionDetect函数的config参数，使detect更高效
                     *
                     * 例：只检测人脸信息和当前贴纸支持的手势等信息时，使用如下配置：
                     * mDetectConfig = mSTMobileStickerNative.getTriggerAction()|STMobileHumanActionNative.ST_MOBILE_FACE_DETECT;
                     */
                    int event = mCustomEvent;
                    STStickerInputParams inputParams;

                    // Note: we take it as back-facing camera first
                    if (mSensorEvent != null && mSensorEvent.values != null && mSensorEvent.values.length > 0) {
                        inputParams = new STStickerInputParams(mSensorEvent.values, false, event);
                    } else {
                        inputParams = new STStickerInputParams(new float[] {0, 0, 0, 1}, false, event);
                    }

                    long stickerStartTime = System.currentTimeMillis();

                    // 如果需要输出buffer推流或其他，设置该开关为true
                    boolean needOutputBuffer = false;
                    if (!needOutputBuffer) {
                        result = mStStickerNative.processTexture(processedTextureId, humanAction,
                                orientation, STRotateType.ST_CLOCKWISE_ROTATE_0, mImageWidth, mImageHeight,
                                false, inputParams, mTextureOutId[0]);
                    } else {
                        byte[] imageOut = new byte[mImageWidth * mImageHeight * 4];
                        result = mStStickerNative.processTextureAndOutputBuffer(processedTextureId,
                                humanAction, orientation, STRotateType.ST_CLOCKWISE_ROTATE_0, mImageWidth,
                                mImageHeight, false, inputParams, mTextureOutId[0],
                                STCommon.ST_PIX_FMT_RGBA8888, imageOut);
                    }

                    if (event == mCustomEvent) {
                        mCustomEvent = 0;
                    }

                    LogUtils.i(TAG, "processTexture result: %d", result);
                    LogUtils.i(TAG, "sticker cost time: %d", System.currentTimeMillis() - stickerStartTime);

                    if (result == 0) {
                        processedTextureId = mTextureOutId[0];
                    }
                }
            }

            if (mCurrentFilterStyle != null &&
                    !mCurrentFilterStyle.equals(mFilterStyle)) {
                mCurrentFilterStyle = mFilterStyle;
                mSTMobileStreamFilterNative.setStyle(mCurrentFilterStyle);
            }

            if (mCurrentFilterStrength != mFilterStrength) {
                mCurrentFilterStrength = mFilterStrength;
                mSTMobileStreamFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
            }

            if (mFilterTextureOutId == null) {
                mFilterTextureOutId = new int[1];
                GlUtil.initEffectTexture(mImageWidth, mImageHeight, mFilterTextureOutId, GLES20.GL_TEXTURE_2D);
            }

            //滤镜
            if (mNeedFilter) {
                long filterStartTime = System.currentTimeMillis();
                int ret = mSTMobileStreamFilterNative.processTexture(processedTextureId, mImageWidth, mImageHeight, mFilterTextureOutId[0]);
                LogUtils.i(TAG, "filter cost time: %d", System.currentTimeMillis() - filterStartTime);
                if (ret == 0) {
                    processedTextureId = mFilterTextureOutId[0];
                }
            }
        }

        return processedTextureId;
    }

    public static class Builder {
        private Context mContext;
        private boolean mNeedSticker = false;
        private boolean mNeedBeautify = false;
        private boolean mNeedFilter = false;

        public Builder setContext(Context context) {
            mContext = context;
            return this;
        }

        public Builder enableSticker(boolean enabled) {
            mNeedSticker = enabled;
            return this;
        }

        public Builder enableBeauty(boolean enabled) {
            mNeedBeautify = enabled;
            return this;
        }

        public Builder enableFilter(boolean enabled) {
            mNeedFilter = enabled;
            return this;
        }

        public STRenderer build() {
            STRenderer renderer = new STRenderer(mContext);
            renderer.enableBeautify(mNeedBeautify);
            renderer.enableFilter(mNeedFilter);
            renderer.enableSticker(mNeedSticker);
            renderer.init();
            return renderer;
        }
    }
}
