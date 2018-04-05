package me.hrps.domain;

import java.util.concurrent.Callable;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/13 下午3:58
 */
public class CallableObject<V> implements Callable<V>{

    @Override
    public V call() throws Exception {
        return null;
    }

    private V v;

    public CallableObject(V v) {
        this.v = v;
    }

    public void print(V v) {
        System.out.println(v);
    }
}
