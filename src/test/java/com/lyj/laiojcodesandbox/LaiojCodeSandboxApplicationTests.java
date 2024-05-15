package com.lyj.laiojcodesandbox;


import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;


class LaiojCodeSandboxApplicationTests {

    @Test
    void contextLoads() {
        // 创建一个模拟的HttpServletRequest对象
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 设置请求头
        request.addHeader("Content-Type", "application/json");

        // 获取请求头
        String contentType = request.getHeader("Content-Type");

        // 断言请求头的值是否正确
        assertEquals("application/json", contentType);
    }

}
