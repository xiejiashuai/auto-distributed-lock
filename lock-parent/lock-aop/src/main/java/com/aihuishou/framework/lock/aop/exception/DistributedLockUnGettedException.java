package com.aihuishou.framework.lock.aop.exception;

/**
 * @author jiashuai.xie
 */
public class DistributedLockUnGettedException extends RuntimeException {

    public DistributedLockUnGettedException() {
        super();
    }


    public DistributedLockUnGettedException(String message) {
        super(message);
    }


    public DistributedLockUnGettedException(String message, Throwable cause) {
        super(message, cause);
    }


    public DistributedLockUnGettedException(Throwable cause) {
        super(cause);
    }


}
