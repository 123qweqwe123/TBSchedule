package me.hrps.schedule.strategy;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/6 下午9:20
 */
public interface IStrategyTask {

    void initialTaskParameter(String strategyName, String taskParameter) throws Exception;

    void stop(String strategyName) throws Exception;
}
