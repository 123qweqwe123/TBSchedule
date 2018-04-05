package me.hrps.schedule.taskmanager;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/19 下午4:18
 */
public class ScheduleTaskItem {
    public enum TaskItemSts {
        ACTIVE, FINISH, HALT
    }

    private String taskType;

    /**
     * 原始任务类型
     */
    private String baseTaskType;

    private TaskItemSts sts = TaskItemSts.ACTIVE;

    /**
     * 任务处理需要的参数
     */
    private String dealParameter = "";

    /**
     * 任务完成情况
     */
    private String dealDesc = "";

    /**
     * 队列的环境标识
     */
    private String ownSign;

    /**
     * 主任务队列 ID
     */
    private String taskItem;

    /**
     * 持有当前任务队列的处理器
     */
    private String currentScheduleServer;

    /**
     * 正在申请此任务队列的任务处理器
     */
    private String requestScheduleServer;

    /**
     * 任务版本号
     */
    private long version;

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getBaseTaskType() {
        return baseTaskType;
    }

    public void setBaseTaskType(String baseTaskType) {
        this.baseTaskType = baseTaskType;
    }

    public TaskItemSts getSts() {
        return sts;
    }

    public void setSts(TaskItemSts sts) {
        this.sts = sts;
    }

    public String getDealParameter() {
        return dealParameter;
    }

    public void setDealParameter(String dealParameter) {
        this.dealParameter = dealParameter;
    }

    public String getDealDesc() {
        return dealDesc;
    }

    public void setDealDesc(String dealDesc) {
        this.dealDesc = dealDesc;
    }

    public String getOwnSign() {
        return ownSign;
    }

    public void setOwnSign(String ownSign) {
        this.ownSign = ownSign;
    }

    public String getTaskItem() {
        return taskItem;
    }

    public void setTaskItem(String taskItem) {
        this.taskItem = taskItem;
    }

    public String getCurrentScheduleServer() {
        return currentScheduleServer;
    }

    public void setCurrentScheduleServer(String currentScheduleServer) {
        this.currentScheduleServer = currentScheduleServer;
    }

    public String getRequestScheduleServer() {
        return requestScheduleServer;
    }

    public void setRequestScheduleServer(String requestScheduleServer) {
        this.requestScheduleServer = requestScheduleServer;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ScheduleTaskItem{" +
                "taskType='" + taskType + '\'' +
                ", baseTaskType='" + baseTaskType + '\'' +
                ", sts=" + sts +
                ", dealParameter='" + dealParameter + '\'' +
                ", dealDesc='" + dealDesc + '\'' +
                ", ownSign='" + ownSign + '\'' +
                ", taskItem='" + taskItem + '\'' +
                ", currentScheduleServer='" + currentScheduleServer + '\'' +
                ", requestScheduleServer='" + requestScheduleServer + '\'' +
                ", version=" + version +
                '}';
    }
}
