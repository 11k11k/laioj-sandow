package com.lyj.laiojcodesandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 向服务器写文件（植入危险程序）
 */
public class WriteFileError {

    public static void main(String[] args) throws InterruptedException, IOException {
        //获取当前文件夹名称
        String userDir = System.getProperty("user.dir");
        //拼接文件名
        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        //定义错误代码
        String errorProgram = "java -version 2>&1";
        //写入文件中
        //Arrays.asList();将数组转换成列表
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
        System.out.println("写木马成功，你完了哈哈");
    }
}
