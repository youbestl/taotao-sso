package com.taotao.sso.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taotao.common.service.RedisService;
import com.taotao.common.utils.CookieUtils;
import com.taotao.sso.mapper.UserMapper;
import com.taotao.sso.pojo.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;

/**
 * Created by JARVIS on 2017.6.18.
 */
@Service
public class UserService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Integer REDIS_TIME = 60 * 30;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;

    public Boolean check(String param, Integer type) {
        User record = new User();
        switch (type) {
            case 1:
                record.setUsername(param);
                break;
            case 2:
                record.setPhone(param);
                break;
            case 3:
                record.setEmail(param);
                break;
            default:
                //参数有误
                return null;
        }
        return userMapper.selectOne(record) == null;
    }

    public Boolean doRegister(User user) {
        user.setId(null);
        user.setCreated(new Date());
        user.setUpdated(user.getCreated());
        //MD5加密处理
        user.setPassword(DigestUtils.md5Hex(user.getPassword()));
        return this.userMapper.insert(user) == 1;
    }

    public String doLogin(String username, String password) throws Exception{
        User record = new User();
        record.setUsername(username);
        User user = this.userMapper.selectOne(record);
        if(user == null){
            //用户名不存在
            return null;
        }
        if (!StringUtils.equals(DigestUtils.md5Hex(password),user.getPassword())) {
            //密码错误
            return null;
        }
        String token = DigestUtils.md5Hex(username + System.currentTimeMillis());

        //将用户保存到redis中
        redisService.set("TOKEN_" + token, MAPPER.writeValueAsString(user), REDIS_TIME);

        return token;
    }

    public User queryUserByToken(String token) {
        String key = "TOKEN_"+token;
        String jsonData = this.redisService.get(key);
        if (StringUtils.isEmpty(jsonData)) {
            return null;
        }
        try {
            //刷新用户的生存时间 --->设置token的缓存时间为，用户和服务器无交互后30分钟。
            this.redisService.expire(key, REDIS_TIME);
            return MAPPER.readValue(jsonData, User.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void logout(String token) {
        String key = "TOKEN_"+token;
        this.redisService.del(key);
    }
}
