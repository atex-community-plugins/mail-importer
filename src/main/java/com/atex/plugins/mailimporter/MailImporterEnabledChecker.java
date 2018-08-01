package com.atex.plugins.mailimporter;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MailImporterEnabledChecker implements Job {

    public static final String MAIL_IMPORTER_ROUTE = "mailImporterRoute";

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MailProcessor mailProcessor;

    @Autowired
    protected CamelContext camelContext;

    public MailImporterEnabledChecker() {}

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        MailImporterConfig config = mailProcessor.getConfig();
        Route existingRoute = camelContext.getRoute(MAIL_IMPORTER_ROUTE);
        if (config != null && config.isEnabled()) {
            try {
                if (existingRoute == null) {
                    RouteBuilder routeBuilder = new RouteBuilder() {
                        @Override
                        public void configure() throws Exception {
                            from(config.getMailUri()).autoStartup(false)
                                    .process(mailProcessor).setId(MAIL_IMPORTER_ROUTE);
                        }
                    };
                    camelContext.start();
                    camelContext.addRoutes(routeBuilder);
                    camelContext.startRoute(MAIL_IMPORTER_ROUTE);
                }
            } catch (Exception e) {
                log.error("failed to start mail importer route",e);
            }
        } else {
            try {
                if (existingRoute != null) {
                    camelContext.stopRoute(MAIL_IMPORTER_ROUTE);
                    camelContext.removeRoute(MAIL_IMPORTER_ROUTE);
                }
            } catch (Exception e) {
                log.error("failed to stop mail importer route",e);
            }
        }

    }
}
