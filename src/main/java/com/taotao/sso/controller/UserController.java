package com.taotao.sso.controller;

import com.taotao.common.service.RedisService;
import com.taotao.common.utils.CookieUtils;
import com.taotao.sso.pojo.User;
import com.taotao.sso.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by JARVIS on 2017.6.18.
 */
@Controller
@RequestMapping("user")
public class UserController {

    private static final String COOKIE_NAME = "TT_TOKEN";

    @Autowired
    private UserService userService;
    /**
     * 注册
     * @return
     */
    @RequestMapping(value = "register",method = RequestMethod.GET)
    public String register() {
        return "register";
    }

    /**
     *  检测数据是否可用
     * @return
     */
    @RequestMapping(value = "{param}/{type}",method = RequestMethod.GET)
    public ResponseEntity<Boolean> check(@PathVariable("param")String param,
                                         @PathVariable("type")Integer type) {

        try {
            Boolean bool = userService.check(param,type);
            if(bool==null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            //为了兼容的逻辑，做出妥协
            return ResponseEntity.ok(!bool);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);

    }

    @RequestMapping(value = "doRegister",method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> doRegister(@Valid User user, BindingResult bindingResult) {
        Map<String, Object> result = new HashMap<>();
        if (bindingResult.hasErrors()) {
            ArrayList<String> msgs = new ArrayList<>();
            //参数有误，校验失败
            List<ObjectError> allErrors = bindingResult.getAllErrors();
            for (ObjectError allError : allErrors) {
                msgs.add(allError.getDefaultMessage());
            }
            result.put("status", "400");
            result.put("data", " 参数有误"+ StringUtils.join(msgs,"|"));
            return result;
        }
        try {
            Boolean bool = this.userService.doRegister(user);
            if (bool) {
                result.put("status", "200");
            }else{
                result.put("status", "500");
                result.put("data", " 呵呵~~~");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "500");
            result.put("data", " 呵呵~~~");
        }
        return result;
    }

    /**
     * 登录
     * @return
     */
    @RequestMapping(value = "login",method = RequestMethod.GET)
    public String toLogin() {
        return "login";
    }

    /**
     * 登录失败
     * @param user
     * @return
     */
    @RequestMapping(value = "doLogin",method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> doLogin(User user, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            String token = this.userService.doLogin(user.getUsername(),user.getPassword());
            if (StringUtils.isEmpty(token)) {
                result.put("status", 500);
                return result;
            }
            //登录成功将token保存到cookie
            CookieUtils.setCookie(request,response,COOKIE_NAME,token);
            result.put("status", 200);
            return result;
        } catch (Exception e) {
            //登录失败
            e.printStackTrace();
            result.put("status", 500);
            return result;
        }
    }

    /**
     * 根据 token 查询用户信息
     * @param token
     * @return
     */
    @RequestMapping(value = "{token}",method = RequestMethod.GET)
    public ResponseEntity<User> query(@PathVariable("token")String token) {
        try {
            User user = this.userService.queryUserByToken(token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

    @RequestMapping(value = "logout",method = RequestMethod.GET)
    public String logout(HttpServletRequest request,HttpServletResponse response) {
        try {
            String token = CookieUtils.getCookieValue(request,COOKIE_NAME);
            this.userService.logout(token);
            CookieUtils.deleteCookie(request,response,COOKIE_NAME);
            return "redirect:http://www.taotao.com/";
        } catch (Exception e) {
            e.printStackTrace();
        }
        //如果有异常，应该跳转到505页面
        return "redirect:http://www.taotao.com/";
    }

}
