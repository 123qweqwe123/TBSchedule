package me.hrps.schedule.zk;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.hrps.schedule.ScheduleUtils;
import me.hrps.schedule.config.annotation.TBScheduledAnnotationBeanPostProcessor;
import me.hrps.schedule.strategy.ManagerFactoryInfo;
import me.hrps.schedule.strategy.ScheduleStrategy;
import me.hrps.schedule.strategy.ScheduleStrategyRuntime;
import me.hrps.schedule.strategy.TBScheduleManagerFactory;
import me.hrps.schedule.taskmanager.ScheduledMethodRunnable;
import me.hrps.schedule.taskmanager.TBScheduledTaskProcessor;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/16 下午4:45
 */
public class ScheduleStrategyDataManager4ZK {

    private ZKManager zkManager;
    private CuratorFramework zkClient;
    private String PATH_Strategy;
    private String PATH_ManagerFactory;
    private String PATH_ScheduledTask;
    private Gson gson;


    public ScheduleStrategyDataManager4ZK(ZKManager zkManager) throws Exception {
        this.zkManager = zkManager;
        this.PATH_Strategy = "/strategy";
        this.PATH_ManagerFactory = "/factory";
        this.PATH_ScheduledTask = "/scheduledTask";
        gson = new GsonBuilder().registerTypeAdapter(Timestamp.class, new ScheduleDataManager4ZK.TimestampTypeAdapter()).setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        this.zkClient = zkManager.getClient();
        if (zkClient.checkExists().forPath(this.PATH_Strategy) == null) {
            zkClient.create().withMode(CreateMode.PERSISTENT).withACL(zkManager.getAclList()).forPath(this.PATH_Strategy);
        }
        if (zkClient.checkExists().forPath(this.PATH_ManagerFactory) == null) {
            zkClient.create().withMode(CreateMode.PERSISTENT).withACL(zkManager.getAclList()).forPath(this.PATH_ManagerFactory);
        }
        if (zkClient.checkExists().forPath(this.PATH_ScheduledTask) == null) {
            zkClient.create().withMode(CreateMode.PERSISTENT).withACL(zkManager.getAclList()).forPath(this.PATH_ScheduledTask);
        }

    }

