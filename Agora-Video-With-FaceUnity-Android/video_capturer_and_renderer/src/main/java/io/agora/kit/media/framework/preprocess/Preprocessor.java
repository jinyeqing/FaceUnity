package io.agora.kit.media.framework.preprocess;

import android.content.Context;

public class Preprocessor {
    // Change this value to use different beautification sdks
    private static final int USE_PREPROCESSOR = IPreprocessor.TYPE_SENSE_TIME;

    public static IPreprocessor createPreprocessor(Context context) {
        switch (USE_PREPROCESSOR) {
            case IPreprocessor.TYPE_FACE_UNITY:
                return new PreprocessorFaceUnity(context);
            case IPreprocessor.TYPE_SENSE_TIME:
                return new PreprocessorSenseTime(context);
            case IPreprocessor.TYPE_NONE:
                return null;
            default:
                throw new IllegalArgumentException("[Preprocessor] Wrong type");
        }
    }
}
