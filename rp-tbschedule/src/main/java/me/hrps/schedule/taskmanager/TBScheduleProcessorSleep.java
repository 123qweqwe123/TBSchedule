package me.hrps.schedule.taskmanager;

import me.hrps.schedule.IScheduleTaskDeal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;

import static me.hrps.schedule.taskmanager.TBScheduleManager.StatisticsInfo;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/6 下午5:26
 */
public class TBScheduleProcessorSleep<T> extends TBScheduleProcessor<T> {
    private static transient Logger logger = LoggerFactory.getLogger(TBScheduleProcessorSleep.class);

    final LockObject m_lockObject = new LockObject();

    public TBScheduleProcessorSleep(TBScheduleManager tbScheduleManager, IScheduleTaskDeal<T> taskDealBean,
                                    StatisticsInfo statisticsInfo) throws Exception {
        super(tbScheduleManager, taskDealBean, statisticsInfo);
    }

    @Override
    public void run() {
        try {
            while (true) {
                this.m_lockObject.addThread();
                Object executeTask;
                while (true) {
                    // 是否停止调度
                    if (this.isStopSchedule) {
                        this.m_lockObject.releaseThread();
                        this.m_lockObject.notifyOtherThread();
                        synchronized (this.threadList) {
                            this.scheduleManager.unRegisterScheduleServer();
                        }
                        return;
                    }

                    // 加载调度任务
                    if (this.isMultiTask) {
                        executeTask = getScheduleTaskIdMulti();
                    } else {
                        executeTask = getScheduleTaskId();
                    }
                    if (executeTask == null) {
                        break;
                    }

                    // 开始运行任务
                    if (isMultiTask) {
                        executeMultiTask((T[]) executeTask);
                    } else {
                        executeSingleTask((T) executeTask);
                    }
                }
                // 当前任务队列都已运行完毕
                logger.trace("{}: 当前运行线程数量:{}", Thread.currentThread().getName(), this.m_lockObject.count());
                // 任务处理完毕，且是最后一个线程，开始获取任务
                if (!this.m_lockObject.releaseThreadButNotLast()) {
                    int size = 0;
                    Thread.sleep(100);
                    // 装载数据
                    size = this.loadScheduleData();
                    if (size > 0) {
                        this.m_lockObject.notifyOtherThread();
                    } else {
                        // 判断当前没有数据时是否退出调度
                        if (!this.isStopSchedule && this.scheduleManager.isContinueWhenData()) {
                            logger.trace("没有装载到数据，start sleep");
                        }
                        this.isSleeping = true;
                        Thread.sleep(this.scheduleManager.getTaskTypeInfo().getSleepTimeNoDate());
                        this.isSleeping = false;
                        logger.trace("休眠结束");
                    }
                } else {
                    logger.trace("不是最后一个线程，sleep");
                    this.m_lockObject.waitCurrentThread();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected int loadScheduleData() {
        try {
            sleepAfterDealTasks();
            loadTasks();
            return taskList.size();
        } catch (Exception e) {
            logger.error("获取数据异常", e);
        }
        return 0;
    }

    @Override
    protected T[] getScheduleTaskIdMulti() {
        if (this.taskList.size() == 0) {
            return null;
        }
        int size = taskList.size() > taskTypeInfo.getExecuteNumber() ? taskTypeInfo.getExecuteNumber()
                : taskList.size();
        T[] result = null;
        if (size > 0) {
            result = (T[]) Array.newInstance(this.taskList.get(0).getClass(), size);
        }
        for (int i = 0; i < size; i++) {
            result[i] = this.taskList.remove(0);
        }
        return result;
    }

    @Override
    protected T getScheduleTaskId() {
        if (this.taskList.size() > 0) {
            return this.taskList.remove(0); // 按正序处理
        }
        return null;
    }
}
