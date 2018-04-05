package me.hrps.schedule.zk;

import com.google.common.collect.Lists;
import me.hrps.schedule.config.TBScheduleConfig;
import me.hrps.schedule.strategy.TBScheduleManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * <pre>
 *     zookeeper 管理器
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/8 下午5:59
 */
public class ZKManager {

    private Logger logger = LoggerFactory.getLogger(ZKManager.class);

    private TBScheduleConfig config;
    private CuratorFramework client;
    private List<ACL> aclList = Lists.newArrayList();
    private boolean isCheckParentPath = true;
    private final TBScheduleManagerFactory factory;


    private enum keys {
        zkConnectString, zkRootPath, zkSessionTimeout, zkUserName, zkPassword, zkIsCheckParentPath
    }

    public ZKManager(TBScheduleConfig config, TBScheduleManagerFactory factory) throws Exception {
        this.config = config;
        this.factory = factory;
        this.connect();
    }

    private void connect() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        createZookeeper(connectionLatch);
        connectionLatch.await(10, TimeUnit.SECONDS);
    }

    public void reConnect() throws Exception {
        if (this.client != null) {
            this.client.close();
            this.client = null;
            this.connect();
        }
    }

    private void createZookeeper(CountDownLatch connectedLatch) throws Exception {
        String authString = config.get(keys.zkUserName.toString()) + ":"
                + config.get(keys.zkPassword.toString());
        if (config.get(keys.zkUserName.toString()) == null) {
            authString = null;
        }
        String namespace = getRootPath().substring(1);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString((String) config.get(keys.zkConnectString.toString()))
                .namespace(namespace)
                .sessionTimeoutMs((Integer) config.get(keys.zkSessionTimeout.toString()))
                .retryPolicy(new ExponentialBackoffRetry(1000, 2))
                .defaultData(null);  // forPath() 的时候不设置默认值
        if (authString != null) {
            builder.authorization("digest", authString.getBytes());
        }
        client = builder.build();
        client.getConnectionStateListenable().addListener((client, newState) -> {
            if (newState.isConnected()) {
                logger.info("收到 ZK 连接成功事件！");
                try {
                    factory.initialData(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                connectedLatch.countDown();
            } else {
                logger.info("连接失败。");
                factory.stopServer();
                connectedLatch.countDown();
            }
        });
        client.start();
        aclList.clear();
        if (authString != null) {
            aclList.add(new ACL(ZooDefs.Perms.ALL, new Id("digest",
                    DigestAuthenticationProvider.generateDigest(authString))));
            aclList.add(new ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE));
        } else {
            aclList.add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE));
        }
        this.isCheckParentPath = (Boolean) config.get(keys.zkIsCheckParentPath.toString());
    }

    /**
     * 关闭连接
     */
    public void close() {
        logger.info("关闭 ZK 连接。");
        if (this.client.getState() == CuratorFrameworkState.STARTED) {
            this.client.close();
        }
    }

    /**
     * @return 检查 zookeeper 是否已连接
     */
    public boolean isZkConnected() {
        return this.client.getZookeeperClient().isConnected();
    }

    public String getConnectStr() {
        return (String) this.config.get(keys.zkConnectString.toString());
    }

    public String getRootPath() {
        return (String) this.config.get(keys.zkRootPath.toString());
    }


    /**
     * 检查并设置程序版本
     *
     * @throws Exception
     */
    public void initial() throws Exception {
        if (isCheckParentPath) {
            checkParent(client);
        }
        CuratorFramework clientWithNoNamespace = client.usingNamespace(null);
        if (clientWithNoNamespace.checkExists().forPath(getRootPath()) == null) {
            clientWithNoNamespace.create().creatingParentContainersIfNeeded().withMode(CreateMode.PERSISTENT).withACL(aclList).forPath(getRootPath());
            clientWithNoNamespace.setData().forPath(getRootPath(), Version.getVersion().getBytes());
        } else {
            byte[] value = clientWithNoNamespace.getData().forPath(getRootPath());
            if (value == null) {
                clientWithNoNamespace.setData().forPath(getRootPath(), Version.getVersion().getBytes());
            } else {
                String dataVersion = new String(value);
                if (!Version.isCompatible(dataVersion)) {
                    throw new Exception("TBSchedule程序版本 " + Version.getVersion() + " 不兼容Zookeeper中的数据版本 " + dataVersion);
                }
                logger.info("当前程序版本：" + Version.getVersion() + "数据版本：" + dataVersion);
            }
        }
    }

    private static void checkParent(CuratorFramework client) throws Exception {
        CuratorFramework noNsClient = client.usingNamespace(null);
        String ns = client.getNamespace();
        String[] list = ns.split("/");
        String zkPath = "";
        for (int i = 0; i < list.length - 1; i++) {
            String str = list[i];
            if (StringUtils.isNotBlank(str)) {
                zkPath = zkPath + "/" + str;
                if (noNsClient.checkExists().forPath(zkPath) != null) {
                    byte[] value = noNsClient.getData().forPath(zkPath);
                    if (value != null) {
                        String tmpVersion = new String(value);
                        if (tmpVersion.contains("taobao-pamirs-schedule-")) {
                            throw new Exception("\"" + zkPath + "\"已被定义为 rootPath，子目录不能再次定义为 rootPath");
                        }
                    }
                }
            }
        }
    }

    public CuratorFramework getClient() throws Exception {
        if (!isZkConnected()) {
            reConnect();
        }
        return this.client;
    }

    public List<ACL> getAclList() {
        return aclList;
    }
}