    /**
     * 将 worker 注册到 factory 下面
     * 通过 ip 将 worker 注册到匹配的策略下面
     *
     * @param managerFactory
     * @return 不能匹配当前 factory 的策略
     * @throws Exception
     */
    public List<String> registerManagerFactory(TBScheduleManagerFactory managerFactory) throws Exception {
        if (managerFactory.getUuid() == null) {
            String uuid = managerFactory.getIp() + "$" + managerFactory.getHostName() + "$" +
                    ScheduleUtils.generateUUID();
            String factoryZkPath = this.PATH_ManagerFactory + "/" + uuid + "$";
            factoryZkPath = zkClient.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).withACL(zkManager.getAclList()).forPath(factoryZkPath);
            managerFactory.setUuid(StringUtils.substringAfterLast(factoryZkPath, "/"));
        } else {
            String factoryZkPath = PATH_ManagerFactory + "/" + managerFactory.getUuid();
            if (zkClient.checkExists().forPath(factoryZkPath) == null) {
                zkClient.create().withMode(CreateMode.EPHEMERAL).withACL(zkManager.getAclList()).forPath(factoryZkPath);
            }
        }
        List<String> result = Lists.newArrayList();
        for (ScheduleStrategy scheduleStrategy : loadAllScheduleStrategy()) {
            boolean isFind = false;
            if (!ScheduleStrategy.STS_PAUSE.equalsIgnoreCase(scheduleStrategy.getSts()) && scheduleStrategy.getIPList() != null) {
                for (String ip : scheduleStrategy.getIPList()) {
                    if (ip.equals("127.0.0.1") || ip.equalsIgnoreCase("localhost")
                            || ip.equals(managerFactory.getIp())
                            || ip.equalsIgnoreCase(managerFactory.getHostName())) {
                        String zkPath = PATH_Strategy + "/" + scheduleStrategy.getStrategyName() + "/" + managerFactory.getUuid();
                        if (zkClient.checkExists().forPath(zkPath) == null) {
                            zkClient.create().withMode(CreateMode.EPHEMERAL).withACL(zkManager.getAclList()).forPath(zkPath);   // TODO:为什么有初始值?
                        }
                        isFind = true;
                        break;
                    }
                }
            }
            if (!isFind) {   // 清除原来注册的 factory
                String zkPath = PATH_Strategy + "/" + scheduleStrategy.getStrategyName() + "/" + managerFactory.getUuid();
                if (zkClient.checkExists().forPath(zkPath) != null) {
                    zkClient.delete().forPath(zkPath);
                    result.add(scheduleStrategy.getStrategyName());
                }
            }
        }
        return result;
    }

    /**
     * 获取策略目录下面配置的所有策略
     *
     * @return
     * @throws Exception
     */
    public List<ScheduleStrategy> loadAllScheduleStrategy() throws Exception {
        List<String> strategyNames = zkClient.getChildren().forPath(PATH_Strategy);
        return strategyNames.stream().map(this::loadScheduleStrategy).collect(Collectors.toList());
    }

    /**
     * 通过策略名称获取策略对象
     *
     * @param strategyName 策略名称
     * @return 当前策略对象
     */
    public ScheduleStrategy loadScheduleStrategy(String strategyName) {
        String strategyPath = this.PATH_Strategy + "/" + strategyName;
        try {
            if (zkClient.checkExists().forPath(strategyPath) == null) {
                return null;
            }
            String valueString = new String(zkClient.getData().forPath(strategyPath));
            return this.gson.fromJson(valueString, ScheduleStrategy.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取注册的工厂信息,并设置 worker 默认启动
     *
     * @param uuid
     * @return
     * @throws Exception
     */
    public ManagerFactoryInfo loadManagerFactoryInfo(String uuid) throws Exception {
        String factoryPath = PATH_ManagerFactory + "/" + uuid;
        if (zkClient.checkExists().forPath(factoryPath) == null) {
            throw new Exception("任务管理器不存在：" + uuid);
        }
        byte[] value = zkClient.getData().forPath(factoryPath);
        ManagerFactoryInfo factoryInfo = new ManagerFactoryInfo();
        factoryInfo.setUuid(uuid);
        if (value == null || value.length == 0) {
            factoryInfo.setStart(true);
        } else {
            factoryInfo.setStart(Boolean.parseBoolean(new String(value)));
        }
        return factoryInfo;
    }

    public void updateManagerFactoryInfo(String uuid, boolean isStart) throws Exception {
        String factoryPath = PATH_ManagerFactory + "/" + uuid;
        if (zkClient.checkExists().forPath(factoryPath) == null) {
            throw new Exception("任务管理器不存在：" + uuid);
        }
        zkClient.setData().forPath(factoryPath, Boolean.toString(isStart).getBytes());
    }

    /**
     * 注销任务，停止调度
     *
     * @param managerFactory
     */
    public void unRegisterManagerFactory(TBScheduleManagerFactory managerFactory) throws Exception {
        for (String strategyName : zkClient.getChildren().forPath(PATH_Strategy)) {
            String zkPath = PATH_Strategy + "/" + strategyName + "/" + managerFactory.getUuid();
            if (zkClient.checkExists().forPath(zkPath) != null) {
                zkClient.delete().deletingChildrenIfNeeded().forPath(zkPath);
            }
        }
    }


    /**
     * @param managerFactoryUUID
     * @return 包含当前 worker 的运行时策略信息
     * @throws Exception
     */
    public List<ScheduleStrategyRuntime> loadAllStrategyRuntimeByUUID(String managerFactoryUUID) throws Exception {
        List<ScheduleStrategyRuntime> result = Lists.newArrayList();
        for (String strategyName : this.zkClient.getChildren().forPath(this.PATH_Strategy)) {
            if (this.zkClient.checkExists().forPath(this.PATH_Strategy + "/" + strategyName + "/" + managerFactoryUUID) != null) {
                result.add(loadScheduleStrategyRuntime(strategyName, managerFactoryUUID));
            }
        }
        return result;
    }


    public ScheduleStrategyRuntime loadScheduleStrategyRuntime(String strategyName, String uuid) throws Exception {
        String zkPath = PATH_Strategy + "/" + strategyName + "/" + uuid;
        ScheduleStrategyRuntime strategyRuntime = null;

        if (zkClient.checkExists().forPath(zkPath) != null) {
            byte[] value = zkClient.getData().forPath(zkPath);
            if (StringUtils.isNotBlank(new String(value))) {
                strategyRuntime = this.gson.fromJson(new String(value), ScheduleStrategyRuntime.class);
                if (strategyRuntime == null) {
                    throw new Exception("gson 反序列化异常，对象为 null");
                } else if (strategyRuntime.getStrategyName() == null) {
                    throw new Exception("gson 反序列化异常，策略名称为 null");
                } else if (strategyRuntime.getUuid() == null) {
                    throw new Exception("gson 反序列化异常，uuid 为 null");
                }
            } else {
                strategyRuntime = new ScheduleStrategyRuntime();
                strategyRuntime.setStrategyName(strategyName);
                strategyRuntime.setUuid(uuid);
                strategyRuntime.setRequestNum(0);
                strategyRuntime.setMessage("");
            }
        }
        return strategyRuntime;
    }

    /**
     * 获取某个策略名称下面所有的 factory
     *
     * @param strategyName
     * @return
     * @throws Exception
     */
    public List<ScheduleStrategyRuntime> loadAllScheduleStrategyRuntimeByTaskType(String strategyName) throws Exception {
        List<ScheduleStrategyRuntime> result = Lists.newArrayList();
        String zkPath = PATH_Strategy + "/" + strategyName;
        if (this.zkClient.checkExists().forPath(zkPath) == null) {
            return result;
        }
        List<String> factoryIds = this.zkClient.getChildren().forPath(zkPath);
        factoryIds.sort(Comparator.comparing(o -> StringUtils.substringAfterLast(o, "$")));
        for (String factoryId : factoryIds) {
            result.add(loadScheduleStrategyRuntime(strategyName, factoryId));
        }
        return result;
    }

    /**
     * 更新请求数量
     *
     * @param strategyName
     * @param managerFactoryUUID
     * @param num
     */
    public void updateStrategyRuntimeRequestNum(String strategyName, String managerFactoryUUID, int num) throws Exception {
        String zkPath = PATH_Strategy + "/" + strategyName + "/" + managerFactoryUUID;
        ScheduleStrategyRuntime result = null;
        if (this.zkClient.checkExists().forPath(zkPath) != null) {
            result = this.loadScheduleStrategyRuntime(strategyName, managerFactoryUUID);
        } else {
            result = new ScheduleStrategyRuntime();
            result.setStrategyName(strategyName);
            result.setUuid(managerFactoryUUID);
            result.setMessage("");
        }
        result.setRequestNum(num);
        String value = this.gson.toJson(result);
        this.zkClient.setData().forPath(zkPath, value.getBytes());
    }


    public void unRegisterScheduleTasks(TBScheduleManagerFactory managerFactory) {
        String zkBasePath = this.PATH_ScheduledTask + "/" + managerFactory.getUuid();
        TBScheduledAnnotationBeanPostProcessor annotationProcessor = managerFactory.getApplicationContext().getBean(TBScheduledAnnotationBeanPostProcessor.class);
        Set<ScheduledMethodRunnable> tasks = annotationProcessor.getScheduledTasks();
        tasks.forEach(task -> {
            String zkPath = zkBasePath + "$" + task.getTaskName();
            try {
                if (this.zkClient.checkExists().forPath(zkPath) != null) {
                    this.zkClient.delete().forPath(zkPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 注册调度任务，名称为 factoryId$beanName$methodName
     *
     * @param managerFactory
     * @return
     * @throws Exception
     */
    public Set<ScheduledMethodRunnable> registerScheduledTasks(TBScheduleManagerFactory managerFactory, TBScheduledTaskProcessor taskProcessor) throws Exception {
        String zkBasePath = this.PATH_ScheduledTask + "/" + managerFactory.getUuid();
        TBScheduledAnnotationBeanPostProcessor annotationProcessor = managerFactory.getApplicationContext().getBean(TBScheduledAnnotationBeanPostProcessor.class);
        Set<ScheduledMethodRunnable> tasks = annotationProcessor.getScheduledTasks();
        tasks.forEach(task -> {
            String zkPath = zkBasePath + "$" + task.getTaskName();
            try {
                // 设置 processor
                task.setProcessor(taskProcessor);
                if (this.zkClient.checkExists().forPath(zkPath) == null) {
                    this.zkClient.create().withMode(CreateMode.EPHEMERAL).withACL(this.zkManager.getAclList()).forPath(zkPath);
                }
                // 设置数据
                this.zkClient.setData().forPath(zkPath, this.gson.toJson(task).getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return tasks;
    }


    public void refreshScheduledTask(TBScheduleManagerFactory managerFactory, ScheduledMethodRunnable task) throws Exception {
        String zkPath = this.PATH_ScheduledTask + "/" + managerFactory.getUuid() + "$" + task.getTaskName();
        if (this.zkClient.checkExists().forPath(zkPath) == null) {
            this.zkClient.create().withMode(CreateMode.EPHEMERAL).forPath(zkPath);
        }
        this.zkClient.setData().forPath(zkPath, this.gson.toJson(task).getBytes());
    }
}
