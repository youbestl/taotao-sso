package com.taotao.sso.service;

import com.taotao.sso.mapper.UserMapper;
import com.taotao.sso.pojo.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created by JARVIS on 2017.6.18.
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

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
}
