package com.rats.test;

import com.rats.DistributedLock;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.UUID;

public class TestService {

    private static JedisPool pool = null;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        // 设置最大连接数
        config.setMaxTotal(200);
        // 设置最大空闲数
        config.setMaxIdle(8);
        // 设置最大等待时间
        config.setMaxWaitMillis(1000 * 100);
        // 在borrow一个jedis实例时，是否需要验证，若为true，则所有jedis实例均是可用的
        config.setTestOnBorrow(true);
        pool = new JedisPool(config, "127.0.0.1", 6379, 3000);
    }

    DistributedLock lock = new DistributedLock(pool);

    int n = 500;

    public void seckill() {
        // 返回锁的value值，供释放锁时候进行判断
        String key = "001";
        String reqid = UUID.randomUUID().toString();
        if( lock.waitLock(key, reqid, 1000,1000) ) {
            System.out.println(Thread.currentThread().getName() + "获得锁成功");
        }else {
            System.out.println(Thread.currentThread().getName() + "---------------------获得锁失败");
        }
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(--n);
        if(lock.releaseLock(key, reqid)){
            System.out.println(Thread.currentThread().getName() + "释放锁");
        }
    }
}
