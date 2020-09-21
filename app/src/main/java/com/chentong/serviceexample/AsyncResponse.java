package com.chentong.serviceexample;

public interface AsyncResponse {
    void onDataReceivedSuccess(String result);
    void onDataReceivedFailed();
}
