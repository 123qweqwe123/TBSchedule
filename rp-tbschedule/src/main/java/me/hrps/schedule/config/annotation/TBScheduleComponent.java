package me.hrps.schedule.config.annotation;

import java.lang.annotation.*;

/**
 * Description:
 * <pre>
 *     spring bean，为 TBSchedule 的任务 bean
 *     使用 @{@link ScheduleScannerRegistrarConfiguration} 注册
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/12 下午8:17
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TBScheduleComponent {
}
