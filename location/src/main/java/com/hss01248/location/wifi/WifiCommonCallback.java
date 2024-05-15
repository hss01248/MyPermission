package com.hss01248.location.wifi;

public interface WifiCommonCallback<T> {

    void onSuccess(T t);

    void onFail(String code,String msg,Throwable throwable);
}

