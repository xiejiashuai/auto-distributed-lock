package com.aihuishou.springframework.boot.lock.autoconfigure;

import org.springframework.context.annotation.Bean;

/**
 * an marker class
 * @author jiashuai.xie
 * @date Created in 2018/10/24 17:50
 */
public class Mark {

    public static class Marker{

    }

    @Bean
    public Marker marker(){
        return new Marker();
    }


}
