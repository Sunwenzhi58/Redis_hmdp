package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopList() {
        //1.从redis查询商铺列表缓存
        List<String> shopTypes = stringRedisTemplate.opsForList().range("cache:shopType:",0,9);
        //2.判断是否存在
        List<ShopType> shopTypesByRedis = new ArrayList<>();
        if (shopTypes.size() != 0) {
            //3.存在，直接返回
            for (String shopType: shopTypes){
                ShopType type = JSONUtil.toBean(shopType,ShopType.class);
                shopTypesByRedis.add(type);
            }
            return Result.ok(shopTypesByRedis);
        }

        //4.不存在，根据id查询数据库
        List<ShopType> shopTypesByMysql = query().orderByAsc("sort").list();;

        //5.将商铺类型存入到redis中
        for (ShopType shopType : shopTypesByMysql){
            String s = JSONUtil.toJsonStr(shopType);
            stringRedisTemplate.opsForList().rightPushAll("cache:shopType:",s);
        }

        //6.返回商铺类型信息
        return Result.ok(shopTypesByMysql);
    }
}
