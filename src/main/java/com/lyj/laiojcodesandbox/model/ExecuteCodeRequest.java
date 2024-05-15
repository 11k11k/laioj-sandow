package com.lyj.laiojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {
    /**
     * 接收前端请求参数
     * 1.代码code
     * 2.接收输入一组
     * 3.执行的编程语言
     */
    private List<String> inputList;
    private String code;
    private String language;
}
