package io.agora.kit.media.video.preprocess;

import android.content.Context;

public class Preprocessor {
    public static IPreprocessor createPreprocessor(Context context, int type) {
        switch (type) {
            case IPreprocessor.TYPE_FACE_UNITY:
                return new FaceUnityPreprocessor(context);
            case IPreprocessor.TYPE_SENSE_TIME:
                return new SenseTimePreprocessor(context);
            case IPreprocessor.TYPE_NONE:
                return null;
            default:
                throw new IllegalArgumentException("[Preprocessor] Wrong type");
        }
    }
}
