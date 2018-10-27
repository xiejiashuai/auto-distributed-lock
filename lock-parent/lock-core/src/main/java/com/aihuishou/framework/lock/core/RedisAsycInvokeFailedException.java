package com.aihuishou.framework.lock.core;

public class RedisAsycInvokeFailedException extends RuntimeException {


    public RedisAsycInvokeFailedException() {

    }

    public RedisAsycInvokeFailedException(String message, Throwable t) {
        super(message, t);
    }

}
