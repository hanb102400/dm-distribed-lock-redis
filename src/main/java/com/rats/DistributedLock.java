package com.rats;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

public class DistributedLock {


    private static final String LOCK_OK = "OK";
    private static final Long RELEASE_OK = 1L;
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final String DELKEY_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final JedisPool jedisPool;

    public DistributedLock(JedisPool jedisPool){
        this.jedisPool = jedisPool;
    }

    /**
     * 尝试获取分布式锁
     * 立即返回，不会等待
     *
     * <p>
     * SET resource_name my_random_value NX PX 10000 保持原子性
     * </p>
     *
     * @param lockKey    锁
     * @param requestId  请求标识
     * @param expireTime 超期时间
     * @return 是否获取成功
     */
    public  boolean tryLock(String lockKey, String requestId, long expireTime) {
        Jedis jedis = jedisPool.getResource();
        String result = jedis.set("lock:" + lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
        if (LOCK_OK.equals(result)) {
            return true;
        }
        return false;
    }


    /**
     * 尝试获取分布式锁,
     * 持续尝试获取，阻塞等待直到获取为止
     *
     * <p>
     * SET resource_name my_random_value NX PX 10000 保持原子性
     * </p>
     *
     * @param lockKey    锁
     * @param requestId  请求标识
     * @param expireTime 超期时间
     * @return 是否获取成功
     */
    public  boolean waitLock(String lockKey, String requestId, long expireTime,long acquireTimeout) {
        Jedis jedis = jedisPool.getResource();
        long start = System.currentTimeMillis();
        do{
            String result = jedis.set("lock:" + lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            if (LOCK_OK.equals(result)) {
                return true;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (System.currentTimeMillis() <  start + acquireTimeout);
        return false;
    }


    /**
     * 释放分布式锁
     *
     * 使用LUA脚本保持原子性
     *
     * @param lockKey 锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public  boolean releaseLock(String lockKey, String requestId) {
        Jedis jedis = jedisPool.getResource();
        Object result = jedis.eval(DELKEY_SCRIPT, Collections.singletonList( "lock:" + lockKey), Collections.singletonList(requestId));
        if (RELEASE_OK.equals(result)) {
            return true;
        }
        return false;
    }

}
