package com.hss01248.location.rx;

/**
 * @Despciption todo
 * @Author hss
 * @Date 13/04/2022 20:05
 * @Version 1.0
 */
public class LocationFailException extends Exception{

    int type;
    String msg;
    boolean isFailBeforeReallyRequest = false;

    public LocationFailException setType(int type) {
        this.type = type;
        return  this;
    }

    public LocationFailException setMsg(String msg) {
        this.msg = msg;
        return  this;
    }

    public LocationFailException setFailBeforeReallyRequest(boolean failBeforeReallyRequest) {
        isFailBeforeReallyRequest = failBeforeReallyRequest;
        return this;
    }

    public LocationFailException() {
    }

    public LocationFailException(String message) {
        super(message);
        this.msg = message;
    }

    public LocationFailException(String message, Throwable cause) {
        super(message, cause);

    }

    public LocationFailException(Throwable cause) {
        super(cause);
        this.msg = cause.getClass().getSimpleName();
    }
}
