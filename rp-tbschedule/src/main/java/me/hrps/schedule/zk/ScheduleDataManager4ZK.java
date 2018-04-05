package me.hrps.schedule.zk;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.*;
import me.hrps.schedule.ScheduleUtils;
import me.hrps.schedule.TaskItemDefine;
import me.hrps.schedule.taskmanager.IScheduleDataManager;
import me.hrps.schedule.taskmanager.ScheduleServer;
import me.hrps.schedule.taskmanager.ScheduleTaskItem;
import me.hrps.schedule.taskmanager.ScheduleTaskType;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNumeric;


/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/12 上午8:25
 */
public class ScheduleDataManager4ZK implements IScheduleDataManager {

    Logger logger = LoggerFactory.getLogger(ScheduleDataManager4ZK.class);

    private ZKManager zkManager;
    private CuratorFramework zkClient;
    private Gson gson;

    private String PATH_BaseTaskType;
    private String PATH_TaskItem = "taskItem";
    private String PATH_Server = "server";
    private long localBaseTime = 0;
    private long zkBaseTime = 0;


    /**
     * 创建 baseTaskType 节点，校验 Zookeeper 服务器时间
     *
     * @param zkManager
     */
    public ScheduleDataManager4ZK(ZKManager zkManager) throws Exception {
        this.zkManager = zkManager;
        this.zkClient = zkManager.getClient();
        this.PATH_BaseTaskType = "/baseTaskType";
        String tempTimeValidationPath = "/systime";
        if (zkClient.checkExists().forPath(PATH_BaseTaskType) == null) {
            zkClient.create().withMode(CreateMode.PERSISTENT).withACL(zkManager.getAclList()).forPath(PATH_BaseTaskType);
        }
        localBaseTime = System.currentTimeMillis();
        zkClient.create().withMode(CreateMode.EPHEMERAL).forPath(tempTimeValidationPath);
        zkBaseTime = zkClient.checkExists().forPath(tempTimeValidationPath).getCtime();
        zkClient.delete().forPath(tempTimeValidationPath);
        long diff = Math.abs(zkBaseTime - localBaseTime);
        if (diff > 5000) {
            logger.error("请注意：zk 服务器时间与本地时间相差：{} ms", diff);
        }
        gson = new GsonBuilder().registerTypeAdapter(Timestamp.class, new TimestampTypeAdapter()).setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    }

    /**
     * 获取 zk 服务器当前时间
     *
     * @return
     */
    @Override
    public long getSystemTime() {
        return this.zkBaseTime - this.localBaseTime + System.currentTimeMillis();
    }

    /**
     * 获取通过前端任务管理配置的任务信息
     *
     * @param baseTaskType
     * @return
     * @throws Exception
     */
    @Override
    public ScheduleTaskType loadTaskTypeBaseInfo(String baseTaskType) throws Exception {
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType;
        if (this.zkClient.checkExists().forPath(zkPath) == null) {
            return null;
        }
        String valueString = new String(this.zkClient.getData().forPath(zkPath));
        return this.gson.fromJson(valueString, ScheduleTaskType.class);
    }

    /**
     *
     * @param baseTaskType          任务类型
     * @param localIp
     * @param expireOwnSignInterval
     * @throws Exception
     */
    @Override
    public void clearExpireTaskTypeRunningInfo(String baseTaskType, String localIp, double expireOwnSignInterval) throws Exception {
        for (String name : this.zkClient.getChildren().forPath(this.PATH_BaseTaskType + "/" + baseTaskType)) {
            String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + name + "/" + this.PATH_TaskItem;
            Stat stat = this.zkClient.checkExists().forPath(zkPath);
            if (stat == null || getSystemTime() - stat.getMtime() > expireOwnSignInterval * 24 * 3600 * 1000) {
                this.zkClient.delete().deletingChildrenIfNeeded().forPath(this.PATH_BaseTaskType + "/" + baseTaskType + "/" + name);
            }
        }
    }

    @Override
    public void registerScheduleServer(ScheduleServer server) throws Exception {
        if (server.isRegister()) {
            throw new Exception(server.getUuid() + " 被重复注册");
        }
        String zkPath = this.PATH_BaseTaskType + "/" + server.getBaseTaskType() + "/" + server.getTaskType() + "/" + this.PATH_Server;
        if (this.zkClient.checkExists().forPath(zkPath) == null) {
            this.zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkPath);
        }

        String realPath = null;
        String zkServerPath = zkPath + "/" + server.getTaskType() + "$" + server.getIp() + "$" + ScheduleUtils.generateUUID() + "$";
        realPath = this.zkClient.create().withMode(CreateMode.PERSISTENT_SEQUENTIAL).withACL(this.zkManager.getAclList()).forPath(zkServerPath);
        server.setUuid(StringUtils.substringAfterLast(realPath, "/"));

