#!/usr/bin/env python
# coding: utf-8

import numpy as np
import scipy.io.wavfile as sci_wav
from sklearn.svm import SVC
from sklearn import preprocessing
from python_speech_features import mfcc
import os
import sys
import joblib
from sklearn.metrics import classification_report
import getopt
from java import jclass


def split(xi, length=20000, step=2000):
    if len(xi) < length:
        xi = np.tile(xi, (length // len(xi) + 1))
    num = (len(xi) - (len(xi) - length) % step - length) / step
    xi = xi[0:len(xi) - (len(xi) - length) % step]
    i = 0
    m = []
    while i <= num:
        m.append(xi[i * step:i * step + length])
        i = i + 1
    xi = np.array(m)
    xi = xi.astype(float)
    xi = preprocessing.scale(xi.T).T
    x = np.array([mfcc(i, 44100, nfft=2048).flatten() for i in xi])
    return x


def start_predict(model_path, sound_path):
    model_add = model_path
    sound_add = sound_path
    print('音频处理中')
    clf = joblib.load(model_add)
    fingerprint = split(sci_wav.read(sound_add)[1])

    labels = [0, 1, 2, 3, 4]
    target_names = ['放电', '风', '雨', '雷', '正常']
    result = clf.predict(fingerprint)

    SoundPredictResult = jclass("com.mn.python.bean.SoundPredictResult")
    predict_result = SoundPredictResult()

    print(result)

    pd_confidence = sum(result == 0) / len(result)

    if pd_confidence > 0.9:
        print("放电")
        predict_result.setResult("放电")
        predict_result.setConfidence(pd_confidence)
    else:
        print("正常无故障")
        predict_result.setResult("正常")
        predict_result.setConfidence(1.0 - pd_confidence)

    return predict_result
