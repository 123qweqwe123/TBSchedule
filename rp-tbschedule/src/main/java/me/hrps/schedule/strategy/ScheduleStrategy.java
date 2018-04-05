package me.hrps.schedule.strategy;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Description:
 * <pre>
 *     通过前端页面配置的调度策略
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/17 上午8:37
 */
public class ScheduleStrategy {

    public enum Kind {Schedule, Java, Bean}

    /**
     * 策略名称
     */
    private String strategyName;
    /**
     * IP地址
     */
    private String[] IPList;
    /**
     * 单JVM最大线程组数量，如果是0，则表示没有限制.每台机器运行的线程组数量 =总量/机器数
     */
    private int numOfSingleServer;
    /**
     * 最大线程组数量
     */
    private int assignNum;
    /**
     * 任务类型
     */
    private Kind kind;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 任务参数
     */
    private String taskParameter;

    /**
     * 服务状态：pause、resume
     */
    private String sts = STS_RESUME;

    public static String STS_RESUME = "resume";

    public static String STS_PAUSE = "pause";


    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static void main(String[] args) {
        System.out.println(new ScheduleStrategy().toString());
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String[] getIPList() {
        return IPList;
    }

    public void setIPList(String[] IPList) {
        this.IPList = IPList;
    }

    public int getNumOfSingleServer() {
        return numOfSingleServer;
    }

    public void setNumOfSingleServer(int numOfSingleServer) {
        this.numOfSingleServer = numOfSingleServer;
    }

    public int getAssignNum() {
        return assignNum;
    }

    public void setAssignNum(int assignNum) {
        this.assignNum = assignNum;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskParameter() {
        return taskParameter;
    }

    public void setTaskParameter(String taskParameter) {
        this.taskParameter = taskParameter;
    }

    public String getSts() {
        return sts;
    }

    public void setSts(String sts) {
        this.sts = sts;
    }

}
