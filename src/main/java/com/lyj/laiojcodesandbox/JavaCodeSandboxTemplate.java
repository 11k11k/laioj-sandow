package com.lyj.laiojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.lyj.laiojcodesandbox.model.ExecuteCodeRequest;
import com.lyj.laiojcodesandbox.model.ExecuteCodeResponse;
import com.lyj.laiojcodesandbox.model.ExecuteMessage;
import com.lyj.laiojcodesandbox.model.JudgeInfo;

import com.lyj.laiojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Java 代码沙箱模板方法的实现
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        //获取前端发过来的数据：输入用例，代码，编程语言
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

//        1. 把用户的代码保存为文件
        //创建文件夹和定义文件名字并存入文件内容和设置编码的类型，使用了hutool工具
        File userCodeFile = saveCodeToFile(code);

//        2. 编译代码，得到 class 文件
        //返回执行命令行后的信息类，
        // 使用进程类执行命令行代码
        // 将执行后的信息使用Process进行获取
        //封装工具类对Process的信息进行处理，并且获取到执行的代码时间并存入到执行消息类
        // 将数据封装进执行消息类进行返回
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        //打印执行信息
        System.out.println(compileFileExecuteMessage);

        // 3. 执行代码，得到输出结果
        //执行代码，将代码文件和输入用例传进去
        //使用进程类进行执行命令行，然后运行，使用Process类进行接收，
        //使用自定义工具类ProcessUtils将执行信息进行封装并存入到列表中
        //中途使用了for循环，循环将信息存入
        //并且使用了线程进行控制
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

//        4. 收集整理输出结果
        //返回响应信息
        //将运行后的信息传入后进行处理、
        //判断是否超时,从编译信息中遍历提取message内容存入
        //处理成响应给前端的数据
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

//        5. 文件清理
        //使用自定义的删除方法进行删除

        boolean b = deleteFile(userCodeFile);
        //如果删除失败
        if (!b) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }
        //返回响应结果
        return outputResponse;
    }


    /**
     * 1. 把用户的代码保存为文件
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code) {
        //获取当前用户所在的工作目录
        String userDir = System.getProperty("user.dir");
        //拼接路径，这里使用File.separator作为斜杠，方便不同系统的调用
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，没有则新建
        //使用引入的hutool工具类进行判断和创建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户的代码隔离存放
        //拼接文件夹的名称，最后进行uuid的数据生成文件夹名字，保证每次生成的文件夹不重复
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        //拼接文件夹名字
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        //使用hutool插件写入文件，参数为代码，文件名字和编译代码
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        //返回文件
        return userCodeFile;
    }

    /**
     * 2、编译代码
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        //构建命令行代码，用于对java代码进行编译，%s是占位符，代替的是文件的绝对路径，
        String compileCmd =
                String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            //使用进程类的exec进行执行cmd命令
            //执行 exec() 方法后，会返回一个表示正在运行的进程的 Process 对象，
            // 可以使用该对象来控制和监视进程的执行。
            // 这包括等待进程执行完成、获取进程的输入输出流、获取进程的退出值等操作。
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            //将执行的信息传入，获取到执行获得信息：执行的时间等
            ExecuteMessage executeMessage =
                    ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            //获取状态码进行判断
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            //返回执行信息类
            return executeMessage;
        } catch (Exception e) {
//            return getErrorResponse(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 3、执行文件，获得执行结果列表
     * @param userCodeFile 用户代码文件
     * @param inputList 前端输入用例
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        //获取代码的绝对路径
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        //新建集合
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        //循环输入用例
        for (String inputArgs : inputList) {
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
           //拼接命令行，使用%s占位符来置换所需要的数据
            //-Xmx256m设置jvm内存最大值，限制资源的分配
            //不得同于256M大小，会超过，需要再系统层面上实现
            String runCmd = String.format
                            ("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",
                                    userCodeParentPath, inputArgs);
            try {
                //执行命令行，返回Process类，进行获取执行信息
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制 运行超时，会中断运行的进程。
                new Thread(() -> {
                    try {
                        //先让进程进行睡眠
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        //睡醒了还没结束，就销毁进程
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                //使用自定义的工具类ProcessUtils将执行消息进行处理
                ExecuteMessage executeMessage = ProcessUtils
                        .runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                //这行代码将每次运行的执行消息添加到执行消息列表中。
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4、获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse  getOutputResponse(List<ExecuteMessage> executeMessageList) {
        //创建新的响应信息类，用于存入和返回
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        //新创建一个数组用于存储outputList
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        //将列表中的信息全部提取
        for (ExecuteMessage executeMessage : executeMessageList) {
            //获取到错误信息
            String errorMessage = executeMessage.getErrorMessage();
            //使用hutool工具类进行判空
            if (StrUtil.isNotBlank(errorMessage)) {
                //如果不为空，存入信息
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            //将获取到得到信息存入到输出列表中
            outputList.add(executeMessage.getMessage());
            //获取执行时间
            Long time = executeMessage.getTime();
            if (time != null) {
                //更新最大执行时间
                //max方法，比较最大值并返回
                maxTime = Math.max(maxTime, time);
            }
        }
        // 这行代码判断输出信息列表的大小是否等于执行消息列表的大小，
        // 如果相等表示所有执行消息都已处理完毕
        if (outputList.size() == executeMessageList.size()) {
            // 设置执行状态为 1（表示正常运行完成）。
            executeCodeResponse.setStatus(1);
        }
        //存入输出信息
        executeCodeResponse.setOutputList(outputList);
        //新new判题信息类，是自己做题的代码和时间，内存
        JudgeInfo judgeInfo = new JudgeInfo();
        //存入最大时间
        judgeInfo.setTime(maxTime);
        // 要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//        judgeInfo.setMemory();
        //将judgeInfo响应给前端
        executeCodeResponse.setJudgeInfo(judgeInfo);
        //返回响应类信息
        return executeCodeResponse;
    }

    /**
     * 5、删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        //判读代码文件的父目录是否存在
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            //使用hutool工具类根据目录地址进行删除
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 6、获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
