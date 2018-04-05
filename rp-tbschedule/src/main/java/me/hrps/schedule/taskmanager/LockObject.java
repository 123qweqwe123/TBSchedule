package me.hrps.schedule.taskmanager;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/7 上午9:21
 */
public class LockObject {

    private int m_threadCount = 0;
    private Object m_wartOnObject = new Object();

    public LockObject() {
    }

    public void waitCurrentThread() throws Exception {
        synchronized (m_wartOnObject) {
            this.m_wartOnObject.wait();
        }
    }

    public void notifyOtherThread() throws Exception {
        synchronized (m_wartOnObject) {
            this.m_wartOnObject.notifyAll();
        }
    }

    public void addThread() {
        synchronized (this) {
            m_threadCount = m_threadCount + 1;
        }
    }

    public void releaseThread() {
        synchronized (this) {
            m_threadCount = m_threadCount - 1;
        }
    }

    public boolean releaseThreadButNotLast() {
        synchronized (this) {
            if (m_threadCount == 1) {
                return false;
            } else {
                m_threadCount = m_threadCount - 1;
                return true;
            }
        }
    }

    public int count() {
        synchronized (this) {
            return m_threadCount;
        }
    }
}
