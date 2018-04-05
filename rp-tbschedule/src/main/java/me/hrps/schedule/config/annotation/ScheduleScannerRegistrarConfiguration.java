package me.hrps.schedule.config.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Description:
 * <pre>
 *     注册 @{@link TBScheduleComponent} 标记的类为 spring bean
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/12 下午8:36
 */
public class ScheduleScannerRegistrarConfiguration implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    Logger logger = LoggerFactory.getLogger(ScheduleScannerRegistrarConfiguration.class);

    private BeanFactory beanFactory;

    private ResourceLoader resourceLoader;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        logger.debug("Searching for mappers annotated with @TBScheduleComponent");
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
        TypeFilter filter = new AnnotationTypeFilter(TBScheduleComponent.class);
        scanner.addIncludeFilter(filter);
        try {
            if (this.resourceLoader != null) {
                scanner.setResourceLoader(this.resourceLoader);
            }
            List<String> pkgs = AutoConfigurationPackages.get(this.beanFactory);
            for (String pkg : pkgs) {
                logger.debug("Using auto-configuration base package '" + pkg + "'");
            }
            int count = scanner.scan(StringUtils.toStringArray(pkgs));
            logger.debug("{} @TBScheduleComponent beans has registered", count);
        } catch (IllegalStateException ex) {
            logger.debug("Could not determine auto-configuration " + "package, automatic mapper scanning disabled.");
        }
    }
}
