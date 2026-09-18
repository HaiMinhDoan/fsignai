package com.sunmoon.backend.service;

public interface RedisService {
    void set(String key, Object value);

    void set(String key, Object value, long ttlSeconds);

    <T> T get(String key, Class<T> clazz);

    boolean delete(String key);

    boolean exists(String key);

    String buildKey(String prefix, String keyContent);
}


