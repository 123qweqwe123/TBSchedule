# TBSchedule

一个简洁的分布式任务调度引擎 http://code.taobao.org/p/tbschedule/wiki/index/

## 特性

- 使用了 curator 对 TBSchedule 进行了重构，支持原 TBSchedule 的全部特性
- 添加了 spring-boot-starter，无缝集成 springBoot，支持配置自动补全
- 添加了 `@TBScheduled` 注解，在 SpringBoot `@Scheduled` 注解的基础上添加了如下特性
    * 支持设置任务参数
    * 能查看所有的任务列表
    * 支持动态设置任务的执行时间
    * 支持立即执行任务
    * 可以查看任务执行状态


## @TBScheduled 任务使用

```
git clone https://github.com/javahuang/TBSchedule.git
mvn clean install
```

```
<dependency>
    <groupId>me.hrps</groupId>
    <artifactId>tbschedule-spring-boot-starter</artifactId>
    <version>1.0.3</version>
</dependency>
```


``` java
@TBScheduleComponent
public class ScheduledBean {

    @TBScheduled(cron = "0 6 0 * * ?")
    @Override
    public void scheduledTask(String taskparam) {
        ...
    }

```

配置自动补全
![tbschedule-springboot](http://o6mo1i54c.bkt.clouddn.com/tbschedule-springboot.gif)

在
![springboot-scheduled-zk](http://o6mo1i54c.bkt.clouddn.com/springboot-scheduled-zk.gif)
![schedule](http://o6mo1i54c.bkt.clouddn.com/schedule.gif)




