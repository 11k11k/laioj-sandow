package com.lyj.laiojcodesandbox;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.lyj.laiojcodesandbox.model.ExecuteCodeRequest;
import com.lyj.laiojcodesandbox.model.ExecuteCodeResponse;
import com.lyj.laiojcodesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    private static final long TIME_OUT = 5000L;

    private static final Boolean FIRST_INIT = true;

    public static void main(String[] args) {
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        //setInputListreadStr
        String code = FileUtil.readString("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    /**
     * 3、创建容器，把文件复制到容器内
     * @param userCodeFile
     * @param inputList
     * @return
     */
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        //定义父目录文件夹名称
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // 拉取镜像
        String image = "openjdk:8-alpine";
        if (FIRST_INIT) {
            //通过java来控制docker
            //拉取镜像docker pull openjdk
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            //定义回调函数
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                //执行下一个代码
                @Override
                public void onNext(PullResponseItem item) {
                    //打印item的状态
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                //接收回调函数
                //会一直进行信息返回
                pullImageCmd
                        .exec(pullImageResultCallback)
                        //等待，知道拉取镜像完成
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }

        System.out.println("下载完成");

        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        //配置主机限制，内存，交换内存，cpu使用,安全管理配置，绑定文件夹
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                //使用容器配置
                .withHostConfig(hostConfig)
                //是否禁用网络
                .withNetworkDisabled(true)
                //只读操作
                .withReadonlyRootfs(true)
                //是否附加容器的标准输入
                .withAttachStdin(true)
//        此方法指定是否附加容器的标准错误 （stderr）。将其设置为表示将附加 stderr。true
                .withAttachStderr(true)
                //附加输出
                .withAttachStdout(true)
//
                .withTty(true)
                //此方法执行并返回一个对象，该对象包含有关新创建的容器的信息。
                .exec();
        System.out.println(createContainerResponse);
        //获取容器id
        String containerId = createContainerResponse.getId();

        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();

        // docker exec keen_blackwell java -cp /app Main 1 3
        // 执行命令并获取结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        //遍历输入用例
        for (String inputArgs : inputList) {
            //监听计时器
            StopWatch stopWatch = new StopWatch();
            //处理参数，使用空格分隔
            String[] inputArgsArray = inputArgs.split(" ");
            //拆分命令
            String[] cmdArray = ArrayUtil.append(
                    new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            //创建容器，并执行
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);
//            创建执行消息类
            ExecuteMessage executeMessage = new ExecuteMessage();
//           声明消息变量并初始化
            final String[] message = {null};
            final String[] errorMessage = {null};
//            声明long类型的time，并初始化为0
            long time = 0L;
            // 判断是否超时
            final boolean[] timeout = {true};
//            获取容器的id
            String execId = execCreateCmdResponse.getId();
//           创建回调函数
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    // 如果执行完成，则表示没超时
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};

            // 获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
//            统计结果回调
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(
                    new ResultCallback<Statistics>() {

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void close() throws IOException {

                }

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }
            });
            statsCmd.exec(statisticsResultCallback);
            try {
//                计时开始
                stopWatch.start();
//                开启容器并执行
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
//                        等待命令结束
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
//                计时结束
                stopWatch.stop();
//                获取最后一个任务时间单位并赋值给time
                time = stopWatch.getLastTaskTimeMillis();
//                关闭容器
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
//            存入消息
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }
        return executeMessageList;
    }
}



