package com.hss01248.mypermissiondemo;

import java.lang.reflect.Method;

public interface ProxyCallback {

   default void before(Object proxy, Method method, Object[] args){}

   default void onResult(Object proxy,Object result, Method method, Object[] args){}

   default void onException(Object proxy,Throwable result, Method method, Object[] args){}

}
