package me.hrps.schedule.taskmanager;

import com.google.common.collect.Lists;
import me.hrps.schedule.CronExpression;
import me.hrps.schedule.IScheduleTaskDeal;
import me.hrps.schedule.ScheduleUtils;
import me.hrps.schedule.TaskItemDefine;
import me.hrps.schedule.strategy.IStrategyTask;
import me.hrps.schedule.strategy.TBScheduleManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Description: 任务调度器
 * <pre>
 *     <li>让所有任务不重复、不遗漏的快速处理</li>
 *     <li>一个 Manager 只管理一种任务类型的一组工作线程</li>
 *     <li>一个 jvm 里可能存在多个处理相同任务类型的 Manager 也可能存在处理不同类型的 Manager。</li>
 *     <li>在不同 jvm 里可能存在处理相同任务的 Manager</li>
 *     <li>调度的 Manager 可以随意增加和停止</li>
 * </pre>
 * <p>
 * 主要的职责：
 * <li>定时向集中的数据配置中心更新当前调度服务器的心跳状态</li>
 * <li>向数据配置中心获取所有服务器的状态来重新计算任务的分配</li>
 * <li>在每个批次的数据处理完毕后，检查是否有其它任务处理器申请自己把持的任务队列，如果有，释放给相关处理服务器</li>
 * </p>
 * <p>
 * 其它：
 * 如果当前服务器在处理当前任务时超市，需要清除当前队列，并释放已经把持的任务，并向控制主动中心报警
 * </p>
 * Author: huangrupeng
 * Create: 17/7/17 下午1:12
 */
abstract class TBScheduleManager implements IStrategyTask {

    private static transient Logger logger = LoggerFactory.getLogger(TBScheduleManager.class);

    /**
     * 用户标识不同线程的序号
     */
    private static int nextSerialNumber = 0;

    protected int currentSerialNumber = 0;

    TBScheduleManagerFactory factory;

    protected IScheduleDataManager scheduleDataManager;

    protected ScheduleTaskType taskTypeInfo;
    protected IScheduleTaskDeal taskDealBean;

    protected ScheduleServer currentScheduleServer;

    private String mBeanName;

    private Timer heartBeatTimer;

    protected boolean isStopSchedule = false;
    protected Lock registerLock = new ReentrantLock();

    /**
     * 运行期信息是否初始化成功
     */
    protected boolean isRuntimeInfoInitial = false;

    /**
     * 最近一次重新装载调度任务的时间
     */
    protected long lastReloadTaskItemListTime = 0;
    protected boolean isNeedReloadTaskItem = true;

    protected String startErrorInfo = null;

    boolean isPauseSchedule = true;
    String pauseMessage = "";

    IScheduleProcessor processor;

    StatisticsInfo statisticsInfo = new StatisticsInfo();

    protected List<TaskItemDefine> currentTaskItemList = Lists.newCopyOnWriteArrayList();

