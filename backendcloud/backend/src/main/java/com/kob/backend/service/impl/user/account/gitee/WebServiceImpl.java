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
        StringBuilder state = new StringBuilder();
        for (int i = 0; i < 10; i ++ ) {
            state.append((char) (random.nextInt(10) + '0'));
        }
        resp.put("result", "success");
        redisTemplate.opsForValue().set(state.toString(), "true");
        redisTemplate.expire(state.toString(), Duration.ofMinutes(10));  // 10分钟
        String applyCodeUrl = "https://gitee.com/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code";
        resp.put("apply_code_url", applyCodeUrl);
        return resp;
    }

    @Override
    public JSONObject receiveCode(String code) {
        JSONObject resp = new JSONObject();
        resp.put("result", "failed");
        if (code == null) {
            return resp;
        }

        OkHttpClient client = new OkHttpClient();

        // 封装请求参数
        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("client_id", clientId)
                .add("redirect_uri", redirectUri)
                .add("client_secret", clientSecret)
                .build();

        Request request = new Request.Builder()
                .post(requestBody)
                .url(applyAccessTokenUrl).build();
        String accessToken = "";
        try {
            Response response = client.newCall(request).execute();
            String json = response.body().string();
            // 获取json串中的access_token属性
            accessToken = (String) JSONObject.parseObject(json).get("access_token");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (accessToken == null || accessToken.isEmpty()) {
            return resp;
        }

        List<NameValuePair> nameValuePairs = new LinkedList<>();
        nameValuePairs.add(new BasicNameValuePair("access_token", accessToken));
        String getString = HttpClientUtil.get(getUserInfoUrl, nameValuePairs);
        if (getString == null) {
            return resp;
        }
        JSONObject getResp = JSONObject.parseObject(getString);
        String id = getResp.getString("id");
        String username = getResp.getString("name");
        String photo = getResp.getString("avatar_url");

        if (username == null || photo == null || id == null) {
            return resp;
        }

//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("openid", id);
//        List<User> users = userMapper.selectList(queryWrapper);
//        if (!users.isEmpty()) {
//            User user = users.get(0);
//            String jwt = JwtUtil.createJWT(user.getId().toString());
//            resp.put("result", "success");
//            resp.put("jwt_token", jwt);
//            return resp;
//        }

        for (int i = 0; i < 100; i ++ ) {
            QueryWrapper<User> usernameQueryWrapper = new QueryWrapper<>();
            usernameQueryWrapper.eq("username", username);
            if (userMapper.selectList(usernameQueryWrapper).isEmpty()) {
                break;
            }
            username += (char)(random.nextInt(10) + '0');
            if (i == 99) {
                return resp;
            }
        }

        User user = new User(
                null,
                username,
                null,
                photo,
                1500,
                id
        );
        userMapper.insert(user);
        String jwt = JwtUtil.createJWT(user.getId().toString());
        resp.put("result", "success");
        resp.put("jwt_token", jwt);
        return resp;
    }
}
