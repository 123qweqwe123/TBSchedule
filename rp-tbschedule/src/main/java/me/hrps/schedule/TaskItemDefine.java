package me.hrps.schedule;

/**
 * Description:
 * <pre>
 *     任务定义，提供关键信息给使用者
 * </pre>
 * Author: huangrupeng
 * Create: 17/7/12 上午8:29
 */
public class TaskItemDefine {

    /**
     * taskItem 的 名称
     */
    private String taskItemId;

    private String parameter;

    public String getTaskItemId() {
        return taskItemId;
    }

    public void setTaskItemId(String taskItemId) {
        this.taskItemId = taskItemId;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        return "(t=" + taskItemId + ", p='" + parameter + ")";
    }

    public static void main(String[] args) {
        System.out.println(11);
    }
}
