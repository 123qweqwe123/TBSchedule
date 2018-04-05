package me.hrps.schedule;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/20 下午2:57
 */
public interface IScheduleTaskDealSingle<T> extends IScheduleTaskDeal<T>{

    /**
     * 执行单个任务
     * @param task
     * @param ownSign
     * @return
     * @throws Exception
     */
    boolean execute(T task, String ownSign) throws Exception;
}
