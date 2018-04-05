package me.hrps.schedule.taskmanager;

import me.hrps.schedule.TaskItemDefine;
import me.hrps.schedule.strategy.TBScheduleManagerFactory;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/19 上午9:15
 */
public class TBScheduleManagerStatic extends TBScheduleManager {

    private static transient Logger logger = LoggerFactory.getLogger(TBScheduleManagerStatic.class);

    protected int taskItemCount = 0;

    protected long lastFetchVersion = -1;

    private final Object needReloadTaskItemLock = new Object();

    public TBScheduleManagerStatic(TBScheduleManagerFactory factory, String baseTaskType, String ownSign, IScheduleDataManager scheduleCenter) throws Exception {
        super(factory, baseTaskType, ownSign, scheduleCenter);
    }

    @Override
    protected void initial() {
        new Thread(this.currentScheduleServer.getTaskType() + "-" + this.currentSerialNumber + "-StartProcess") {
            @Override
            public void run() {
                try {
                    logger.debug("开始获取调度任务队列......of " + currentScheduleServer.getUuid());
                    while (!isRuntimeInfoInitial) {
                        if (isStopSchedule) {
                            logger.debug("外部命令终止调度，退出调度队列获取" + currentScheduleServer.getUuid());
                        }
                        try {
                            initialRunningInfo();
                            isRuntimeInfoInitial = scheduleDataManager.isInitialRunningInfoSuccess(currentScheduleServer.getBaseTaskType(),
                                    currentScheduleServer.getOwnSign());

                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        if (!isRuntimeInfoInitial) {
                            sleep(1000);
                        }
                    }
                    int count = 0;
                    lastReloadTaskItemListTime = scheduleDataManager.getSystemTime();
                    while (getCurrentScheduleTaskItemListNow().size() <= 0) {
                        if (isStopSchedule) {
                            logger.debug("外部命令终止调度,退出调度队列获取：" + currentScheduleServer.getUuid());
                            return;
                        }
                        sleep(1000);
                        count = count + 1;
                    }
                    StringBuilder tmpStr = new StringBuilder("TaskItemDefine:");
                    for (int i = 0; i < currentTaskItemList.size(); i++) {
                        if (i > 0) {
                            tmpStr.append(",");
                        }
                        tmpStr.append(currentTaskItemList.get(i));
                    }
                    logger.debug("获取到任务处理队列，开始调度：{} of {}", tmpStr.toString(), currentScheduleServer.getUuid());

                    // 任务总量
                    taskItemCount = scheduleDataManager.loadAllTaskItem(currentScheduleServer.getTaskType()).size();
                    // 只有在已经获取到任务处理队列后才开始启动任务管理器
                    computeStart();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    String str = e.getMessage();
                    if (str.length() > 300) {
                        str = str.substring(0, 300);
                    }
                    startErrorInfo = "启动处理异常：" + str;
                }
            }

        }.start();
    }


    private void initialRunningInfo() throws Exception {
        scheduleDataManager.clearExpireScheduleServer(this.currentScheduleServer.getTaskType(), this.taskTypeInfo.getJudgeDeadInterval());
        List<String> list = scheduleDataManager.loadScheduleServerNames(this.currentScheduleServer.getTaskType());
        if (this.scheduleDataManager.isLeader(this.currentScheduleServer.getUuid(), list)) {
            // 第一次启动，清除所有垃圾数据
            // logger.info("");
            this.scheduleDataManager.initialRunningInfo4Static(this.currentScheduleServer.getBaseTaskType(),
                    this.currentScheduleServer.getOwnSign(), this.currentScheduleServer.getUuid());
        }
    }

    /**
     * 设置
     * 返回能被当前 server 处理的 taskItem
     *
     * @return
     * @throws Exception
     */
    protected synchronized List<TaskItemDefine> getCurrentScheduleTaskItemListNow() throws Exception {
        // 判断 server 列表是否存在僵尸 server
        Map<String, Stat> statMap = this.scheduleDataManager.getCurrentServerStats(this.currentScheduleServer.getTaskType());
        if (isExistZombieServer(this.currentScheduleServer.getTaskType(), statMap)) {
            logger.error("zombie serverList exists");
        }

        // 获取最新的 server 版本号
        this.lastFetchVersion = this.scheduleDataManager.getReloadTaskItemFlag(this.currentScheduleServer.getTaskType());
        logger.info("this.currentScheduleServer.getTaskType()={}, need reload = ", this.currentScheduleServer.getTaskType(), isNeedReloadTaskItem);
        try {
            // 持有当前 server 的 task_item 是否被别的 server 申请(req_server 不为空)，如果被申请，则设置 server 的 reload=true
            this.scheduleDataManager.releaseDealTaskItem(this.currentScheduleServer.getTaskType(), this.currentScheduleServer.getUuid());

            this.currentTaskItemList.clear();
            // 返回 能够被当前 scheduleServer 处理的 taskItem
            this.currentTaskItemList = this.scheduleDataManager.reloadDealTaskItem(this.currentScheduleServer.getTaskType(),
                    this.currentScheduleServer.getUuid());

            // 如果超过10个心跳周期还没有获取到调度队列，则报警
            if (this.currentTaskItemList.size() == 0 &&
                    scheduleDataManager.getSystemTime() - this.lastReloadTaskItemListTime
                            > this.taskTypeInfo.getHeartBeatRate() * 20) {
                StringBuffer buf = new StringBuffer();
                buf.append("调度服务器");
                buf.append(this.currentScheduleServer.getUuid());
                buf.append("[TASK_TYPE=");
                buf.append(this.currentScheduleServer.getTaskType());
                buf.append("]自启动以来，超过20个心跳周期，还 没有获取到分配的任务队列;");
                buf.append("  currentTaskItemList.size() =").append(currentTaskItemList.size());
                buf.append(" ,scheduleCenter.getSystemTime()=").append(this.scheduleDataManager.getSystemTime());
                buf.append(" ,lastReloadTaskItemListTime=").append(lastReloadTaskItemListTime);
                buf.append(" ,taskTypeInfo.getHeartBeatRate()=").append(taskTypeInfo.getHeartBeatRate() * 10);
                logger.warn(buf.toString());
            }

            if (this.currentTaskItemList.size() > 0) {
                this.lastReloadTaskItemListTime = this.scheduleDataManager.getSystemTime();
            }
            return this.currentTaskItemList;
        } catch (Throwable e) {
            this.lastFetchVersion = 01;
            if (e instanceof Exception) {
                throw (Exception) e;
            } else {
                throw new Exception(e);
            }
        }
    }

    private boolean isExistZombieServer(String taskType, Map<String, Stat> statMap) {
        final boolean[] exist = {false};
        statMap.forEach((serverId, stat) -> {
            if (this.scheduleDataManager.getSystemTime() - stat.getMtime() > this.taskTypeInfo.getHeartBeatRate() * 40) {
                logger.error("zombie server exists!server={},type={} 超过 40 次心跳周期未更新");
                exist[0] = true;
            }
        });
        return exist[0];
    }


    /**
     * 定时向数据配置中心更新当前服务器心跳信息
     * 如果发现本次更新时间已经超时了，服务器死亡的心跳周期，则不能再向服务器更新信息
     * 而应该当做新的服务器，重新注册
     */
    @Override
    protected void refreshScheduleServerInfo() throws Exception {
        try {
            // 刷新 scheduleServer 数据
            rewriteScheduleInfo();
            if (!this.isRuntimeInfoInitial) {
                return;
            }
            // 重新分配任务
            this.assignScheduleTask();

            // 判断是否需要重新加载任务队列，避免任务处理进程不必要的检查和等待
            boolean tmpNeedReload = this.isNeedReloadTaskItemList();
            if (tmpNeedReload != this.isNeedReloadTaskItem) {
                synchronized (needReloadTaskItemLock) {
                    this.isNeedReloadTaskItem = true;
                }
                rewriteScheduleInfo();
            }

            if (this.isPauseSchedule || this.processor != null && processor.isSleeping()) {
                // 如果服务已经暂停，则需要重新定时更新 cur_server 和 req_server
                // 如果没有暂停，一定不能调用
                this.getCurrentScheduleTaskItemListNow();
            }
        } catch (Exception e) {
            this.clearMemoInfo();
            throw e;
        }
    }

    /**
     * leader 重新分配任务时，在每个 server 释放原来占有的任务时，都会修改这个版本号
     *
     * @return
     * @throws Exception
     */
    private boolean isNeedReloadTaskItemList() throws Exception {
        return this.lastFetchVersion < this.scheduleDataManager.getReloadTaskItemFlag(this.currentScheduleServer.getTaskType());
    }

    /**
     * 根据当前调度服务器的信息，重新计算分配所有的调度任务
     *
     * @throws Exception
     */
    private void assignScheduleTask() throws Exception {
        // 清除失效的 scheduleServer
        scheduleDataManager.clearExpireScheduleServer(this.currentScheduleServer.getTaskType(), this.taskTypeInfo.getJudgeDeadInterval());
        List<String> serverList = scheduleDataManager.loadScheduleServerNames(this.currentScheduleServer.getTaskType());
        if (!scheduleDataManager.isLeader(this.currentScheduleServer.getUuid(), serverList)) {
            logger.trace("{}不是负责任务分配的 leader，直接返回", this.currentScheduleServer.getUuid());
            return;
        }
        scheduleDataManager.setInitialRunningInfoSuccess(this.currentScheduleServer.getBaseTaskType(), this.currentScheduleServer.getTaskType(), this.currentScheduleServer.getUuid());
        // 检查 taskItem 的 cur_server 是否在 server 列表中
        scheduleDataManager.clearTaskItem(this.currentScheduleServer.getTaskType(), serverList);
        // 将 server 分配给 taskItem 的 cur_server
        scheduleDataManager.assignTaskItem(this.currentScheduleServer.getTaskType(), this.currentScheduleServer.getUuid(), this.taskTypeInfo.getMaxTaskItemsOfOneThreadGroup(), serverList);
    }

    @Override
    public List<TaskItemDefine> getCurrentScheduleTaskItemList() {
        try {
            if (this.isNeedReloadTaskItem) {
                // 特别注意：需要判断数据队列是否已经空了，否则可能在队列切换的时候导致数据重复
                // 主要是线程不休眠就加载数据的时候一定需要这个判断
                if (this.processor != null) {
                    while (!this.processor.isDealFinishAllData()) {
                        Thread.sleep(50);
                    }
                }
                // 真正开始处理数据
                synchronized (needReloadTaskItemLock) {
                    this.getCurrentScheduleTaskItemListNow();
                    this.isNeedReloadTaskItem = false;
                }
            }
            this.lastReloadTaskItemListTime = this.scheduleDataManager.getSystemTime();
            return this.currentTaskItemList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getTaskItemCount() {
        return this.taskItemCount;
    }
}
