package com.rats.test;

import com.rats.DistributedLock;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.UUID;

public class TestThread extends Thread {

    private TestService service;

    public TestThread(TestService service) {
        this.service = service;
    }

    @Override
    public void run() {
        service.seckill();
    }

    public static void mthod1(){
        TestService service = new TestService();
        for (int i = 0; i < 50; i++) {
            TestThread threadA = new TestThread(service);
            threadA.start();
        }
    }

    public static void mthod2(){
        JedisPoolConfig config = new JedisPoolConfig();
        // 设置最大连接数
        config.setMaxTotal(200);
        // 设置最大空闲数
        config.setMaxIdle(8);
        // 设置最大等待时间
        config.setMaxWaitMillis(1000 * 100);
        // 在borrow一个jedis实例时，是否需要验证，若为true，则所有jedis实例均是可用的
        config.setTestOnBorrow(true);
        JedisPool pool = new JedisPool(config, "127.0.0.1", 6379, 3000);

        DistributedLock lock = new DistributedLock(pool);
        String reqid = UUID.randomUUID().toString();
        lock.tryLock("xxx002", reqid, 10000);
        System.out.println("lock");

        lock.releaseLock("xxx002", reqid);
        System.out.println("release");
    }

    public static void main(String[] args) {
        mthod1();
    }
}