        Timestamp heartBeatTime = new Timestamp(this.getSystemTime());
        server.setHeartBeatTime(heartBeatTime);

        String valueString = this.gson.toJson(server);
        this.zkClient.setData().forPath(realPath, valueString.getBytes());
        server.setRegister(true);
    }

    @Override
    public int clearExpireScheduleServer(String taskType, long expireTime) throws Exception {
        int result = 0;
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_Server;
        if (this.zkClient.checkExists().forPath(zkPath) == null) {
            this.zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkPath);
        }
        for (String name : this.zkClient.getChildren().forPath(zkPath)) {
            try {
                Stat stat = this.zkClient.checkExists().forPath(zkPath + "/" + name);
                if (getSystemTime() - stat.getMtime() > expireTime) {
                    this.zkClient.delete().deletingChildrenIfNeeded().forPath(zkPath + "/" + name);
                    result++;
                }
            } catch (Exception e) {
                result++;
            }
        }
        return result;
    }

    @Override
    public List<String> loadScheduleServerNames(String taskType) throws Exception {
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_Server;
        List<String> result = Lists.newArrayList();
        if (this.zkClient.checkExists().forPath(zkPath) == null) {
            return result;
        }
        List<String> serverNames = this.zkClient.getChildren().forPath(zkPath);
        serverNames.sort(Comparator.comparing(o -> Integer.parseInt(StringUtils.substringAfterLast(o, "$"))));
        return serverNames;
    }

    @Override
    public boolean isLeader(String uuid, List<String> serverList) {
        return uuid.equals(getLeader(serverList));
    }

    @Override
    public String getLeader(List<String> serverList) {
        if (serverList == null || serverList.size() == 0) {
            return "";
        }
        serverList.sort(Comparator.comparing(serverName -> Integer.parseInt(StringUtils.substringAfterLast(serverName, "$"))));
        return serverList.get(0);
    }

    @Override
    public void initialRunningInfo4Static(String baseTaskType, String ownSign, String uuid) throws Exception {
        String taskType = ScheduleUtils.getTaskTypeFromBaseAndOwnSign(baseTaskType, ownSign);
        // 清除所有的老信息，只有 leader 能执行此操作
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_TaskItem;
        try {
            this.zkClient.delete().deletingChildrenIfNeeded().forPath(zkPath);
        } catch (Exception e) {
            logger.warn("{} 不存在，删除失败", zkPath);
        }
        this.zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkPath);
        // 创建任务项
        this.createScheduleTaskItem(baseTaskType, ownSign, this.loadTaskTypeBaseInfo(baseTaskType).getTaskItems());
        // 标记信息初始化成功
        setInitialRunningInfoSuccess(baseTaskType, taskType, uuid);
    }

    /**
     * 设置 taskItem 数据为 scheduleServer-leader 的 uuid
     * <li>初始化时设置</li>
     * <li>定时任务设置</li>
     * @param baseTaskType
     * @param taskType
     * @param uuid
     * @throws Exception
     */
    @Override
    public void setInitialRunningInfoSuccess(String baseTaskType, String taskType, String uuid) throws Exception {
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_TaskItem;
        this.zkClient.setData().forPath(zkPath, uuid.getBytes());
    }

    /**
     * 根据前端配置的任务项来创建各个域里面的任务项
     *
     * @param baseTaskType
     * @param ownSign
     * @param baseTaskItems
     */
    private void createScheduleTaskItem(String baseTaskType, String ownSign, String[] baseTaskItems) throws Exception {
        ScheduleTaskItem[] taskItems = new ScheduleTaskItem[baseTaskItems.length];
        Pattern p = Pattern.compile("\\s*:\\s*\\{");

        for (int i = 0; i < baseTaskItems.length; i++) {
            taskItems[i] = new ScheduleTaskItem();
            taskItems[i].setBaseTaskType(baseTaskType);
            taskItems[i].setTaskType(ScheduleUtils.getTaskTypeFromBaseAndOwnSign(baseTaskType, ownSign));
            taskItems[i].setOwnSign(ownSign);
            taskItems[i].setSts(ScheduleTaskItem.TaskItemSts.ACTIVE);
            Matcher matcher = p.matcher(baseTaskItems[i]);
            if (matcher.find()) {
                taskItems[i].setTaskItem(baseTaskItems[i].substring(0, matcher.start()).trim());
                taskItems[i].setDealParameter(baseTaskItems[i].substring(matcher.end(), baseTaskItems[i].length() - 1).trim());
            } else {
                taskItems[i].setTaskItem(baseTaskItems[i]);
            }
        }
        createScheduleTaskItem(taskItems);
    }

    private void createScheduleTaskItem(ScheduleTaskItem[] taskItems) throws Exception {
        for (ScheduleTaskItem item : taskItems) {
            String zkPath = this.PATH_BaseTaskType + "/" + item.getBaseTaskType() + "/" + item.getTaskType() + "/" + this.PATH_TaskItem;
            if (this.zkClient.checkExists().forPath(zkPath) == null) {
                this.zkClient.create().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkPath);
            }
            String zkTaskItemPath = zkPath + "/" + item.getTaskItem();
            this.zkClient.create().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkTaskItemPath);
            this.zkClient.create().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkTaskItemPath + "/cur_server");
            this.zkClient.create().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkTaskItemPath + "/req_server");
            this.zkClient.create().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkTaskItemPath + "/sts", item.getSts().toString().getBytes());
            this.zkClient.create().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkTaskItemPath + "/parameter", item.getDealParameter().getBytes());
            this.zkClient.create().withMode(CreateMode.PERSISTENT).withACL(this.zkManager.getAclList()).forPath(zkTaskItemPath + "/deal_desc", item.getDealDesc().getBytes());
        }

    }

    @Override
    public boolean isInitialRunningInfoSuccess(String baseTaskType, String ownSign) throws Exception {
        String taskType = ScheduleUtils.getTaskTypeFromBaseAndOwnSign(baseTaskType, ownSign);
        String leader = this.getLeader(this.loadScheduleServerNames(taskType));
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_TaskItem;
        if (this.zkClient.checkExists().forPath(zkPath) != null) {
            byte[] curContent = this.zkClient.getData().forPath(zkPath);
            if (curContent != null && new String(curContent).equals(leader)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<String, Stat> getCurrentServerStats(String taskType) throws Exception {
        Map<String, Stat> serverStats = Maps.newHashMap();
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_Server;
        for (String server : this.zkClient.getChildren().forPath(zkPath)) {
            serverStats.put(server, this.zkClient.checkExists().forPath(zkPath + "/" + server));
        }
        return serverStats;
    }

    /**
     * 获取 server 的 version
     * @param taskType
     * @return
     * @throws Exception
     */
    @Override
    public long getReloadTaskItemFlag(String taskType) throws Exception {
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_Server;
        return this.zkClient.checkExists().forPath(zkPath).getVersion();
    }

    /**
     * 如果存在 taskItem 的 cur_server 被申请，重新装载任务
     * @param taskType
     * @param uuid
     * @throws Exception
     */
    @Override
    public void releaseDealTaskItem(String taskType, String uuid) throws Exception {
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_TaskItem;
        boolean isModify = false;
        for (String name : this.zkClient.getChildren().forPath(zkPath)) {
            byte[] curServerValue = this.zkClient.getData().forPath(zkPath + "/" + name + "/cur_server");
            byte[] reqServerValue = this.zkClient.getData().forPath(zkPath + "/" + name + "/req_server");
            if (reqServerValue != null && curServerValue != null && uuid.equals(new String(curServerValue))) {
                this.zkClient.setData().forPath(zkPath + "/" + name + "/cur_server", reqServerValue);
                this.zkClient.setData().forPath(zkPath + "/" + name + "/req_server", null);
                isModify = true;
            }
        }
        if (isModify) {
            this.updateReloadTaskItemFlag(taskType);
        }
    }

    /**
     * 重新装载任务(存在 taskItem 的 cur_server) 被修改过
     *
     * @param taskType
     * @return
     * @throws Exception
     */
    @Override
    public long updateReloadTaskItemFlag(String taskType) throws Exception {
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_Server;
        Stat stat = this.zkClient.setData().forPath(zkPath, "reload=true".getBytes());
        return stat.getVersion();
    }

    /**
     *
     * @param taskType
     * @param uuid schedule-server uuid
     * @return
     * @throws Exception
     */
    @Override
    public List<TaskItemDefine> reloadDealTaskItem(String taskType, String uuid) throws Exception {
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_TaskItem;

        List<String> taskItems = this.zkClient.getChildren().forPath(zkPath);
        taskItems.sort((o1, o2) -> {
            if (isNumeric(o1) && isNumeric(o2)) {
                return new Integer(Integer.parseInt(o1)).compareTo(Integer.parseInt(o2));
            } else {
                return o1.compareTo(o2);
            }
        });

        logger.info("{} current uid = {} , zk reloadDealTaskItem", taskType, uuid);
        List<TaskItemDefine> result = Lists.newArrayList();
        for (String name : taskItems) {
            byte[] serverValue = this.zkClient.getData().forPath(zkPath + "/" + name + "/cur_server");
            if (serverValue != null && uuid.equals(new String(serverValue))) {
                TaskItemDefine item = new TaskItemDefine();
                item.setTaskItemId(name);
                byte[] parameterValue = this.zkClient.getData().forPath(zkPath + "/" + name + "/parameter");
                if (parameterValue != null) {
                    item.setParameter(new String(parameterValue));
                }
                result.add(item);
            } else if (serverValue != null && !uuid.equals(new String(serverValue))) {
                logger.trace(" current uid={}, zk cur_server uid= {}", uuid, new String(serverValue));
            } else {
                logger.trace(" current uid={}", uuid);
            }
        }
        return result;
    }

    /**
     * 获取所有的 taskItem
     * @param taskType
     * @return
     * @throws Exception
     */
    @Override
    public List<ScheduleTaskItem> loadAllTaskItem(String taskType) throws Exception {
        List<ScheduleTaskItem> result = Lists.newArrayList();
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_TaskItem;
        if (this.zkClient.checkExists().forPath(zkPath) == null) {
            return result;
        }
        List<String> taskItems = this.zkClient.getChildren().forPath(zkPath);
        taskItems.sort((o1, o2) -> {
            if (StringUtils.isNumeric(o1) && StringUtils.isNumeric(o2)) {
                return new Integer(o1).compareTo(Integer.parseInt(o2));
            }
            return o1.compareTo(o2);
        });
        for (String taskItem : taskItems) {
            ScheduleTaskItem info = new ScheduleTaskItem();
            info.setTaskType(taskType);
            info.setTaskItem(taskItem);
            String zkItemPath = zkPath + "/" + taskItem;
            byte[] curContent = this.zkClient.getData().forPath(zkItemPath + "/cur_server");
            if (curContent != null) {
                info.setCurrentScheduleServer(new String(curContent));
            }
            byte[] reqContent = this.zkClient.getData().forPath(zkItemPath + "/req_server");
            if (reqContent != null) {
                info.setRequestScheduleServer(new String(reqContent));
            }
            byte[] stsContent = this.zkClient.getData().forPath(zkItemPath + "/sts");
            if (stsContent != null) {
                info.setSts(ScheduleTaskItem.TaskItemSts.valueOf(new String(stsContent)));
            }
            byte[] parameterContent = this.zkClient.getData().forPath(zkItemPath + "/parameter");
            if (parameterContent != null) {
                info.setDealParameter(new String(parameterContent));
            }
            byte[] dealDescContent = this.zkClient.getData().forPath(zkItemPath + "/deal_desc");
            if (dealDescContent != null) {
                info.setDealDesc(new String(dealDescContent));
            }
            result.add(info);
        }
        return result;
    }

    /**
     * 刷新 scheduleServer 数据
     * @param currentScheduleServer
     * @return
     * @throws Exception
     */
    @Override
    public boolean refreshScheduleServer(ScheduleServer currentScheduleServer) throws Exception {
        Timestamp heartBeatTime = new Timestamp(this.getSystemTime());
        String zkPath = this.PATH_BaseTaskType + "/" + currentScheduleServer.getBaseTaskType() +
                "/" + currentScheduleServer.getTaskType() + "/" + this.PATH_Server + "/" + currentScheduleServer.getUuid();
        if (this.zkClient.checkExists().forPath(zkPath) == null) {
            currentScheduleServer.setRegister(false);
            return false;
        } else {
            Timestamp oldHeartBeatTime = currentScheduleServer.getHeartBeatTime();
            currentScheduleServer.setHeartBeatTime(heartBeatTime);
            currentScheduleServer.setVersion(currentScheduleServer.getVersion() + 1);
            String valueString = this.gson.toJson(currentScheduleServer);
            try {
                this.zkClient.setData().forPath(zkPath, valueString.getBytes());
            } catch (Exception e) {
                currentScheduleServer.setHeartBeatTime(oldHeartBeatTime);
                currentScheduleServer.setVersion(currentScheduleServer.getVersion() - 1);
                throw e;
            }
            return true;
        }
    }

    @Override
    public void unRegisterScheduleServer(String taskType, String uuid) throws Exception {
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_Server + "/" + uuid;
        if (this.zkClient.checkExists().forPath(zkPath) != null) {
            this.zkClient.delete().deletingChildrenIfNeeded().forPath(zkPath);
        }
    }

    /**
     * 当前任务项的 cur_server 如果不在 server 列表中，则将 cur_server 清空
     *
     * @param taskType
     * @param serverList
     * @throws Exception
     */
    @Override
    public int clearTaskItem(String taskType, List<String> serverList) throws Exception {
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_TaskItem;
        int result = 0;
        for (String taskItemName : this.zkClient.getChildren().forPath(zkPath)) {
            String currServerZkPath = zkPath + "/" + taskItemName + "/cur_server";
            byte[] curServerValue = this.zkClient.getData().forPath(currServerZkPath);
            if (curServerValue.length > 0) {
                String curServer = new String(curServerValue);
                if (!serverList.contains(curServer)) {
                    this.zkClient.setData().forPath(currServerZkPath, null);
                    result++;
                }
            } else {
                result++;
            }
        }
        return result;
    }

    /**
     * 分配任务项
     *
     * @param taskType                     任务类型
     * @param uuid                         server id
     * @param maxTaskItemsOfOneThreadGroup
     * @param serverList
     * @throws Exception
     */
    @Override
    public void assignTaskItem(String taskType, String uuid, int maxTaskItemsOfOneThreadGroup, List<String> serverList) throws Exception {
        if (!this.isLeader(uuid, serverList)) {
            logger.info("{}：不是任务分配的 Leader，直接返回");
            return;
        }
        logger.info("{}：开始重新分配任务......");
        if (serverList.size() <= 0) {
            return;
        }
        String baseTaskType = ScheduleUtils.getBaseTaskTypeFromTaskType(taskType);
        String zkPath = this.PATH_BaseTaskType + "/" + baseTaskType + "/" + taskType + "/" + this.PATH_TaskItem;
        // 任务项列表
        List<String> taskItemNames = this.zkClient.getChildren().forPath(zkPath);
        taskItemNames.sort((o1, o2) -> {
            if (StringUtils.isNumeric(o1) && StringUtils.isNumeric(o2)) {
                return new Integer(o1).compareTo(new Integer(o2));
            }
            return o1.compareTo(o2);
        });

        // assignTaskNumber 每个server 获取几个任务，1对多
        // 下面是 每个任务被哪个 server 处理，多对1
        int unModifyCount = 0;
        int[] taskNums = ScheduleUtils.assignTaskNumber(serverList.size(), taskItemNames.size(), maxTaskItemsOfOneThreadGroup);
        String NO_SERVER_DEAL = "没有分配到服务器";
        int point = 0;  // 对应 taskNums 的 index
        int count = 0;
        for (int i = 0; i < taskItemNames.size(); i++) {
            String taskItemName = taskItemNames.get(i);
            if (point < serverList.size() && i >= count + taskNums[point]) {
                count = count + taskNums[point];
                point += 1;
            }
            String serverName = NO_SERVER_DEAL;
            if (point < serverList.size()) {
                serverName = serverList.get(point);
            }

            String curServerZkPath = zkPath + "/" + taskItemName + "/cur_server";
            String reqServerZkPath = zkPath + "/" + taskItemName + "/req_server";
            byte[] curServerValue = this.zkClient.getData().forPath(curServerZkPath);
            byte[] reqServerValue = this.zkClient.getData().forPath(reqServerZkPath);
            // 如果未分配 server，则开始分配
            if (curServerValue == null || curServerValue.length == 0 || new String(curServerValue).equals(NO_SERVER_DEAL)) {
                this.zkClient.setData().forPath(curServerZkPath, serverName.getBytes());
                this.zkClient.setData().forPath(reqServerZkPath, null);
            } else if (new String(curServerValue).equals(serverName) && (reqServerValue == null || reqServerValue.length == 0)) {
                // 当前已分配 server，且与要分配的 server 一致
                unModifyCount += 1;
            } else {
                // 已分配的 server 和要分配的 server 不一致
                this.zkClient.setData().forPath(reqServerZkPath, serverName.getBytes());
            }
        }

        if (unModifyCount < taskItemNames.size()) {
            logger.info("设置所有服务器重新装载任务：updateReloadTaskItemFlag......{}，currentUUid {}", taskType, uuid);
            this.updateReloadTaskItemFlag(taskType);
        }
    }


    public static class TimestampTypeAdapter implements JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {
        @Override
        public Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!(json instanceof JsonPrimitive)) {
                throw new JsonParseException("The date should be a string value");
            }

            try {
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = format.parse(json.getAsString());
                return new Timestamp(date.getTime());
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(Timestamp src, Type typeOfSrc, JsonSerializationContext context) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateFormatAsString = format.format(new Date(src.getTime()));
            return new JsonPrimitive(dateFormatAsString);
        }
    }
}
