package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    RedisTemplate redisTemplate;
    @Override
    public List<ShopType> select() {
        //1.查询Redis中有没有该类型数据
        List<ShopType> list  = redisTemplate.opsForList().range("cache:shop:type", 0, -1);
        log.info("listRedis: " + list);
        //2.有，返回
        if (list !=  null  && list.size() != 0) {
            log.info("listRedisyou: " + list);
            return list;
        }

        //3.没有，查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList == null) {
            //数据库没有，返回错误
            return null;
        }
        //4.有，存入Redis中
        redisTemplate.opsForList().leftPushAll("cache:shop:type", typeList);
        //5.返回
        return typeList;
    }
}


