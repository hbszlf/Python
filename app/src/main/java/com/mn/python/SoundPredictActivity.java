package com.mn.python;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mn.aipredictservice.ISoundCallback;
import com.mn.aipredictservice.ISoundPredict;
import com.mn.python.utils.FileUtils;

import java.io.File;
import java.util.Arrays;

public class SoundPredictActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "PythonDemo.MainActivity";

    private static final int PermissionsRequestCode = 2000;

    private static final int DurCntDefault = 10;

    Context mContext;

    Button runBtn;
    EditText resultTv;

    TextView modelBtn;
    TextView soundBtn;

    EditText modelPathEt;
    EditText soundPathEt;

    EditText durCntEt;
    ProgressBar progressBar;

    private ISoundPredict iSoundPredict;

    long startTime;

    int durCnt;
    int durCntMax;

    String resultInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_predict);

        mContext = this;

        durCnt = DurCntDefault;
        durCntMax = durCnt;

        initView();

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions();
        }

    }

    private void initView() {
        modelBtn = (TextView) findViewById(R.id.browseModelBtn);
        modelBtn.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
//        modelBtn.setOnClickListener(this);
//        modelBtn.setEnabled(false);

        soundBtn = (TextView) findViewById(R.id.browseSoundBtn);
        soundBtn.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
//        soundBtn.setOnClickListener(this);
//        soundBtn.setEnabled(false);

        modelPathEt = (EditText) findViewById(R.id.modelPathEt);

        soundPathEt = (EditText) findViewById(R.id.soundPathEt);

        runBtn = (Button) findViewById(R.id.runBtn);
        runBtn.setOnClickListener(this);
        runBtn.setEnabled(false);

        resultTv = (EditText) findViewById(R.id.pythonTv);
        resultTv.setText("init python ...");

        durCntEt = (EditText) findViewById(R.id.durCntEt);
        durCntEt.setText(String.valueOf(durCnt));
        durCntEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    int cnt = Integer.valueOf(editable.toString());
                    if (cnt < 0 || cnt > 2000) {
                        Toast.makeText(mContext, "set duration count is invalid.", Toast.LENGTH_LONG).show();
                    } else {
                        durCnt = cnt;
                        durCntMax = durCnt;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "set duration count is invalid.", Toast.LENGTH_LONG).show();
                }
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void initAndLoadModule() {

        modelPathEt.setText(FileUtils.ModelPath);

        soundPathEt.setText(FileUtils.SoundPath);

        runBtn.setEnabled(true);

        Intent intent = new Intent();
        intent.setAction(FileUtils.AiPredictAction);
        intent.setPackage(FileUtils.AiPredictPackage);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.runBtn:
                resultInfo = "";
                resultTv.setText("predict sound ...");
                startSoundPredictProcess();
                break;
        }
    }

    private void startSoundPredictProcess() {
        if (isValidFile(FileUtils.ModelPath) && isValidFile(FileUtils.SoundPath)) {
            startTime = System.currentTimeMillis();
            if (iSoundPredict != null) {
                try {
                    iSoundPredict.startPredict(FileUtils.SoundPath);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(mContext, "模型或者声音文件没有找到！！", Toast.LENGTH_LONG).show();
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            iSoundPredict = ISoundPredict.Stub.asInterface(iBinder);
            try {
                iSoundPredict.asBinder().linkToDeath(mDeathRecipient, 0);
                iSoundPredict.registerCallBack(iSoundCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (null == iSoundPredict) {
                return;
            }
            iSoundPredict.asBinder().unlinkToDeath(mDeathRecipient, 0);
            iSoundPredict = null;
        }
    };

    private ISoundCallback iSoundCallback = new ISoundCallback.Stub() {
        @Override
        public void onSuccess(String result, float confidence) throws RemoteException {
            durCnt--;

            progressBar.setProgress((durCntMax - durCnt) * 100 / durCntMax, true);

            if (!TextUtils.isEmpty(resultInfo)) {
                resultInfo = "\n" + resultInfo;
            }

            String curResult = result + " (" + confidence * 100 + "%)";
            long endTime = System.currentTimeMillis();
            String time = (endTime - startTime) > 1000 ? (endTime - startTime) / 1000 + "s" : (endTime - startTime) + "ms";
            curResult += "   run time:" + time;

            resultInfo = curResult + resultInfo;
            resultTv.setText(resultInfo);

            if (durCnt > 0) {
                startSoundPredictProcess();
            }
        }

        @Override
        public void onError(String e) throws RemoteException {
            resultTv.setText(e);
        }
    };

    @Override
    protected void onDestroy() {

        if (null != iSoundPredict && iSoundPredict.asBinder().isBinderAlive()) {
            try {
                iSoundPredict.unregisterCallBack(iSoundCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        unbindService(serviceConnection);

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult requestCode:" + requestCode + ", permissions:" + permissions[0].toString() + ",grantResults:" + Arrays.toString(grantResults));
        if (requestCode == PermissionsRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                initAndLoadModule();
            } else {// Permission Denied
                new AlertDialog.Builder(mContext)
                        .setMessage("The application need STORAGE permission to process the file.")
                        .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionsRequestCode);
                            }
                        })
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        })
                        .create()
                        .show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void requestPermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?, if the permission was asked more then one time.
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(mContext)
                        .setMessage("The application need STORAGE permission to process the read file.")
                        .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {    // the user decide to continue, show the request.
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionsRequestCode);
                            }
                        })
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() { // the user decide to exit ThermApp.
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionsRequestCode);
                return;
            }
        } else {
            initAndLoadModule();
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
}