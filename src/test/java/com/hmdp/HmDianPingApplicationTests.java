package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
@Slf4j
@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
private  RedisWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IUserService userService;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };

        long begin = System.currentTimeMillis();

        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }

        latch.await();

        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
        System.out.println(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
    }
    @Test
    public void createToken() {
        // 1. 使用配置文件路径
        String filePath = "D:\\token.txt";

        // 2. 使用try-with-resources确保资源关闭
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(filePath))) {
            List<User> list = userService.list();

            if (CollectionUtils.isEmpty(list)) {
                log.info("用户列表为空");
                return;
            }

            // 3. 可选：使用批量操作提高Redis性能
            // stringRedisTemplate.executePipelined(...)

            for (User user : list) {
                if (user == null) {
                    log.warn("发现null用户，跳过");
                    continue;
                }

                try {
                    // 4. 生成token
                    String token = UUID.randomUUID().toString().replace("-", ""); // 移除连字符

                    // 5. 转换为DTO
                    UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

                    // 6. 转换为Map，安全处理null值
                    Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                            CopyOptions.create()
                                    .setIgnoreNullValue(true)
                                    .setFieldValueEditor((fieldName, fieldValue) -> {
                                        if (fieldValue == null) {
                                            return "";
                                        }
                                        // 特殊类型处理
                                        if (fieldValue instanceof Date) {
                                            return String.valueOf(((Date) fieldValue).getTime());
                                        }
                                        return fieldValue.toString();
                                    }));

                    // 7. 存储到Redis
                    String tokenKey = LOGIN_USER_KEY + token;
                    stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
                    stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

                    // 8. 写入文件
                    printWriter.println(token);

                } catch (Exception e) {
                    log.error("处理用户 {} 失败: {}", user.getId(), e.getMessage());
                    // 继续处理其他用户
                }
            }

            // 9. 循环结束后flush一次
            printWriter.flush();

        } catch (IOException e) {
            log.error("文件写入失败: {}", e.getMessage());
            throw new RuntimeException("生成token文件失败", e);
        }
    }


}
