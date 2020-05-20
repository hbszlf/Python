package com.mn.python.aipredict;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.mn.aipredictservice.ISoundCallback;
import com.mn.aipredictservice.ISoundPredict;
import com.mn.python.bean.SoundPredictResult;
import com.mn.python.utils.FileUtils;

import java.io.File;

public class SoundService extends Service {
    private static final String TAG = "Ai.SoundService";

    Context mContext;

    long startTime;

    String soundPath;

    private RemoteCallbackList<ISoundCallback> callbackList = new RemoteCallbackList<>();

    public SoundService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final ISoundPredict.Stub binder = new ISoundPredict.Stub() {
        @Override
        public void registerCallBack(ISoundCallback iSoundCallback) throws RemoteException {
            callbackList.register(iSoundCallback);
        }

        @Override
        public void unregisterCallBack(ISoundCallback iSoundCallback) throws RemoteException {
            callbackList.unregister(iSoundCallback);
        }

        @Override
        public void startPredict(String filePath) throws RemoteException {

            if (isValidFile(filePath)) {
                soundPath = filePath;
                Log.d(TAG, "start predict sound file path=" + soundPath);
                initAndLoadModel();
            } else {
                final int num = callbackList.beginBroadcast();
                for (int i = 0; i < num; i++) {
                    ISoundCallback iSoundCallback = callbackList.getBroadcastItem(i);
                    iSoundCallback.onError(filePath + " does not exist!");
                }
                callbackList.finishBroadcast();
            }
        }

    };


    static final String ModelPath = FileUtils.ModelPath;


    static final int MsgModelInit = 100;
    static final int MsgModelLoadDone = 101;
    static final int MsgModelLoadFailed = 102;
    static final int MsgSoundTestDone = 200;


    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MsgModelInit:
                    Toast.makeText(mContext, "模型加载中，请稍候...", Toast.LENGTH_LONG).show();
                    break;
                case MsgModelLoadDone:
                    Toast.makeText(mContext, "模型加载完成，声音识别中...", Toast.LENGTH_LONG).show();
                    initPython();
                    startSoundPredict();
                    break;
                case MsgModelLoadFailed:
                    Toast.makeText(mContext, "模型加载失败:" + (String) msg.obj + " 请检查后重试。", Toast.LENGTH_LONG).show();
                    break;
                case MsgSoundTestDone:
                    long endTime = System.currentTimeMillis();
                    String time = (endTime - startTime) > 1000 ? (endTime - startTime) / 1000 + "s" : (endTime - startTime) + "ms";
                    Toast.makeText(mContext, "识别结束。耗时：" + time, Toast.LENGTH_LONG).show();

                    SoundPredictResult soundPredictResult = (SoundPredictResult) msg.obj;

                    int num = callbackList.beginBroadcast();
                    for (int i = 0; i < num; i++) {
                        ISoundCallback iSoundCallback = callbackList.getBroadcastItem(i);
                        try {
                            iSoundCallback.onSuccess(soundPredictResult.getResult(), soundPredictResult.getConfidence());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    callbackList.finishBroadcast();
                    break;

            }
        }
    };

    /**
     * init python env
     */
    void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    /**
     * init and load svm
     */
    void initAndLoadModel() {
        if (isValidFile(ModelPath)) {
            handler.sendEmptyMessage(MsgModelLoadDone);
        } else {
            Message msg = new Message();
            msg.what = MsgModelLoadFailed;
            msg.obj = "model or sound file does not exist!";
            handler.sendMessage(msg);
        }
    }


    /**
     * check file
     *
     * @param path file path
     * @return true - file is valid
     */
    boolean isValidFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        try {
            File file = new File(path);
            if (file.exists()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * start sound predict
     */
    void startSoundPredict() {
        startTime = System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Python py = Python.getInstance();
                PyObject pyObject = py.getModule("svm").callAttr("start_predict", ModelPath, soundPath);
                SoundPredictResult soundPredictResult = pyObject.toJava(SoundPredictResult.class);
                Log.d(TAG, "--- startSoundTestPython Thread get result:" + soundPredictResult.toString());
                Message msg = new Message();
                msg.what = MsgSoundTestDone;
                msg.obj = soundPredictResult;
                handler.sendMessage(msg);
            }
        }).start();
    }


}
