package com.aihuishou.framework.lock.core;

import java.util.concurrent.TimeUnit;

public interface Lock {


    /**
     * when get lock failed ,will fast return ,never retry
     *
     * @return if success to get lock return true otherwise return false
     */
    default boolean tryLock() {
        return false;
    }

    /**
     * @param retryTime retry time
     * @param unit
     * @return if success to get lock return true otherwise return false
     * @throws InterruptedException
     */
    default boolean tryLock(long retryTime, TimeUnit unit) {
        return false;
    }


    /**
     * release lock , if current thread is not the owner of lock , will throw {@link IllegalMonitorStateException}
     */
    default void unlock() {

    }

}
