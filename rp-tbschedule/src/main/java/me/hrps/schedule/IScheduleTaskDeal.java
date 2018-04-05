package me.hrps.schedule;

import java.util.Comparator;
import java.util.List;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/19 下午1:34
 */
public interface IScheduleTaskDeal<T> {

    /**
     * 根据条件，查询当前调度服务器可处理的任务
     * @param taskParameter 任务的自定义参数
     * @param ownSign   当前环境名称
     * @param taskItemNum   当前任务类型的任务队列数量
     * @param taskItemList  当前调度服务器，分配的可处理队列
     * @param eachFetchDataNum  每次获取数据的数量
     * @return
     * @throws Exception
     */
    List<T> selectTasks(String taskParameter, String ownSign,
                        int taskItemNum, List<TaskItemDefine> taskItemList,
                        int eachFetchDataNum) throws Exception;

    Comparator<T> getComparator();
}
