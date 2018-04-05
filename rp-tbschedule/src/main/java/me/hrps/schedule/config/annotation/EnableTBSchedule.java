package me.hrps.schedule.config.annotation;

import java.lang.annotation.*;

/**
 * Description:
 * <pre>
 *     是否开启淘宝定时任务，目前采用 spring-boot-starter 的方式自动装配
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/12 下午7:59
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface EnableTBSchedule {
}
