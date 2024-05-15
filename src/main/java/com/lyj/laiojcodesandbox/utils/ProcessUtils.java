package com.lyj.laiojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.lyj.laiojcodesandbox.model.ExecuteMessage;

import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtils {

    /**
     * 执行进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        //新建一个消息类：用于保存进行执行的信息

        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            //StopWatch是一种计时的工具，提供了停止，重置和获取经过时间等功能，可以测量代码运行时间
            //为了提供代码执行的时间
            StopWatch stopWatch = new StopWatch();
            //开始计时
            stopWatch.start();
            // 等待程序执行，获取错误码
            //通过返回的Process对象获取对应的编译信息
            int exitValue = runProcess.waitFor();
            //将信息存入到cmd执行信息类
            executeMessage.setExitValue(exitValue);
            // 正常退出
            //判断
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                // 分批获取进程的正常输出
                //字节流转换成字符流，进行逐行读取操作
                BufferedReader bufferedReader =
                        new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                //创建输出信息列表
                List<String> outputStrList = new ArrayList<>();
                // 逐行读取，将读取到的值存入到集合中
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compileOutputLine);
                }
                //存入编译执行类中的消息属性，使用了StringUtils工具类进行拼接
                executeMessage.setMessage(StringUtils.join(outputStrList, '\n'));
            } else {
                // 异常退出
                //打印执行失败的操作和错误码
                System.out.println(opName + "失败，错误码： " + exitValue);
                // 分批获取进程的正常输出
                BufferedReader bufferedReader
                        = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputStrList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compileOutputLine);
                }
                //通过StringUtils工具类进行拼接
                executeMessage.setMessage(StringUtils.join(outputStrList, '\n'));

                // 分批获取进程的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                // 逐行读取
                List<String> errorOutputStrList = new ArrayList<>();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorOutputStrList.add(errorCompileOutputLine);
                }
                //存入到错误信息
                executeMessage.setErrorMessage(StringUtils.join(errorOutputStrList, '\n'));
            }
            //停止计时
            stopWatch.stop();
            //获取最后任务执行的时间并存入执行时间
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //返回执行信息类：执行获得信息，时间，响应码等
        return executeMessage;
    }

    /**
     * 执行交互式进程并获取信息
     *
     * @param runProcess
     * @param args
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            // 相当于按了回车，执行输入的发送
            outputStreamWriter.flush();

            // 分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            // 记得资源的释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
}
