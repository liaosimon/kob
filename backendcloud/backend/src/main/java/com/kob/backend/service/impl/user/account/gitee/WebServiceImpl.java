package com.kob.backend.service.impl.user.account.gitee;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.user.account.gitee.utils.HttpClientUtil;
import com.kob.backend.service.user.account.gitee.WebService;
import com.kob.backend.utils.JwtUtil;
import okhttp3.*;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@Service
public class WebServiceImpl implements WebService {
    private final static String clientId = "c5d5622701599bd5fed4e09dbe21b5e50c067d741d03c25de38b3e86785a518c";
    private final static String clientSecret = "89a800d24f5470a9eaf56c55a8ff17a2ef6c731ecfe11cf9fd50bec4499d79e7";
    // todo mark
    private final static String redirectUri = "http://localhost:3000/api/user/account/gitee/web/receive_code/";
    private final static String applyAccessTokenUrl = "https://gitee.com/oauth/token";
    private final static String getUserInfoUrl = "https://gitee.com/api/v5/user?";
    private final static Random random = new Random();
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public JSONObject applyCode() {
        JSONObject resp = new JSONObject();

        // 随机字符串，防止 csrf 攻击
        StringBuilder state = new StringBuilder();
        for (int i = 0; i < 10; i ++ ) {
            state.append((char) (random.nextInt(10) + '0'));
        }
        resp.put("result", "success");
        // 存到redis里，有效期设置为10分钟
        redisTemplate.opsForValue().set(state.toString(), "true");
        redisTemplate.expire(state.toString(), Duration.ofMinutes(10));  // 10分钟
        String applyCodeUrl = "https://gitee.com/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&state=" + state;
        resp.put("apply_code_url", applyCodeUrl);
        System.out.println("applyCode: " + applyCodeUrl);
        return resp;
    }

    @Override
    public JSONObject receiveCode(String code, String state) {
        JSONObject resp = new JSONObject();
        resp.put("result", "failed");
        if (code == null || state == null) {
            return resp;
        }

        if (Boolean.FALSE.equals(redisTemplate.hasKey(state))) {
            return resp;
        }
        redisTemplate.delete(state);
        // 获取access_token
        List<NameValuePair> nameValuePairs = new LinkedList<>();
        nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
        nameValuePairs.add(new BasicNameValuePair("client_id", clientId));
        nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));
        nameValuePairs.add(new BasicNameValuePair("code", code));
        nameValuePairs.add(new BasicNameValuePair("redirect_uri", redirectUri));

        String getString = HttpClientUtil.post(applyAccessTokenUrl, nameValuePairs);
        if (null == getString) {
            return resp;
        }
        JSONObject getResp = JSONObject.parseObject(getString);
        String accessToken = getResp.getString("access_token");

        // 获取openid
        nameValuePairs = new LinkedList<>();
        nameValuePairs.add(new BasicNameValuePair("access_token", accessToken));

        getString = HttpClientUtil.get(getUserInfoUrl, nameValuePairs);
        if(null == getString) {
            return resp;
        }
        getResp = JSONObject.parseObject(getString);
        String openid = getResp.getString("id");

        if (accessToken == null || openid == null) {
            return resp;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid", openid);
        List<User> users = userMapper.selectList(queryWrapper);

        // 用户已经授权，自动登录
        if (null != users && !users.isEmpty()) {
            User user = users.get(0);
            // 生成jwt
            String jwt = JwtUtil.createJWT(user.getId().toString());

            resp.put("result", "success");
            resp.put("jwt_token", jwt);
            return resp;
        }
        String username = getResp.getString("name");
        // 40 * 40 的头像
        String photo = getResp.getString("avatar_url");

        if (null == username || null == photo) return resp;

        // 每次循环，用户名重复的概率为上一次的1/10
        for (int i = 0; i < 100; i ++) {
            QueryWrapper<User> usernameQueryWrapper = new QueryWrapper<>();
            usernameQueryWrapper.eq("username", username);
            if (userMapper.selectCount(usernameQueryWrapper) == 0) {
                break;
            }
            username += (char)(random.nextInt(10) + '0');
            if (i == 99) {
                return resp;
            }
        }
        User user = new User(null, username, null, photo, 1500, openid);
        userMapper.insert(user);
        // 生成 jwt
        String jwt = JwtUtil.createJWT(user.getId().toString());
        resp.put("result", "success");
        resp.put("jwt_token", jwt);
        return resp;
    }
}