    TBScheduleManager(TBScheduleManagerFactory factory, String baseTaskType, String ownSign, IScheduleDataManager scheduleCenter) throws Exception {
        this.factory = factory;
        this.currentSerialNumber = serialNumber();
        this.scheduleDataManager = scheduleCenter;
        this.taskTypeInfo = this.scheduleDataManager.loadTaskTypeBaseInfo(baseTaskType);
        this.scheduleDataManager.clearExpireTaskTypeRunningInfo(baseTaskType, ScheduleUtils.getLocalIp(), this.taskTypeInfo.getExpireOwnSignInterval());

        Object dealBean = factory.getBean(this.taskTypeInfo.getDealBeanName());
        if (dealBean == null) {
            throw new Exception("SpringBean：" + this.taskTypeInfo.getDealBeanName() + " 不存在");
        }
        if (!(dealBean instanceof IScheduleTaskDeal)) {
            throw new Exception("SpringBean：" + this.taskTypeInfo.getDealBeanName() + "没有实现 IScheduleTaskDeal 接口 ");
        }
        this.taskDealBean = (IScheduleTaskDeal) dealBean;

        if (this.taskTypeInfo.getJudgeDeadInterval() < this.taskTypeInfo.getHeartBeatRate() * 5) {
            throw new Exception("数据配置存在问题，死亡的时间间隔，至少要大于心跳线程的5倍。当前配置数据：JudgeDeadInterval = "
                    + this.taskTypeInfo.getJudgeDeadInterval()
                    + ",HeartBeatRate = " + this.taskTypeInfo.getHeartBeatRate());
        }
        this.currentScheduleServer = ScheduleServer.createScheduleServer(scheduleCenter, baseTaskType, ownSign, this.taskTypeInfo.getThreadNumber());
        this.currentScheduleServer.setManagerFactoryUUID(factory.getUuid());
        scheduleCenter.registerScheduleServer(this.currentScheduleServer);
        this.mBeanName = "pamirs:name=" + "schedule.ServerMananger." + this.currentScheduleServer.getUuid();
        this.heartBeatTimer = new Timer(this.currentScheduleServer.getTaskType() + "-" + this.currentSerialNumber + "-HeartBeat");
        this.heartBeatTimer.schedule(new HeartBeatTimerTask(this),
                new Date(System.currentTimeMillis() + 500), this.taskTypeInfo.getHeartBeatRate());
        initial();
    }

    /**
     * 对象创建时执行的初始化动作
     */
    protected abstract void initial();

    @Override
    public void initialTaskParameter(String strategyName, String taskParameter) throws Exception {

    }

    @Override
    public void stop(String strategyName) throws Exception {
        logger.info("停止服务器：{}", this.currentScheduleServer.getUuid());
        this.isPauseSchedule = false;
        if (this.processor != null) {
            this.processor.stopSchedule();
        } else {
            this.unRegisterScheduleServer();
        }
    }

    /**
     * 开始的时候执行第一次计算
     */
    public void computeStart() throws Exception {
        boolean isRunNow = false;
        if (taskTypeInfo.getPermitRunStartTime() == null) {
            isRunNow = true;
        } else {
            String tmpStartStr = this.taskTypeInfo.getPermitRunStartTime();
            if (tmpStartStr.toLowerCase().startsWith("startrun:")) {
                isRunNow = true;
                tmpStartStr = tmpStartStr.substring("startrun:".length());
            }
            CronExpression cExpStart = new CronExpression(tmpStartStr);
            Date current = new Date(this.scheduleDataManager.getSystemTime());
            Date firstStartTime = cExpStart.getNextValidTimeAfter(current);
            this.heartBeatTimer.schedule(
                    new PauseOrResumeScheduleTask(
                            this, this.heartBeatTimer, PauseOrResumeScheduleTask.TYPE_RESUME, tmpStartStr),
                    firstStartTime);
            this.currentScheduleServer.setNextRunStartTime(ScheduleUtils.formatDateTime(firstStartTime));
            if (this.taskTypeInfo.getPermitRunEndTime() == null ||
                    this.taskTypeInfo.getPermitRunEndTime().equals("-1")) {
                this.currentScheduleServer.setNextRunEndTime("当不能获取到数据的时候 pause");
            } else {
                try {
                    String tmpEndStr = this.taskTypeInfo.getPermitRunEndTime();
                    CronExpression cExpEnd = new CronExpression(tmpEndStr);
                    Date firstEndTime = cExpEnd.getNextValidTimeAfter(firstStartTime);  // 第一次结束时间
                    Date nowEndTime = cExpEnd.getNextValidTimeAfter(current);   // 当前结束时间
                    if (!nowEndTime.equals(firstEndTime) && current.before(nowEndTime)) {
                        isRunNow = true;
                        firstEndTime = nowEndTime;
                    }
                    this.heartBeatTimer.schedule(new PauseOrResumeScheduleTask(this, this.heartBeatTimer, PauseOrResumeScheduleTask.TYPE_PAUSE, tmpEndStr), firstEndTime);
                    this.currentScheduleServer.setNextRunEndTime(ScheduleUtils.formatDateTime(firstEndTime));
                } catch (Exception e) {
                    logger.error("计算第一次执行时间出现异常：{}", currentScheduleServer.getUuid(), e);
                    throw new Exception("计算第一次执行时间出现异常：" + currentScheduleServer.getUuid(), e);
                }
            }
        }
        if (isRunNow) {
            this.resume("开始服务立即启动");
        }
        this.rewriteScheduleInfo();
    }

