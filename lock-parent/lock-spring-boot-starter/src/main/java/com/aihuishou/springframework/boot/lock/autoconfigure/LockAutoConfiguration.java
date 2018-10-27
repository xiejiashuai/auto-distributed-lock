package com.aihuishou.springframework.boot.lock.autoconfigure;

import com.aihuishou.framework.lock.aop.DistributedLockAspect;
import com.aihuishou.framework.lock.core.RedisLockManager;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * lock auto configuration
 * @author jiashuai.xie
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean(value = {Mark.Marker.class,RedisTemplate.class})
public class LockAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean(DistributedLockAspect.class)
    public DistributedLockAspect distributedLockAspect(RedisLockManager redisLockManager){
        DistributedLockAspect distributedLockAspect=new DistributedLockAspect();
        distributedLockAspect.setLockManager(redisLockManager);
        return distributedLockAspect;
    }

    @Bean
    @ConditionalOnMissingBean(RedisLockManager.class)
    public RedisLockManager redisLockManager(RedisTemplate redisTemplate){
        RedisLockManager redisLockManager=new RedisLockManager();
        if (redisTemplate.getKeySerializer() instanceof  JdkSerializationRedisSerializer){
            redisTemplate.setValueSerializer(new StringRedisSerializer());
        }
        redisLockManager.setRedisTemplate(redisTemplate);
        return redisLockManager;
    }


}
