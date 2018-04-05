package me.hrps.schedule;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/16 下午9:52
 */
public class ScheduleUtils {

    public static String OWN_SIGN_BASE = "BASE";

    public static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "";
        }
    }

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 将任务平均分配到每个 worker
     *
     * @param serverNum
     * @param taskItemNum
     * @param maxNumOfOneServer
     * @return
     */
    public static int[] assignTaskNumber(int serverNum, int taskItemNum, int maxNumOfOneServer) {
        int[] taskNums = new int[serverNum];
        int numOfSingle = taskItemNum / serverNum;
        int otherNum = taskItemNum % serverNum;
        for (int i = 0; i < taskNums.length; i++) {
            if (i < otherNum) {
                taskNums[i] = numOfSingle + 1;
            } else {
                taskNums[i] = numOfSingle;
            }
        }
        return taskNums;
    }

    public static String getBaseTaskTypeFromTaskType(String taskType) {
        if (taskType.contains("$")) {
            return StringUtils.substringBefore(taskType, "$");
        }
        return taskType;
    }

    public static String getOwnSignFromTaskType(String taskType) {
        if (taskType.contains("$")) {
            return StringUtils.substringAfterLast(taskType, "$");
        }
        return OWN_SIGN_BASE;
    }

    public static String getTaskTypeFromBaseAndOwnSign(String baseType, String ownSign) {
        if (ownSign.equals(OWN_SIGN_BASE)) {
            return baseType;
        }
        return baseType + "$" + ownSign;
    }

    public static String formatDateTime(Date d) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(d);
    }

    public static Date parseDateTimeStr(String dateTimeStr) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.parse(dateTimeStr);
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

}
