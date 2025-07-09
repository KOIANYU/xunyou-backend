package com.hgz.xunyoubackend.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void test() {
        RList<String> rList = redissonClient.getList("my-list");
        rList.add("KOIAN");
        System.out.println(rList.get(0));
    }

    @Test
    void lockTest() {
        RLock lock = redissonClient.getLock("xunyou:precachejob:cache:lock");

        try {
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                Thread.sleep(100000);
                System.out.println("Lock acquired" + Thread.currentThread().getId());
            }
        } catch (InterruptedException e) {
            System.out.println("Lock acquisition failed: " + e.getMessage());
        } finally {
            // 只有自己才能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


}
