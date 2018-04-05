package me.hrps.schedule.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/9 上午10:04
 */
@ConfigurationProperties(prefix = "rp.tbschedule")
public class TBScheduleProperties {

    private String zkConnectString = "localhost:2181";
    private String zkRootPath;
    private Integer zkSessionTimeout = 60 * 1000;
    private String zkUserName;
    private String zkPassword;
    private Boolean zkIsCheckParentPath = false;


    public String getZkConnectString() {
        return zkConnectString;
    }

    public void setZkConnectString(String zkConnectString) {
        this.zkConnectString = zkConnectString;
    }

    public String getZkRootPath() {
        return zkRootPath;
    }

    public void setZkRootPath(String zkRootPath) {
        this.zkRootPath = zkRootPath;
    }

    public Integer getZkSessionTimeout() {
        return zkSessionTimeout;
    }

    public void setZkSessionTimeout(Integer zkSessionTimeout) {
        this.zkSessionTimeout = zkSessionTimeout;
    }

    public String getZkUserName() {
        return zkUserName;
    }

    public void setZkUserName(String zkUserName) {
        this.zkUserName = zkUserName;
    }

    public String getZkPassword() {
        return zkPassword;
    }

    public void setZkPassword(String zkPassword) {
        this.zkPassword = zkPassword;
    }

    public Boolean getZkIsCheckParentPath() {
        return zkIsCheckParentPath;
    }

    public void setZkIsCheckParentPath(Boolean zkIsCheckParentPath) {
        this.zkIsCheckParentPath = zkIsCheckParentPath;
    }
}
