package me.hrps.schedule;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/7 上午10:03
 */
public interface IScheduleTaskDealMulti<T> extends IScheduleTaskDeal<T>{

    boolean execute(T[] tasks, String ownSign) throws Exception;
}
