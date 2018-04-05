package me.hrps;

import com.google.common.collect.Lists;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.junit.Test;

import java.util.List;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/13 下午9:46
 */
public class CuratorTests {

    @Test
    public void testConnect() throws Exception {
        String namespace = "rp";
        String authString = "ScheduleAdmin:password";
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .namespace(namespace)
                .sessionTimeoutMs(6000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .authorization("digest", authString.getBytes())
                .defaultData(null)  // forPath() 的时候不设置默认值
                .build();
        client.getConnectionStateListenable().addListener((c, newState) -> {
            if (newState.isConnected()) {
                System.out.println("收到 ZK 连接成功事件！");
            } else {
                System.out.println("connected");
            }
        });
        client.start();
        List<ACL> aclList = Lists.newArrayList();
        aclList.add(new ACL(ZooDefs.Perms.ALL, new Id("digest",
                DigestAuthenticationProvider.generateDigest(authString))));
        aclList.add(new ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE));
        String str = "{\"cron\":\"0/20 * * * * ? \",\"taskName\":\"sampleTask$sayHello\",\"running\":true,\"startTime\":1503034350003,\"endTime\":1503034350003}";
        client.setData().forPath("/scheduledTask/127.0.0.1$rp$FB14BE7A891C42CB9D8B0C9E3F64EBCF$0000000009$sampleTask$sayHello", str.getBytes());
    }


    @Test
    public void testAcl() {

    }
}
