// ISoundCallback.aidl
package com.mn.aipredictservice;

// Declare any non-default types here with import statements

interface ISoundCallback {
    void onSuccess(String result, float confidence);
    void onError(String e);
}
