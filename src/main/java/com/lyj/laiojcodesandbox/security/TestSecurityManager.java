package com.lyj.laiojcodesandbox.security;

import cn.hutool.core.io.FileUtil;

import java.nio.charset.Charset;

/**
 * 测试安全管理器
 */
public class TestSecurityManager {

    public static void main(String[] args) {
        //设置当前安全管理器
        System.setSecurityManager(new MySecurityManager());
        //写入文件，触发安全管理器
        //在默认情况下，Java 安全管理器对文件操作不做限制，因此通常情况下会允许执行该操作。
        FileUtil.writeString("aa", "aaa", Charset.defaultCharset());
    }
}
