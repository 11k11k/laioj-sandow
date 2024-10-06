package com.lyj.laiojcodesandbox.controller;

import com.lyj.laiojcodesandbox.JavaNativeCodeSandbox;
import com.lyj.laiojcodesandbox.model.ExecuteCodeRequest;
import com.lyj.laiojcodesandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Slf4j
@RestController("/")
public class MainController {
    private static final String AUTH_REQUEST_HEADER="auth";
    private static final String AUTH_REQUEST_SECRET="secretKey";

    @Resource
    //根据名称注入java代码沙箱
    private JavaNativeCodeSandbox javaNativeCodeSandbox;
    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                    HttpServletResponse response) {
        log.info(String.valueOf(request));
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        System.out.println("这是前端的请求参数"+executeCodeRequest);
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }
}
