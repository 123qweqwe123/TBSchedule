package me.hrps.schedule.taskmanager;

import com.google.common.collect.Lists;
import me.hrps.schedule.IScheduleTaskDeal;
import me.hrps.schedule.IScheduleTaskDealMulti;
import me.hrps.schedule.IScheduleTaskDealSingle;
import me.hrps.schedule.TaskItemDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static me.hrps.schedule.taskmanager.TBScheduleManager.StatisticsInfo;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/6 下午6:18
 */
public abstract class TBScheduleProcessor<T> implements IScheduleProcessor, Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(TBScheduleProcessor.class);

    List<Thread> threadList = Lists.newCopyOnWriteArrayList();
    protected List<T> taskList = Lists.newCopyOnWriteArrayList();

    protected TBScheduleManager scheduleManager;
    protected ScheduleTaskType taskTypeInfo;
    protected StatisticsInfo statisticsInfo;

    protected IScheduleTaskDeal<T> taskDealBean;

    boolean isStopSchedule = false;
    boolean isSleeping = false;
    // 是否可以批处理
    boolean isMultiTask = false;

    public TBScheduleProcessor(TBScheduleManager scheduleManager, IScheduleTaskDeal<T> taskDealBean, StatisticsInfo statisticsInfo) throws Exception {
        this.scheduleManager = scheduleManager;
        this.taskDealBean = taskDealBean;
        this.statisticsInfo = statisticsInfo;
        this.taskTypeInfo = this.scheduleManager.getTaskTypeInfo();
        if (this.taskDealBean instanceof IScheduleTaskDealSingle) {
            if (this.taskTypeInfo.getExecuteNumber() > 1) {
                this.taskTypeInfo.setExecuteNumber(1);
            }
            isMultiTask = false;
        } else {
            isMultiTask = true;
        }
        if (taskTypeInfo.getFetchDataNumber() < taskTypeInfo.getThreadNumber() * 10) {
            logger.warn("参数设置不合理，系统性能不佳。【每次从数据库获取的数量fetchnum】 >= 【线程数量threadnum】 *【最少循环次数10】 ");
        }
        for (int i = 0; i < taskTypeInfo.getThreadNumber(); i++) {
            this.startThread(i);
        }
    }

    private void startThread(int index) {
        Thread thread = new Thread(this);
        threadList.add(thread);
        String threadName = this.scheduleManager.getScheduleServer().getTaskType() + "-"
                + this.scheduleManager.getCurrentSerialNumber() + "-exe" + index;
        thread.setName(threadName);
        thread.start();
    }

    /**
     * 处理完一批数据休眠指定时间
     */
    protected void sleepAfterDealTasks() {
        if (taskTypeInfo.getSleepTimeInterval() > 0) {
            logger.trace("处理完一批数据休眠：{}", taskTypeInfo.getSleepTimeInterval());
            this.isSleeping = true;
            try {
                Thread.sleep(taskTypeInfo.getSleepTimeInterval());
            } catch (InterruptedException e) {
                logger.error("休眠时错误", e);
            }
            this.isSleeping = false;
            logger.trace("处理完一批数据恢复：{}");
        }
    }

    /**
     * 调用 taskBean 获取任务
     *
     * @return
     * @throws Exception
     */
    protected int loadTasks() throws Exception {
        List<TaskItemDefine> taskItemDefines = this.scheduleManager.getCurrentScheduleTaskItemList();
        if (taskItemDefines.size() > 0) {
            List<TaskItemDefine> tmpTaskItems = Lists.newArrayList();
            synchronized (taskItemDefines) {
                tmpTaskItems.addAll(taskItemDefines);
            }
            List<T> tmpTaskList = this.taskDealBean.selectTasks(
                    taskTypeInfo.getTaskParameter(),
                    scheduleManager.getScheduleServer().getOwnSign(),
                    scheduleManager.getTaskItemCount(),
                    tmpTaskItems,
                    taskTypeInfo.getFetchDataNumber()
            );
            if (tmpTaskList != null) {
                this.taskList.addAll(tmpTaskList);
            }
        } else {
            logger.debug("没有任务分配");
        }
        addFetchNumber(taskList.size(), "TBScheduleProcessor.loadScheduleData");
        return taskList.size();
    }

    protected void addFetchNumber(int size, String s) {
        this.statisticsInfo.addFetchDataCount(1);
        this.statisticsInfo.addFetchDataNum(size);
    }

    protected void addSuccessNum(long num, long spendTime, String addr) {
        this.statisticsInfo.addDealDataSucess(num);
        this.statisticsInfo.addDealSpendTime(spendTime);
    }

    protected void addFailNum(long num, long spendTime, String addr) {
        this.statisticsInfo.addDealDataFail(num);
        this.statisticsInfo.addDealSpendTime(spendTime);
    }

    protected void executeSingleTask(T task) {
        long startTime = scheduleManager.getScheduleDataManager().getSystemTime();
        try {
            if (((IScheduleTaskDealSingle<T>) taskDealBean).execute((T) task, scheduleManager.getScheduleServer().getOwnSign())) {
                addSuccessNum(1, scheduleManager.getScheduleDataManager().getSystemTime() - startTime,
                        this.getClass().getName());
            } else {
                addFailNum(1, scheduleManager.getScheduleDataManager().getSystemTime() - startTime,
                        this.getClass().getName() + ".run()");
            }
        } catch (Exception e) {
            addFailNum(1, scheduleManager.getScheduleDataManager().getSystemTime() - startTime,
                    this.getClass().getName() + ".run()");
        }
    }

    protected void executeMultiTask(T[] task) {
        long startTime = scheduleManager.getScheduleDataManager().getSystemTime();
        try {
            if (((IScheduleTaskDealMulti) taskDealBean).execute((T[]) task, scheduleManager.getScheduleServer().getOwnSign())) {
                addSuccessNum(1, scheduleManager.getScheduleDataManager().getSystemTime() - startTime,
                        this.getClass().getName() + ".run()");
            } else {
                addFailNum(1, scheduleManager.getScheduleDataManager().getSystemTime() - startTime,
                        this.getClass().getName() + ".run()");
            }
        } catch (Exception e) {
            addFailNum(1, scheduleManager.getScheduleDataManager().getSystemTime() - startTime,
                    this.getClass().getName() + ".run()");
            logger.warn("处理任务:{} 失败", task, e);
        }
    }

    /**
     * 批量获取任务
     *
     * @return
     */
    protected abstract T[] getScheduleTaskIdMulti();

    /**
     * 获取单个任务
     *
     * @return
     */
    protected abstract T getScheduleTaskId();

    protected abstract int loadScheduleData();

    @Override
    public boolean isDealFinishAllData() {
        return false;
    }

    @Override
    public boolean isSleeping() {
        return this.isSleeping;
    }

    @Override
    public void stopSchedule() throws Exception {
        // 设置停止调度的标识，调度线程发现这个标识，执行完当前任务后，就退出调度
        this.isStopSchedule = true;
        // 清除所有未处理任务，已经进入处理队列的，需要处理完毕
        this.taskList.clear();
    }

    @Override
    public void clearAllHasFetchData() {
        this.taskList.clear();
    }
}
