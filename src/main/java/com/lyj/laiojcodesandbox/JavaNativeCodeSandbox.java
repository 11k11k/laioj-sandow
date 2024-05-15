package com.lyj.laiojcodesandbox;


import cn.hutool.core.io.resource.ResourceUtil;
import com.lyj.laiojcodesandbox.model.ExecuteCodeRequest;
import com.lyj.laiojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Java 原生代码沙箱实现（直接复用模板方法）
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {

    public static void main(String[] args) {
//        测试代码，提供假数据进行测试
//        //创建java编译器沙箱
//        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
//        //实现接受前端的参数实体类
//        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
//        //存入输入实例
//        executeCodeRequest.setInputList(Collections.singletonList("1 2"));
//        //使用Hutool库的ResourceUtil工具对文件内容进行读取和选择编译码
//        String s = ResourceUtil.readStr("testCode/Main.java", StandardCharsets.UTF_8);
//        executeCodeRequest.setCode(s);
//        executeCodeRequest.setLanguage("java");
//        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
//        System.out.println(executeCodeResponse);
    }
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("这是在代码沙箱中前端的参数"+executeCodeRequest);
        return super.executeCode(executeCodeRequest);
    }
}
