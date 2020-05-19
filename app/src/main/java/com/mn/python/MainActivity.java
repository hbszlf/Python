package com.mn.python;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.chaquo.python.Kwarg;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.mn.python.bean.JavaBean;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "PythonDemo.MainActivity";

    static final int MsgInitPython = 100;
    static final int MsgInitPythonDone = 101;

    Button runBtn;
    TextView resultTv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        initPython();
    }

    private void initView() {
        runBtn = (Button) findViewById(R.id.runBtn);
        runBtn.setOnClickListener(this);
        runBtn.setEnabled(false);

        resultTv = (TextView) findViewById(R.id.pythonTv);
        resultTv.setText("init python ...");
    }


    /**
     * init python env
     */
    void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Message msg = new Message();
        handler.sendEmptyMessage(MsgInitPythonDone);
    }


    /**
     * python and android
     */
    void callPythonCode() {
        String resultInfo;

        Python py = Python.getInstance();
        // 调用hello.py模块中的greet函数，并传一个参数
        py.getModule("hello").callAttr("greet", "Android");

        // 调用python内建函数help()，输出了帮助信息
        py.getBuiltins().get("help").call();

        PyObject obj1 = py.getModule("hello").callAttr("add", 2, 3);
        // 将Python返回值换为Java中的Integer类型
        Integer sum = obj1.toJava(Integer.class);
        Log.d(TAG, "add = " + sum.toString());
        resultInfo = "Python.add(2,3) = " + sum.toString();
        resultTv.setText(resultInfo);

        // 调用python函数，命名式传参，等同 sub(10,b=1,c=3)
        PyObject obj2 = py.getModule("hello").callAttr("sub", 10, new Kwarg("b", 1), new Kwarg("c", 3));
        Integer result = obj2.toJava(Integer.class);
        Log.d(TAG, "sub = " + result.toString());
        resultInfo += "\nPython.sub(10,b=1,c=3) = " + result.toString();
        resultTv.setText(resultInfo);

        // 调用Python函数，将返回的Python中的list转为Java的list
        PyObject obj3 = py.getModule("hello").callAttr("get_list", 10, "xx", 5.6, 'c');
        List<PyObject> pyList = obj3.asList();
        Log.d(TAG, "get_list = " + pyList.toString());
        resultInfo += "\nPython.get_list(10, xx, 5.6, c) = " + pyList.toString();
        resultTv.setText(resultInfo);

        // 将Java的ArrayList对象传入Python中使用
        List<PyObject> params = new ArrayList<PyObject>();
        params.add(PyObject.fromJava("alex"));
        params.add(PyObject.fromJava("bruce"));
        py.getModule("hello").callAttr("print_list", params);

        // Python中调用Java类
        PyObject obj4 = py.getModule("hello").callAttr("get_java_bean");
        JavaBean data = obj4.toJava(JavaBean.class);
        data.print();
        resultInfo += "\nPython.get_java_bean() = " + data.toString();
        resultTv.setText(resultInfo);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.runBtn:
                callPythonCode();
                break;
        }
    }


    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MsgInitPython:
                    resultTv.setText("init python ...");
                    break;
                case MsgInitPythonDone:
                    resultTv.setText("init python done.");
                    runBtn.setEnabled(true);
                    break;
            }
        }
    };
}
