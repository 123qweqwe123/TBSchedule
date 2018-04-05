package me.hrps.schedule.config.annotation;

import me.hrps.schedule.taskmanager.ScheduledMethodRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:
 * <pre>
 *     处理 @TBScheduled 标记的方法，将该方法注册为任务实体
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/12 下午8:56
 */
public class TBScheduledAnnotationBeanPostProcessor
        implements MergedBeanDefinitionPostProcessor, DestructionAwareBeanPostProcessor,
        Ordered, EmbeddedValueResolverAware, BeanNameAware, BeanFactoryAware, ApplicationContextAware,
        SmartInitializingSingleton, ApplicationListener<ContextRefreshedEvent>, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(TBScheduledAnnotationBeanPostProcessor.class);

    private String beanName;
    private BeanFactory beanFactory;
    private ApplicationContext applicationContext;
    /**
     * 根据类是否包含注解来过滤 bean
     */
    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

    /**
     * bean 和 runnable 对应关系
     */
    private final Set<ScheduledMethodRunnable> scheduledTasks = new LinkedHashSet<>(4);

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setBeanName(String s) {
        this.beanName = s;
    }

    @Override
    public void destroy() throws Exception {
        synchronized (this.scheduledTasks) {
            this.scheduledTasks.clear();
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.nonAnnotatedClasses.clear();

        if (this.applicationContext == null) {
            // Not running in an ApplicationContext -> register tasks early...
            finishRegistration();
        }
    }

    @Override
    public void postProcessBeforeDestruction(Object o, String s) throws BeansException {

    }

    @Override
    public boolean requiresDestruction(Object o) {
        return false;
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition rootBeanDefinition, Class<?> aClass, String s) {

    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * 查找所有包含 TBScheduled 注解的方法，创建任务
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        if (!this.nonAnnotatedClasses.contains(targetClass)) {
            // 获取 bean 内所有包含 @TBScheduled 注解的方法
            Map<Method, TBScheduled> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                    (MethodIntrospector.MetadataLookup<TBScheduled>) method ->
                            AnnotatedElementUtils.getMergedAnnotation(method, TBScheduled.class));
            if (annotatedMethods.isEmpty()) {
                this.nonAnnotatedClasses.add(targetClass);
                if (logger.isTraceEnabled()) {
                    logger.trace("No @Scheduled annotations found on bean class: " + bean.getClass());
                }
            } else {
                for (Map.Entry<Method, TBScheduled> entry : annotatedMethods.entrySet()) {
                    Method method = entry.getKey();
                    TBScheduled scheduled = entry.getValue();
                    processScheduled(scheduled, method, bean, beanName);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug(annotatedMethods.size() + " @Scheduled methods processed on bean '" + beanName +
                            "': " + annotatedMethods);
                }
            }
        }
        return bean;
    }

    /**
     *
     *
     * @param scheduled
     * @param method
     * @param bean
     * @param beanName
     */
    protected void processScheduled(TBScheduled scheduled, Method method, Object bean, String beanName) {
        try {
            Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
            ScheduledMethodRunnable task = new ScheduledMethodRunnable(bean, invocableMethod, scheduled.cron());
            task.setTaskName(beanName + "$" + method.getName());
            // 初始化任务参数
            task.parseArgStrToArgs();
            synchronized (this.scheduledTasks) {
                scheduledTasks.add(task);
            }
        } catch (IllegalArgumentException e) {

        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if (this.beanFactory == null) {
            this.beanFactory = applicationContext;
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() == this.applicationContext) {
            finishRegistration();
        }
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {

    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    private void finishRegistration() {

    }

    public Set<ScheduledMethodRunnable> getScheduledTasks() {
        return scheduledTasks;
    }
}