    private void pause(String message) throws Exception {
        if (!this.isPauseSchedule) {
            this.isPauseSchedule = true;
            this.pauseMessage = message;
            logger.debug("暂停调度：serverId={}:{}", this.currentScheduleServer.getUuid(), this.statisticsInfo.getDealDescription());
            if (this.processor != null) {
                this.processor.stopSchedule();
            }
            rewriteScheduleInfo();
        }
    }

    @SuppressWarnings("unchecked")
    private void resume(String message) throws Exception {
        if (this.isPauseSchedule) {
            logger.debug("恢复调度：{}", this.currentScheduleServer.getUuid());
            this.isPauseSchedule = false;
            this.pauseMessage = message;
            if (this.taskDealBean != null) {
                if (this.taskTypeInfo.getProcessorType() != null &&
                        this.taskTypeInfo.getProcessorType().equalsIgnoreCase("NOTSLEEP")) {
                    this.taskTypeInfo.setProcessorType("NOTSLEEP");
                    this.processor = new TBScheduleProcessorNotSleep(this, taskDealBean, this.statisticsInfo);
                } else {
                    this.taskTypeInfo.setProcessorType("SLEEP");
                    this.processor = new TBScheduleProcessorSleep(this, taskDealBean, this.statisticsInfo);
                }
            }
            rewriteScheduleInfo();
        }
    }


    /**
     * 刷新 scheduleServer 数据
     * @throws Exception
     */
    protected void rewriteScheduleInfo() throws Exception {
        registerLock.lock();
        try {
            if (this.isStopSchedule) {
                logger.debug("外部命令终止调度，不再注册调度服务，避免遗留垃圾数据：{}", this.currentScheduleServer.getUuid());
                return;
            }
            if (startErrorInfo == null) {
                this.currentScheduleServer.setDealInfoDesc(this.pauseMessage + ":" + this.statisticsInfo.getDealDescription());
            } else {
                this.currentScheduleServer.setDealInfoDesc(startErrorInfo);
            }

            if (!this.scheduleDataManager.refreshScheduleServer(this.currentScheduleServer)) {
                this.clearMemoInfo();
                this.scheduleDataManager.registerScheduleServer(this.currentScheduleServer);
            }
        } finally {
            registerLock.unlock();
        }
    }

    protected void clearMemoInfo() {
        try {
            this.currentTaskItemList.clear();
            if (this.processor != null) {
                this.processor.clearAllHasFetchData();
            }
        } finally {
            // 设置内存里面的数据需要重新装载
            this.isNeedReloadTaskItem = true;
        }
    }

    public ScheduleTaskType getTaskTypeInfo() {
        return taskTypeInfo;
    }

    public ScheduleServer getScheduleServer() {
        return currentScheduleServer;
    }

    public int getCurrentSerialNumber() {
        return currentSerialNumber;
    }

    public IScheduleDataManager getScheduleDataManager() {
        return scheduleDataManager;
    }

    protected abstract void refreshScheduleServerInfo() throws Exception;


    private int serialNumber() {
        return nextSerialNumber++;
    }

    protected void unRegisterScheduleServer() throws Exception {
        registerLock.lock();
        try {
            if (this.processor != null) {
                this.processor = null;
            }
            if (this.isPauseSchedule) {
                // 暂停调度 不注销 manager 自己
                return;
            }
            logger.debug("注销服务器：{}" + this.currentScheduleServer.getUuid());
            this.isStopSchedule = true;
            // 取消心跳 Timer
            this.heartBeatTimer.cancel();
            // 从配置中心注销自己 删除 server 节点
            this.scheduleDataManager.unRegisterScheduleServer(
                    this.currentScheduleServer.getTaskType(),
                    this.currentScheduleServer.getUuid()
            );
        } finally {
            registerLock.unlock();
        }
    }

