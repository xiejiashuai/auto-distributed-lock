package com.aihuishou.framework.lock.core;

import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import java.util.UUID;

/**
 * redis lock manager
 *
 * @author jiashuai.xie
 */
public class RedisLockManager {

    private RedisTemplate<Object, Object> redisTemplate;

    @PostConstruct
    public void init() throws Exception {
        if (redisTemplate == null) {
            throw new IllegalArgumentException("redisTemplate can't be null");
        }
    }

    public RedisLockManager() {

    }

    public RedisLockManager(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @param lockKey
     * @param lockExpireTime expire time
     * @return
     */
    public RedisLock getLock(String lockKey,long lockExpireTime) {
        return new ReentrantRedisDistributedLock(lockKey, lockExpireTime, redisTemplate);
    }

    public void setRedisTemplate(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
