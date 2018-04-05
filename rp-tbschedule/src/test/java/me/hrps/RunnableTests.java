package me.hrps;

import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/17 下午9:30
 */
public class RunnableTests {

    private void talk(String name, String id) {
        System.out.println(name + " " + id);
    }

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException {
        Method method = ReflectionUtils.findMethod(RunnableTests.class, "talk", null);
        Class<?>[] types = method.getParameterTypes();
        for (Class t : types) {
            System.out.println(t.getName());
            System.out.println(BeanUtils.isSimpleValueType(t));
            ;
        }

        //        ReflectionUtils.makeAccessible(method);
        RunnableTests t = new RunnableTests();
        //ReflectionUtils.invokeMethod(method, t, "aa", "bb");
        Object[] parameters = { null,null };
        method.invoke(t, parameters);
        String str = "sfd  22    2 3  55";
        System.out.println(str.split("\\s+")[4]);
    }


}
