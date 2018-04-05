package me.hrps.schedule.taskmanager;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/20 下午2:12
 */
interface IScheduleProcessor {

    /**
     * 是否处理完毕内存中所有的数据
     *
     * @return
     */
    boolean isDealFinishAllData();

    /**
     * 判断进程是否处于休眠状态
     *
     * @return
     */
    boolean isSleeping();

    /**
     * 停止任务处理器
     *
     * @throws Exception
     */
    void stopSchedule() throws Exception;

    /**
     * 清除所有已经取到内存中的数据，在心跳线程失败的时候调用，避免数据重复
     */
    void clearAllHasFetchData();
}
