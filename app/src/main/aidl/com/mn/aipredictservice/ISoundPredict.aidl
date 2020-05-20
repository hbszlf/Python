// ISoundPredict.aidl
package com.mn.aipredictservice;

// Declare any non-default types here with import statements
import com.mn.aipredictservice.ISoundCallback;

interface ISoundPredict {
//    /**
//     * Demonstrates some basic types that you can use as parameters
//     * and return values in AIDL.
//     */
//    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
//            double aDouble, String aString);

      void registerCallBack(ISoundCallback iSoundCallback);
      void unregisterCallBack(ISoundCallback iSoundCallback);

      void startPredict(String filePath);
}
