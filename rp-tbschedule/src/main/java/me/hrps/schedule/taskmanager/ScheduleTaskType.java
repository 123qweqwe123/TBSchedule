package me.hrps.schedule.taskmanager;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description:
 * <pre>
 *     对应前端配置任务管理
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/19 上午9:27
 */
public class ScheduleTaskType implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 任务类型
     */
    private String baseTaskType;
    /**
     * 向配置中心更新心跳信息的频率
     */
    private long heartBeatRate = 5 * 1000;

    /**
     * 判断一个服务器死亡的周期，为了安全，至少是心跳周期的两倍以上
     */
    private long judgeDeadInterval = 1 * 60 * 1000;

    /**
     * 没有数据时休眠时间
     * TODO: 修改 date 为 data
     */
    private int sleepTimeNoDate = 500;

    /**
     * 每次数据处理完毕休眠时间
     */
    private int sleepTimeInterval = 0;

    /**
     * 每次获取数据的数量
     */
    private int fetchDataNumber = 500;


    /**
     * 批处理的时候，每次处理的数据量
     */
    private int executeNumber = 1;

    private int threadNumber = 5;

    /**
     * 处理模式
     */
    private String processorType = "SLEEP";

    /**
     * 允许执行的开始/结束时间 cron 表达式
     */
    private String permitRunStartTime;

    private String permitRunEndTime;

    /**
     * 清除过期环境的时间间隔，以天为单位
     * 即 baseTaskType 下面的 baseTask/subTask
     */
    private double expireOwnSignInterval = 1;

    /**
     * 处理任务的 BeanName
     */
    private String dealBeanName;

    private String taskParameter;

    private String taskKind = TASKKIND_STATIC;

    public static String TASKKIND_STATIC = "static";
    public static String TASKKIND_DYNAMIC = "dynamic";

    /**
     * 任务项数组
     */
    private String[] taskItems;

    /**
     * 每个线程组能处理的最大任务项数目
     */
    private int maxTaskItemsOfOneThreadGroup = 0;

    private long version;

    /**
     * 服务状态：pause、resume
     */
    private String sts = STS_RESUME;

    public static String STS_RESUME = "resume";
    public static String STS_PAUSE = "pause";


    public static String[] splitTaskItem(String str) {
        List<String> list = Lists.newArrayList();
        String REGEX = "(\\w+:?(\\{(\\w+=\\w+[,，]?)*\\})?)+";
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(str);
        while (m.find()) {
            list.add(m.group());
        }
        return list.toArray(new String[0]);
    }


    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getBaseTaskType() {
        return baseTaskType;
    }

    public void setBaseTaskType(String baseTaskType) {
        this.baseTaskType = baseTaskType;
    }

    public long getHeartBeatRate() {
        return heartBeatRate;
    }

    public void setHeartBeatRate(long heartBeatRate) {
        this.heartBeatRate = heartBeatRate;
    }

    public long getJudgeDeadInterval() {
        return judgeDeadInterval;
    }

    public void setJudgeDeadInterval(long judgeDeadInterval) {
        this.judgeDeadInterval = judgeDeadInterval;
    }

    public int getSleepTimeNoDate() {
        return sleepTimeNoDate;
    }

    public void setSleepTimeNoDate(int sleepTimeNoDate) {
        this.sleepTimeNoDate = sleepTimeNoDate;
    }

    public int getSleepTimeInterval() {
        return sleepTimeInterval;
    }

    public void setSleepTimeInterval(int sleepTimeInterval) {
        this.sleepTimeInterval = sleepTimeInterval;
    }

    public int getFetchDataNumber() {
        return fetchDataNumber;
    }

    public void setFetchDataNumber(int fetchDataNumber) {
        this.fetchDataNumber = fetchDataNumber;
    }

    public int getExecuteNumber() {
        return executeNumber;
    }

    public void setExecuteNumber(int executeNumber) {
        this.executeNumber = executeNumber;
    }

    public int getThreadNumber() {
        return threadNumber;
    }

    public void setThreadNumber(int threadNumber) {
        this.threadNumber = threadNumber;
    }

    public String getProcessorType() {
        return processorType;
    }

    public void setProcessorType(String processorType) {
        this.processorType = processorType;
    }

    public String getPermitRunStartTime() {
        return permitRunStartTime;
    }

    public void setPermitRunStartTime(String permitRunStartTime) {
        this.permitRunStartTime = permitRunStartTime;
        if (StringUtils.isBlank(permitRunStartTime)) {
            this.permitRunStartTime = null;
        }
    }

    public String getPermitRunEndTime() {
        return permitRunEndTime;
    }

    public void setPermitRunEndTime(String permitRunEndTime) {
        this.permitRunEndTime = permitRunEndTime;
        if (StringUtils.isBlank(permitRunEndTime)) {
            this.permitRunEndTime = null;
        }
    }

    public double getExpireOwnSignInterval() {
        return expireOwnSignInterval;
    }

    public void setExpireOwnSignInterval(double expireOwnSignInterval) {
        this.expireOwnSignInterval = expireOwnSignInterval;
    }

    public String getDealBeanName() {
        return dealBeanName;
    }

    public void setDealBeanName(String dealBeanName) {
        this.dealBeanName = dealBeanName;
    }

    public String getTaskParameter() {
        return taskParameter;
    }

    public void setTaskParameter(String taskParameter) {
        this.taskParameter = taskParameter;
    }

    public String getTaskKind() {
        return taskKind;
    }

    public void setTaskKind(String taskKind) {
        this.taskKind = taskKind;
    }

    public String[] getTaskItems() {
        return taskItems;
    }

    public void setTaskItems(String[] taskItems) {
        this.taskItems = taskItems;
    }

    public int getMaxTaskItemsOfOneThreadGroup() {
        return maxTaskItemsOfOneThreadGroup;
    }

    public void setMaxTaskItemsOfOneThreadGroup(int maxTaskItemsOfOneThreadGroup) {
        this.maxTaskItemsOfOneThreadGroup = maxTaskItemsOfOneThreadGroup;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getSts() {
        return sts;
    }

    public void setSts(String sts) {
        this.sts = sts;
    }

}
