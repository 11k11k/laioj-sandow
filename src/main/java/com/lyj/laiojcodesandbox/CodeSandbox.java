package com.lyj.laiojcodesandbox;


import com.lyj.laiojcodesandbox.model.ExecuteCodeRequest;
import com.lyj.laiojcodesandbox.model.ExecuteCodeResponse;

public interface CodeSandbox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
