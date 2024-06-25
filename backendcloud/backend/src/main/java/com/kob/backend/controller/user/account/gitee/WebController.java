package com.kob.backend.controller.user.account.gitee;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.service.user.account.gitee.WebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WebController {
    @Autowired
    private WebService webService;

    @GetMapping("/api/user/account/gitee/web/apply_code/")
    public JSONObject applyCode() {
        return webService.applyCode();
    }

    @GetMapping("/api/user/account/gitee/web/receive_code/")
    public JSONObject receiveCode(@RequestParam Map<String, String> data) {
        String code = data.get("code");
        return webService.receiveCode(code);
    }

    @GetMapping("/favicon.ico")
    public String receiveCode() {
        return "";
    }
}

