package me.hrps;

import me.hrps.domain.CallableObject;
import org.junit.Test;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/13 下午3:57
 */
public class MethodTest {

    @Test
    public void test() {
        CallableObject<Void> obj = new CallableObject<>(null);
        CallableObject num;
        num = new CallableObject<>(11);
        num.print("111");
    }
}
