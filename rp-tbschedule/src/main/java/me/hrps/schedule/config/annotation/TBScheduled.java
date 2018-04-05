package me.hrps.schedule.config.annotation;

import java.lang.annotation.*;

/**
 * Description:
 * <pre>
 *     标识一个方法为 TBSchedule 的调度任务
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/12 下午8:16
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TBScheduled {

    /**
     * 任务执行的 cron 表达式
     * @return
     */
    String cron() default "";
}
