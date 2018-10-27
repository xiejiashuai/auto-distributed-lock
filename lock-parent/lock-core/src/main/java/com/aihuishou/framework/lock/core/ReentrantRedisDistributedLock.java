package com.aihuishou.framework.lock.core;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Redis Implement of ILock
 *
 * @author jiashuai.xie
 */
public class ReentrantRedisDistributedLock implements RedisLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReentrantRedisDistributedLock.class);

    /**
     * default sleep time when get lock failed
     * ms
     */
    private static final long DEFAULT_INTERVAL_RETRY_TIME = 100;

    /**
     * default lock expire time
     */
    private static final long DEFAULT_EXPIRE_TIME = 30 * 1000L;

    /**
     * redis lock key
     */
    private final String lockKey;


    /**
     * redis lock expire time
     */
    private long lockExpireTime = DEFAULT_EXPIRE_TIME;

    /**
     * redisTemplate
     */
    private final RedisTemplate<Object, Object> redisTemplate;

    private final StringRedisSerializer keySerializer=new StringRedisSerializer();

    public ReentrantRedisDistributedLock(String lockKey, long lockExpireTime, RedisTemplate<Object, Object> redisTemplate) {
        this.lockKey = lockKey;
        if (lockExpireTime != 0) {
            this.lockExpireTime = lockExpireTime;
        }
        this.redisTemplate = redisTemplate;
    }


    @Override
    public boolean tryLock() {
        return tryLock(-1, TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean tryLock(long retryTime, TimeUnit unit) {
        return tryLock(lockExpireTime, retryTime, unit);

    }

    @Override
    public void unlock() {

        // eval == 1 mean un lock successfully
        // eval == 0 mean lock count - 1 successfully
        // eval == null mean to current thread isn't owner of lock
        Long eval = evalScript(UNLOCK_SCRIPT);

        //
        if (eval == null) {
            LOGGER.info("thread-{},failed to un-lock , because  current thread isn't owner of lock , lock-key:{},lock-value:{}", Thread.currentThread().getName(), getRedisKey(), getLockValue());
            throw new IllegalMonitorStateException("failed to unlock");
        }

        if (eval == 1) {
            LOGGER.info("thread-{},success to un-lock , lock-key:{},lock-value:{}", Thread.currentThread().getName(), getRedisKey(), getLockValue());
            return;
        }

        if (eval == 0) {
            LOGGER.info("thread-{},the lock counter decry 1 , lock-key:{},lock-value:{}", Thread.currentThread().getName(), getRedisKey(), getLockValue());
            return;
        }


    }

    private Long evalScript(String script) {

        return redisTemplate.execute((RedisCallback<Long>) connection -> {


            List<String> keys = new ArrayList<>();
            keys.add(getRedisKey());

            List<String> values = new ArrayList<>();
            values.add(getLockValue());
            values.add(String.valueOf(lockExpireTime));

            Object rawConnection = connection.getNativeConnection();


            Object result;

            if (rawConnection instanceof Jedis) {

                Jedis jedis = (Jedis) rawConnection;

                result = jedis.eval(script, keys, values);

            } else if (rawConnection instanceof JedisCluster) {

                JedisCluster jedisCluster = (JedisCluster) rawConnection;

                result = jedisCluster.eval(script, keys, values);

            } else {

                // instance of RedisClusterAsyncCommands<byte[], byte[]>

                RedisClusterAsyncCommands<byte[], byte[]> commands = (RedisClusterAsyncCommands<byte[], byte[]>) rawConnection;

                String[] keysArray = keys.toArray(new String[keys.size()]);

                String[] valuesArray = values.toArray(new String[values.size()]);

                RedisFuture redisFuture = commands.eval(script, ScriptOutputType.INTEGER, serializeKeyParams(keysArray), serializeValueParams(valuesArray));

                try {

                    result = redisFuture.get();

                } catch (InterruptedException | ExecutionException e) {

                    LOGGER.error("thread-{}, future invoke get failed , lock-key:{},lock-value:{} , msg:{}", Thread.currentThread().getName(), getRedisKey(), getLockValue(), e.getMessage(), e);

                    throw new RedisConnectionFailureException("future invoke get failed,failed to get lock");

                }

            }


            return (Long) result;


        });
    }

    /**
     * try to get lock
     *
     * @param expireTime the lock expire time
     * @param retryTime  when get lock failed , the retry time to get lock
     * @param unit       retry time unit
     * @return if success to get lock return true otherwise return false
     */
    protected boolean tryLock(long expireTime, long retryTime, TimeUnit unit) {

        Long ttl = tryAcquire(expireTime, TimeUnit.MILLISECONDS);

        // success to get lock
        if (null == ttl) {
            return true;
        }

        long waitTimeMillis = unit.toMillis(retryTime);
        if (waitTimeMillis <= 0) {
            return false;
        }

        LOGGER.info("thread-{},retry to get lock , lock-key:{},lock-value:{},retry-time:{}", Thread.currentThread().getName(), getRedisKey(), getLockValue(), waitTimeMillis);

        // retry get lock
        while (true) {

            ttl = tryAcquire(expireTime, unit);

            // lock acquired
            if (ttl == null) {
                return true;
            }

            if (waitTimeMillis <= 0) {
                return false;
            }

            LOGGER.info("thread-{},retry to get lock , lock-key:{},lock-value:{},retry-time:{}", Thread.currentThread().getName(), getRedisKey(), getLockValue(), waitTimeMillis);

            try {
                Thread.sleep(DEFAULT_INTERVAL_RETRY_TIME);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long elapsed = System.currentTimeMillis();

            waitTimeMillis -= elapsed;
        }

    }

    /**
     * @param expireTime 锁过期时间
     * @param unit       时间单位
     * @return
     */
    private Long tryAcquire(long expireTime, final TimeUnit unit) {

        // use client expire time otherwise use default
        if (expireTime >= 0) {
            this.lockExpireTime = unit.toMillis(expireTime);
        }

        Long eval = evalScript(LOCK_SCRIPT);

        // success to get lock
        if (eval == null) {

            LOGGER.info("thread-{},success to get lock , lock-key:{},lock-value:{}", Thread.currentThread().getName(), getRedisKey(), getLockValue());

            // TODO handler the lock key release early for example target method don't complete
        }

        return eval;

    }


    /**
     * serialize redis key params
     *
     * @param params
     * @return
     */
    protected byte[][] serializeKeyParams(Object... params) {

        byte[][] serialize = null;
        if (params != null && params.length > 0) {
            serialize = new byte[params.length][];
            for (int i = 0; i < params.length; i++) {
                serialize[i] = keySerializer.serialize(params[i] + "");
            }
        }
        return serialize;
    }

    /**
     * serialize redis key params
     *
     * @param params
     * @return
     */
    protected byte[][] serializeValueParams(Object... params) {

        byte[][] serialize = null;
        if (params != null && params.length > 0) {
            serialize = new byte[params.length][];
            for (int i = 0; i < params.length; i++) {
                serialize[i] = ((RedisSerializer<Object>) redisTemplate.getValueSerializer()).serialize(params[i] + "");
            }
        }
        return serialize;
    }


    /**
     * @return redis lock key
     */
    private String getRedisKey() {
        return lockKey;
    }

    /**
     * @return the redis lock value about key
     */
    private String getLockValue() {
        return String.valueOf(Thread.currentThread().getId());
    }


    /**
     * unlock script
     */
    private static final String UNLOCK_SCRIPT =
            "if (redis.call('exists', KEYS[1]) == 0) then " +
                    "return 1; " +
                    "end;" +
                    "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then " +
                    "return nil;" +
                    "end; " +
                    "local counter = redis.call('hincrby', KEYS[1], ARGV[1], -1); " +
                    "if (counter > 0) then " +
                    "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                    "return 0; " +
                    "else " +
                    "redis.call('del', KEYS[1]); " +
                    "return 1; " +
                    "end; " +
                    "return nil;";

    /**
     * lock script
     */
    private static final String LOCK_SCRIPT =
            "if (redis.call('exists', KEYS[1]) == 0) then " +
                    "redis.call('hset', KEYS[1], ARGV[1], 1); " +
                    "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                    "return nil; " +
                    "end; " +
                    "if (redis.call('hexists', KEYS[1], ARGV[1]) == 1) then " +
                    "redis.call('hincrby', KEYS[1], ARGV[1], 1); " +
                    "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                    "return nil; " +
                    "end; " +
                    "return redis.call('pttl', KEYS[1]);";


//    public static void main(String[] args) throws InterruptedException {
//
//        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
//        configuration.setHostName("10.255.184.10");
//        configuration.setPort(6379);
//
////        JedisConnectionFactory connectionFactory=new JedisConnectionFactory(configuration);
//        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(configuration);
//        connectionFactory.afterPropertiesSet();
//
//        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate();
//        redisTemplate.setConnectionFactory(connectionFactory);
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(new StringRedisSerializer());
//        redisTemplate.afterPropertiesSet();
//
//
//        ReentrantRedisDistributedLock reentrantRedisDistributedLock = new ReentrantRedisDistributedLock("test-lock", UUID.randomUUID().toString(), 10000, redisTemplate);
//
//        reentrantRedisDistributedLock.tryLock(50000, TimeUnit.MILLISECONDS);
//
////        reentrantRedisDistributedLock.unlock();
//
////        CountDownLatch countDownLatch = new CountDownLatch(1);
////
////        for (int i = 0; i <= 30; i++) {
////
////            Thread thread = new Thread(() -> {
////
////                try {
////                    countDownLatch.await();
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
////
////                boolean result = false;
////
////                result = reentrantRedisDistributedLock.tryLock(50000, TimeUnit.MILLISECONDS);
////                reentrantRedisDistributedLock.tryLock(50000, TimeUnit.MILLISECONDS);
////
////                if (result) {
////
////                } else {
////                    System.out.printf("[线程-%s]获取锁失败\n", Thread.currentThread().getName());
////
////                }
////
////
////                if (result) {
////                    reentrantRedisDistributedLock.unlock();
////                    reentrantRedisDistributedLock.unlock();
////
////                }
////
////
////            }, "thread-" + i);
////
////            thread.start();
////
////        }
////
////        countDownLatch.countDown();
////
////        Thread.currentThread().join();
//
//    }


}
