package com.atex.plugins.mailimporter;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;

public class MailImporterEnabledCheckerTest {

    private static final String LOG_NAME = MailImporterEnabledCheckerTest.class.getName();
    private static final Logger logger = LogManager.getLogger(MailImporterEnabledCheckerTest.class.getName());
    @Test
    public void testExec() throws Exception {
        BasicConfigurator.configure();
        logger.setLevel(Level.ALL);
        logger.info("Configuring dummy route");

        CamelContext context = new DefaultCamelContext();
        try {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    logger.log(Level.INFO, "Configuring dummy route2");
                    log.error("blah");
                    context.setUseMDCLogging(false);
//                    from("direct:start")
//                      .log(LoggingLevel.INFO, LOG_NAME, "log:starting")
//                      .to("pop3://pop.gmail.com:995?username=demoatex0@gmail.com&password=DemoAtex000!!!debugMode=true")
                    from("pop3s://pop.gmail.com:995?username=demoatex0@gmail.com&password=DemoAtex000!!!&delete=true&debugMode=true")
                      .to("log:INFO")
                      .to("log:DEBUG")
                      .log("logger:somethings wrong")
                      .to("mock:finally")
                      .end();
                }
            });
            context.start();
            Thread.sleep(10000);
        } finally {
            context.stop();
        }
    }

    @Test
    public void name() {

    }
}
