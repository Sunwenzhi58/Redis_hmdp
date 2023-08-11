package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import io.netty.handler.codec.json.JsonObjectDecoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
//        Shop shop = cacheClient
//                .queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);
        //逻辑过期解决缓存击穿
        Shop shop = cacheClient
                .queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL, TimeUnit.MINUTES);


        if (shop == null){
            return Result.fail("店铺不存在");
        }

        //7.返回
        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

//    public Shop queryWithLogicalExpire(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        //1.从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2.判断是否存在
//        if (StrUtil.isBlank(shopJson)) {
//            //3.不存在，直接返回null
//            return null;
//        }
//
//        //4.命中，需要先把json反序列化对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject)redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        //5.判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())){
//            //5.1未过期，直接返回店铺信息
//            return shop;
//
//        }
//        //5.2已过期，需要缓存重建
//        //6.缓存重建
//        //6.1获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        //6.2判断是否获取锁成功
//        if (isLock){
//            //6.3成功，开启独立线程，实现缓存重建
//            CACHE_REBUILD_EXECUTOR.submit( () -> {
//                try {
//                    //重建缓存
//                    this.saveShop2Redis(id, 20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }finally {
//                    //释放锁
//                    unlock(lockKey);
//                }
//
//            });
//        }
//
//        //6.4失败，返回过期的商铺信息
//        return shop;
//    }

//
//    public Shop queryWithMutex(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        //1.从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2.判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //3.存在，直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        //判断命中的是否是空值
//        if (shopJson == ""){
//            //返回错误信息
//            return null;
//        }
//
//        //实现缓存重建
//        //4.1获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            boolean islock = tryLock(lockKey);
//            //4.2判断获取是否成功
//            if (!islock){
//                //4.3失败，则休眠并重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            //4.4成功，根据id查询数据库
//            shop = getById(id);
//            //模拟重建的延迟
//            Thread.sleep(200);
//
//            //5.不存在，返回错误
//            if (shop == null){
//                //将空值写入redis
//                stringRedisTemplate.opsForValue().set(key, "",CACHE_NULL_TTL,TimeUnit.MINUTES);
//                //返回错误信息
//                return null;
//            }
//
//            //6.存在，写入redis
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }finally {
//            //7.释放互斥锁
//            unlock(lockKey);
//        }
//
//        //8.返回
//        return shop;
//    }

//    public Shop queryWithPassThrough(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        //1.从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2.判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //3.存在，直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        //判断命中的是否是空值
//        if (shopJson == ""){
//            //返回错误信息
//            return null;
//        }
//
//        //4.不存在，根据id查询数据库
//        Shop shop = getById(id);
//
//        //5.不存在，返回错误
//        if (shop == null){
//            //将空值写入redis
//            stringRedisTemplate.opsForValue().set(key, "",CACHE_NULL_TTL,TimeUnit.MINUTES);
//            //返回错误信息
//            return null;
//        }
//        //6.存在，写入redis
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);
//
//        //7.返回
//        return shop;
//    }

//    //尝试获取锁
//    private boolean tryLock(String key){
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//    //释放锁
//     private void unlock(String key){
//        stringRedisTemplate.delete(key);
//     }

//     public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
//         //1.查询店铺数据
//         Shop shop = getById(id);
//         Thread.sleep(200);
//         //2.封装逻辑过期
//         RedisData redisData = new RedisData();
//         redisData.setData(shop);
//         redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//         //3.写入redis
//         stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//     }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();

    }
}
