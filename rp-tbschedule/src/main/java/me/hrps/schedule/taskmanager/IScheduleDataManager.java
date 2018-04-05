package me.hrps.schedule.taskmanager;

import me.hrps.schedule.TaskItemDefine;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.Map;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/12 上午8:27
 */
public interface IScheduleDataManager {

    /**
     * 获取 zk 服务器的当前时间
     *
     * @return
     */
    long getSystemTime();


    ScheduleTaskType loadTaskTypeBaseInfo(String baseTaskType) throws Exception;

    /**
     * 清除已经过去的 OWN_SIGN 的自动生成的数据
     *
     * @param baseTaskType          任务类型
     * @param localIp
     * @param expireOwnSignInterval
     */
    void clearExpireTaskTypeRunningInfo(String baseTaskType, String localIp, double expireOwnSignInterval) throws Exception;

    void registerScheduleServer(ScheduleServer currentScheduleServer) throws Exception;

    /**
     * 清除已经过期的调度服务器信息
     *
     * @param taskType
     * @param expireTime
     */
    int clearExpireScheduleServer(String taskType, long expireTime) throws Exception;

    /**
     * 获取 task 下面的 server 列表
     *
     * @param taskType
     * @return
     * @throws Exception
     */
    List<String> loadScheduleServerNames(String taskType) throws Exception;


    boolean isLeader(String uuid, List<String> serverList);

    String getLeader(List<String> serverList);

    /**
     * 初始化任务项
     * 初始化成功之后，在任务项目录标记上 leader-server 的 id
     *
     * @param baseTaskType
     * @param ownSign
     * @param uuid         scheduleServer 的 id
     * @throws Exception
     */
    void initialRunningInfo4Static(String baseTaskType, String ownSign, String uuid) throws Exception;

    /**
     * taskItem 有值，值为 server leader 的 uuid
     *
     * @param baseTaskType
     * @param ownSign
     * @return
     * @throws Exception
     */
    boolean isInitialRunningInfoSuccess(String baseTaskType, String ownSign) throws Exception;

    Map<String, Stat> getCurrentServerStats(String taskType) throws Exception;

    /**
     * 获取 taskType 下面 server 节点的 version
     *
     * @param taskType
     * @return
     * @throws Exception
     */
    long getReloadTaskItemFlag(String taskType) throws Exception;

    void releaseDealTaskItem(String taskType, String uuid) throws Exception;

    /**
     * @param taskType
     * @return
     * @throws Exception
     */
    long updateReloadTaskItemFlag(String taskType) throws Exception;

    List<TaskItemDefine> reloadDealTaskItem(String taskType, String uuid) throws Exception;

    List<ScheduleTaskItem> loadAllTaskItem(String taskType) throws Exception;

    /**
     * 更新 scheduleServer 的心跳和版本号
     *
     * @param currentScheduleServer
     * @return
     * @throws Exception
     */
    boolean refreshScheduleServer(ScheduleServer currentScheduleServer) throws Exception;

    void unRegisterScheduleServer(String taskType, String uuid) throws Exception;

    void setInitialRunningInfoSuccess(String baseTaskType, String taskType, String uuid) throws Exception;

    int clearTaskItem(String taskType, List<String> serverList) throws Exception;

    void assignTaskItem(String taskType, String uuid, int maxTaskItemsOfOneThreadGroup, List<String> serverList) throws Exception;

}
