package me.hrps.schedule.zk;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/10 上午10:23
 */
public class Version {

    public final static String version = "tbschedule-3.2.12";

    public static String getVersion() {
        return version;
    }

    public static boolean isCompatible(String dataVersion) {
        if (version.compareTo(dataVersion) >= 0) {
            return true;
        } else {
            return false;
        }
    }
}
