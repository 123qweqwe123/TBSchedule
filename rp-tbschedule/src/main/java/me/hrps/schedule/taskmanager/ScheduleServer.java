package me.hrps.schedule.taskmanager;

import me.hrps.schedule.ScheduleUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Description:
 * <pre>
 *     调度服务器
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/19 下午1:40
 */
public class ScheduleServer {

    private String uuid;
    private long id;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 原始任务类型
     */
    private String baseTaskType;

    /**
     * 对运行环境的划分，进行调度任务和数据隔离。例如：开发环境、测试环境
     */
    private String ownSign;

    private String ip;

    private String hostName;

    private int threadNum;

    private Timestamp registerTime;

    private Timestamp heartBeatTime;

    private Timestamp lastFetchDataTime;

    private String dealInfoDesc;

    private String nextRunStartTime;

    private String nextRunEndTime;

    private Timestamp centerServerTime;

    private long version;

    private boolean isRegister;

    private String managerFactoryUUID;

    public static ScheduleServer createScheduleServer(IScheduleDataManager scheduleDataManager, String baseTaskType,
                                                      String ownSign, int threadNum) {
        ScheduleServer server = new ScheduleServer();
        server.baseTaskType = baseTaskType;
        server.ownSign = ownSign;
        server.taskType = ScheduleUtils.getTaskTypeFromBaseAndOwnSign(baseTaskType, ownSign);
        server.ip = ScheduleUtils.getLocalIp();
        server.registerTime = new Timestamp(scheduleDataManager.getSystemTime());
        server.threadNum = threadNum;
        server.heartBeatTime = null;
        server.dealInfoDesc = "调度初始化";
        server.version = 0;
        server.uuid = server.ip + "$" + ScheduleUtils.generateUUID();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyMMdd");
        String s = dateFormatter.format(new Date(scheduleDataManager.getSystemTime()));
        server.id = Long.parseLong(s) * 100000000
                + Math.abs(server.uuid.hashCode() % 100000000);
        return server;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public String getOwnSign() {
        return ownSign;
    }

    public void setOwnSign(String ownSign) {
        this.ownSign = ownSign;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public Timestamp getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Timestamp registerTime) {
        this.registerTime = registerTime;
    }

    public Timestamp getHeartBeatTime() {
        return heartBeatTime;
    }

    public void setHeartBeatTime(Timestamp heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }

    public Timestamp getLastFetchDataTime() {
        return lastFetchDataTime;
    }

    public void setLastFetchDataTime(Timestamp lastFetchDataTime) {
        this.lastFetchDataTime = lastFetchDataTime;
    }

    public String getDealInfoDesc() {
        return dealInfoDesc;
    }

    public void setDealInfoDesc(String dealInfoDesc) {
        this.dealInfoDesc = dealInfoDesc;
    }

    public String getNextRunStartTime() {
        return nextRunStartTime;
    }

    public void setNextRunStartTime(String nextRunStartTime) {
        this.nextRunStartTime = nextRunStartTime;
    }

    public String getNextRunEndTime() {
        return nextRunEndTime;
    }

    public void setNextRunEndTime(String nextRunEndTime) {
        this.nextRunEndTime = nextRunEndTime;
    }

    public Timestamp getCenterServerTime() {
        return centerServerTime;
    }

    public void setCenterServerTime(Timestamp centerServerTime) {
        this.centerServerTime = centerServerTime;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public boolean isRegister() {
        return isRegister;
    }

    public void setRegister(boolean register) {
        isRegister = register;
    }

    public String getManagerFactoryUUID() {
        return managerFactoryUUID;
    }

    public void setManagerFactoryUUID(String managerFactoryUUID) {
        this.managerFactoryUUID = managerFactoryUUID;
    }
}
