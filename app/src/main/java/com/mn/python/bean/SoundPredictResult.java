package com.mn.python.bean;

/**
 * project name: PythonDemo
 *
 * @class describe
 * Created by liufeng on 2020/4/29 13:07.
 */
public class SoundPredictResult {
    String result;

    float confidence;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "SoundPredictResult{" +
                "result='" + result + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
