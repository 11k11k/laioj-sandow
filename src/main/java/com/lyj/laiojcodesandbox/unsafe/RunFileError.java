package com.lyj.laiojcodesandbox.unsafe;

import java.io.*;

/**
 * 运行其他程序（比如危险木马）
 */
public class RunFileError {

    public static void main(String[] args) throws InterruptedException, IOException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        //运行程序返回程序运行内容
        Process process = Runtime.getRuntime().exec(filePath);
        //等待子进程运行完成
        process.waitFor();

        // 分批获取进程的正常输出
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        // 逐行读取
        String compileOutputLine;
        while ((compileOutputLine = bufferedReader.readLine()) != null) {
            System.out.println(compileOutputLine);
        }
        System.out.println("执行异常程序成功");
    }
}
