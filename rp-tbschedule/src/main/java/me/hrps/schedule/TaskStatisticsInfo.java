package me.hrps.schedule;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/13 下午3:38
 */
public class TaskStatisticsInfo {

    private long fetchDataCount;    // 总数据量
    private long dealDataSuccess;   // 成功数据量
    private long dealDataFail;  // 失败数据量

    public long getFetchDataCount() {
        return fetchDataCount;
    }

    public void setFetchDataCount(long fetchDataCount) {
        this.fetchDataCount = fetchDataCount;
    }

    public long getDealDataSuccess() {
        return dealDataSuccess;
    }

    public void setDealDataSuccess(long dealDataSuccess) {
        this.dealDataSuccess = dealDataSuccess;
    }

    public long getDealDataFail() {
        return dealDataFail;
    }

    public void setDealDataFail(long dealDataFail) {
        this.dealDataFail = dealDataFail;
    }
}
