package com.mn.python.utils;

import android.os.Environment;

/**
 * project name: PythonDemo
 *
 * Created by liufeng on 2020/4/29 10:57.
 */
public class FileUtils {
    private static final String RootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sound/";
    public static final String ModelPath = RootPath + "SVM.pkl";
    public static final String SoundPath = RootPath + "discharge.wav";

    public static final String AiPredictAction = "com.mn.aipredictservice.aidl";
    public static final String AiPredictPackage = "com.mn.python";
}
