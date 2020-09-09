package com.atex.plugins.mailimporter;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;


@Configuration
public class QuartzConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzConfig.class);

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        LOGGER.debug("QuartzConfig initialized.");
    }

    @Bean
    public SchedulerFactoryBean quartzScheduler() {
        SchedulerFactoryBean quartzScheduler = new SchedulerFactoryBean();

        quartzScheduler.setOverwriteExistingJobs(true);
        quartzScheduler.setSchedulerName("mailimporter-quartz-scheduler");

        // custom job factory of spring with DI support for @Autowired!
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        quartzScheduler.setJobFactory(jobFactory);

        quartzScheduler.setQuartzProperties(quartzProperties());

        Trigger[] triggers = { processMQTrigger().getObject() };
        quartzScheduler.setTriggers(triggers);

        return quartzScheduler;
    }

    @Bean
    public JobDetailFactoryBean processMQJob() {
        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(MailImporterEnabledChecker.class);
        jobDetailFactory.setGroup("spring3-quartz");
        return jobDetailFactory;
    }

    @Bean
    public CronTriggerFactoryBean processMQTrigger() {
        CronTriggerFactoryBean cronTriggerFactoryBean = new CronTriggerFactoryBean();
        cronTriggerFactoryBean.setJobDetail(processMQJob().getObject());
        cronTriggerFactoryBean.setCronExpression("0 0/2 * ? * * *");
        cronTriggerFactoryBean.setGroup("spring3-quartz");
        return cronTriggerFactoryBean;
    }

    @Bean
    public Properties quartzProperties() {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/quartz.properties"));
        Properties properties = null;
        try {
            propertiesFactoryBean.afterPropertiesSet();
            properties = propertiesFactoryBean.getObject();

        } catch (IOException e) {
            LOGGER.warn("Cannot load quartz.properties: " + e.getMessage());
        }

        return properties;
    }
}