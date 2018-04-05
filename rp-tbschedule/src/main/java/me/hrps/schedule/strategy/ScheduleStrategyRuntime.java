package me.hrps.schedule.strategy;

/**
 * Description:
 * <pre>
 *     运行时调度策略
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/17 下午3:56
 */
public class ScheduleStrategyRuntime {

    /**
     * 任务类型
     */
    private String strategyName;
    private String uuid;
    private String ip;

    private ScheduleStrategy.Kind kind;

    private String taskName;

    private String taskParameter;

    /**
     * 需要的任务数量
     */
    private int requestNum;
    /**
     * 当前任务数量
     */
    private int currentNum;

    private String message;

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public ScheduleStrategy.Kind getKind() {
        return kind;
    }

    public void setKind(ScheduleStrategy.Kind kind) {
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

    public int getRequestNum() {
        return requestNum;
    }

    public void setRequestNum(int requestNum) {
        this.requestNum = requestNum;
    }

    public int getCurrentNum() {
        return currentNum;
    }

    public void setCurrentNum(int currentNum) {
        this.currentNum = currentNum;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ScheduleStrategyRuntime{" +
                "strategyName='" + strategyName + '\'' +
                ", uuid='" + uuid + '\'' +
                ", ip='" + ip + '\'' +
                ", kind=" + kind +
                ", taskName='" + taskName + '\'' +
                ", taskParameter='" + taskParameter + '\'' +
                ", requestNum=" + requestNum +
                ", currentNum=" + currentNum +
                ", message='" + message + '\'' +
                '}';
    }
}