    public abstract List<TaskItemDefine> getCurrentScheduleTaskItemList();

    public abstract int getTaskItemCount();

    public boolean isContinueWhenData() throws Exception {
        if (isPauseWhenNoData()) {
            this.pause("没有数据，暂停调度");
            return false;
        } else {
            return true;
        }
    }

    /**
     * 当任务的执行结束时间配置没有配置或者等于 -1 时，没有数据暂停调度
     *
     * @return
     */
    public boolean isPauseWhenNoData() {
        if (this.currentTaskItemList.size() > 0
                && this.taskTypeInfo.getPermitRunStartTime() != null) {
            if (this.taskTypeInfo.getPermitRunEndTime() == null ||
                    this.taskTypeInfo.getPermitRunEndTime().equals("-1")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    static class HeartBeatTimerTask extends TimerTask {
        private Logger logger = LoggerFactory.getLogger(HeartBeatTimerTask.class);

        TBScheduleManager manager;

        public HeartBeatTimerTask(TBScheduleManager manager) {
            this.manager = manager;
        }

        @Override
        public void run() {
            try {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                manager.refreshScheduleServerInfo();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    static class PauseOrResumeScheduleTask extends TimerTask {

        private static transient Logger logger = LoggerFactory.getLogger(PauseOrResumeScheduleTask.class);
        public static int TYPE_PAUSE = 1;
        public static int TYPE_RESUME = 2;
        TBScheduleManager manager;
        Timer timer;
        int type;
        String cronTabExpress;

        public PauseOrResumeScheduleTask(TBScheduleManager manager, Timer timer, int type, String cronTabExpress) {
            this.manager = manager;
            this.timer = timer;
            this.type = type;
            this.cronTabExpress = cronTabExpress;
        }

        @Override
        public void run() {
            try {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                this.cancel();  // run 方法只执行一次
                Date current = new Date();
                CronExpression cExp = new CronExpression(cronTabExpress);
                Date nextTime = cExp.getNextValidTimeAfter(current);
                if (this.type == TYPE_PAUSE) {
                    manager.pause("到达终止时间，pause 调度");
                } else {
                    manager.resume("到达开始时间，resume 调度");
                    this.manager.getScheduleServer().setNextRunStartTime(ScheduleUtils.formatDateTime(nextTime));
                }
                this.timer.schedule(new PauseOrResumeScheduleTask(this.manager, this.timer, this.type, this.cronTabExpress), nextTime);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    static class StatisticsInfo {

        private AtomicLong fetchDataNum = new AtomicLong(0);    //读取次数
        private AtomicLong fetchDataCount = new AtomicLong(0);  //读取的数据量
        private AtomicLong dealDataSuccess = new AtomicLong(0);  //处理成功的数据量
        private AtomicLong dealDataFail = new AtomicLong(0);    //处理失败的数据量
        private AtomicLong dealSpendTime = new AtomicLong(0);   //处理总耗时,没有做同步，可能存在一定的误差
        private AtomicLong otherCompareCount = new AtomicLong(0);   //特殊比较的次数

        public void addFetchDataNum(long value) {
            this.fetchDataNum.addAndGet(value);
        }

        public void addFetchDataCount(long value) {
            this.fetchDataCount.addAndGet(value);
        }

        public void addDealDataSucess(long value) {
            this.dealDataSuccess.addAndGet(value);
        }

        public void addDealDataFail(long value) {
            this.dealDataFail.addAndGet(value);
        }

        public void addDealSpendTime(long value) {
            this.dealSpendTime.addAndGet(value);
        }

        public void addOtherCompareCount(long value) {
            this.otherCompareCount.addAndGet(value);
        }

        public String getDealDescription() {
            return "FetchDataCount=" + this.fetchDataCount
                    + ",FetchDataNum=" + this.fetchDataNum
                    + ",DealDataSucess=" + this.dealDataSuccess
                    + ",DealDataFail=" + this.dealDataFail
                    + ",DealSpendTime=" + this.dealSpendTime
                    + ",otherCompareCount=" + this.otherCompareCount;
        }
    }
}
