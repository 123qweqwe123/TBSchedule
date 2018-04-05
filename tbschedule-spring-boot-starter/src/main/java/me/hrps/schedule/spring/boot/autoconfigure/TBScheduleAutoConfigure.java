package me.hrps.schedule.spring.boot.autoconfigure;

import me.hrps.schedule.config.TBScheduleConfig;
import me.hrps.schedule.config.annotation.ScheduleScannerRegistrarConfiguration;
import me.hrps.schedule.config.annotation.TBScheduledAnnotationBeanPostProcessor;
import me.hrps.schedule.strategy.TBScheduleManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: huangrupeng
 * Create: 17/8/9 上午9:55
 */
@Configuration
//@ConditionalOnClass(me.hrps.schedule.strategy.TBScheduleManagerFactory.class)
@EnableConfigurationProperties(TBScheduleProperties.class)
@EnableScheduling
@Import({ ScheduleScannerRegistrarConfiguration.class })
public class TBScheduleAutoConfigure {

    @Autowired
    private TBScheduleProperties tbScheduleProperties;

    @Bean
    @ConditionalOnMissingBean
    public TBScheduleConfig tbscheduleConfig() {
        TBScheduleConfig TBScheduleConfig = new TBScheduleConfig();
        TBScheduleConfig.put(zkConfig.zkConnectString.toString(), tbScheduleProperties.getZkConnectString());
        TBScheduleConfig.put(zkConfig.zkRootPath.toString(), tbScheduleProperties.getZkRootPath());
        TBScheduleConfig.put(zkConfig.zkSessionTimeout.toString(), tbScheduleProperties.getZkSessionTimeout());
        TBScheduleConfig.put(zkConfig.zkUserName.toString(), tbScheduleProperties.getZkUserName());
        TBScheduleConfig.put(zkConfig.zkPassword.toString(), tbScheduleProperties.getZkPassword());
        TBScheduleConfig.put(zkConfig.zkIsCheckParentPath.toString(), tbScheduleProperties.getZkIsCheckParentPath());
        return TBScheduleConfig;
    }

    @Bean
    @ConditionalOnMissingBean
    public TBScheduleManagerFactory tbScheduleManagerFactory(TBScheduleConfig tbscheduleConfig) {
        return new TBScheduleManagerFactory(tbscheduleConfig);
    }

    private enum zkConfig {
        zkConnectString, zkRootPath, zkSessionTimeout, zkUserName, zkPassword, zkIsCheckParentPath;
    }

    @Bean
    @ConditionalOnMissingBean
    public TBScheduledAnnotationBeanPostProcessor tbScheduledAnnotationBeanPostProcessor() {
        return new TBScheduledAnnotationBeanPostProcessor();
    }
}
