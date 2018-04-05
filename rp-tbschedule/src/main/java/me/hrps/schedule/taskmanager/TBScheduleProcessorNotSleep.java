package me.hrps.schedule.taskmanager;

import com.google.common.collect.Lists;
import me.hrps.schedule.IScheduleTaskDeal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static me.hrps.schedule.taskmanager.TBScheduleManager.StatisticsInfo;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/20 下午2:40
 */
public class TBScheduleProcessorNotSleep<T> extends TBScheduleProcessor<T> {

    private static transient Logger logger = LoggerFactory.getLogger(TBScheduleProcessorNotSleep.class);

    private Comparator<T> taskComparator;

    /**
     * 正在执行的任务队列
     */
    private List<Object> runningTaskList = Lists.newCopyOnWriteArrayList();
    /**
     * 可能重复的任务队列
     */
    private List<T> maybeRepeatTaskList = Lists.newCopyOnWriteArrayList();

    private Lock lockFetchID = new ReentrantLock();
    private Lock lockFetchMultiID = new ReentrantLock();
    private Lock lockLoadData = new ReentrantLock();

    public TBScheduleProcessorNotSleep(TBScheduleManager scheduleManager, IScheduleTaskDeal<T> taskDealBean, StatisticsInfo statisticsInfo) throws Exception {
        super(scheduleManager, taskDealBean, statisticsInfo);
        this.taskComparator = new MYComparator(this.taskDealBean.getComparator());
    }

    /**
     * 装载数据
     */
    @Override
    protected int loadScheduleData() {
        lockLoadData.lock();
        try {
            if (this.taskList.size() > 0 || this.isStopSchedule) {
                return this.taskList.size();
            }
            sleepAfterDealTasks();
            putLastRunningTaskList();
            loadTasks();
            if (taskList.size() <= 0) {
                // 判断没有数据时是否退出调度
                if (this.scheduleManager.isContinueWhenData()) {
                    if (taskTypeInfo.getSleepTimeNoDate() > 0) {
                        this.isSleeping = true;
                        Thread.sleep(taskTypeInfo.getSleepTimeNoDate());
                        this.isSleeping = false;
                    }
                }
            }
            return this.taskList.size();
        } catch (Throwable t) {
            logger.error("获取任务数据错误", t);
            return 0;
        } finally {
            lockLoadData.unlock();
        }
    }

    /**
     * 将running队列的数据拷贝到可能重复的队列中
     */
    @SuppressWarnings("unchecked")
    private void putLastRunningTaskList() {
        lockFetchID.lock();
        try {
            this.maybeRepeatTaskList.clear();
            if (this.runningTaskList.size() == 0) {
                return;
            }
            Object[] tmpList = this.runningTaskList.toArray();
            for (int i = 0; i < tmpList.length; i++) {
                if (this.isMultiTask) {
                    T[] aTasks = (T[]) tmpList[i];
                    this.maybeRepeatTaskList.addAll(Arrays.asList(aTasks));
                } else {
                    this.maybeRepeatTaskList.add((T) tmpList[i]);
                }
            }
        } finally {
            lockFetchID.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isDealing(T aTask) {
        if (this.maybeRepeatTaskList.size() == 0) {
            return false;
        }
        T[] tmpList = (T[]) this.maybeRepeatTaskList.toArray();
        for (int i = 0; i < tmpList.length; i++) {
            if (this.taskComparator.compare(aTask, tmpList[i]) == 0) {
                this.maybeRepeatTaskList.remove(tmpList[i]);
                return true;
            }
        }
        return false;
    }

    @Override
    protected T[] getScheduleTaskIdMulti() {
        lockFetchMultiID.lock();
        try {
            if (this.taskList.size() == 0) {
                return null;
            }
            int size = taskList.size() > taskTypeInfo.getExecuteNumber() ? taskTypeInfo.getExecuteNumber() :
                    taskList.size();
            List<T> result = Lists.newArrayList();
            int point = 0;
            T tmpObject = null;
            while (point < size
                    && (((tmpObject = this.getScheduleTaskId()) != null))) {
                result.add(tmpObject);
                point = point + 1;
            }
            if (result.size() == 0) {
                return null;
            } else {
                return result.toArray((T[]) Array.newInstance(result.get(0).getClass(), 0));
            }
        } finally {
            lockFetchMultiID.unlock();
        }
    }

    @Override
    protected T getScheduleTaskId() {
        lockFetchID.lock();
        try {
            T result = null;
            while (true) {
                if (this.taskList.size() > 0) {
                    result = this.taskList.remove(0);   // 按正序处理
                } else {
                    return null;
                }
                if (!this.isDealing(result)) {
                    return result;
                }
            }
        } finally {
            lockFetchID.unlock();
        }
    }

    @Override
    public void run() {
        Object executeTask = null;
        while (true) {
            try {
                // 停止队列调度
                if (this.isStopSchedule) {
                    synchronized (this.threadList) {
                        this.threadList.remove(Thread.currentThread());
                        if (this.threadList.size() == 0) {
                            this.scheduleManager.unRegisterScheduleServer();
                        }
                    }
                    return;
                }
                if (!this.isMultiTask) {
                    executeTask = this.getScheduleTaskId();
                } else {
                    executeTask = this.getScheduleTaskIdMulti();
                }
                if (executeTask == null) {
                    this.loadScheduleData();
                    continue;
                }

                try {
                    this.runningTaskList.add(executeTask);
                    if (this.isMultiTask) {
                        executeMultiTask((T[]) executeTask);
                    } else {
                        executeSingleTask((T) executeTask);
                    }
                } catch (Throwable t) {
                    logger.error("任务处理失败");
                }
            } catch (Exception e) {

            }
        }
    }

    class MYComparator implements Comparator<T> {

        Comparator<T> comparator;

        public MYComparator(Comparator<T> comparator) {
            this.comparator = comparator;
        }

        @Override
        public int compare(T o1, T o2) {
            statisticsInfo.addOtherCompareCount(1);
            return this.comparator.compare(o1, o2);
        }
    }

}
