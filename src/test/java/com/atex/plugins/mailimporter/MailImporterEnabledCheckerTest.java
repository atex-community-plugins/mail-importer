package com.atex.plugins.mailimporter;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailImporterEnabledCheckerTest {

    private static final Logger logger = LoggerFactory.getLogger(MailImporterEnabledCheckerTest.class.getName());

    @Ignore
    @Test
    public void testExec() throws Exception {
        logger.info("Configuring dummy route");

        CamelContext context = new DefaultCamelContext();
        try {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    logger.info("Configuring dummy route2");
                    log.error("blah");
                    context.setUseMDCLogging(false);
                    // you can create a new one with https://ethereal.email/create
                    from("pop3s://pop.gmail.com:995?username=xxxx&password=xxxx&delete=true&debugMode=true")
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
    public void testHidePwd() {
        final MailImporterEnabledChecker checker = new MailImporterEnabledChecker();
        Assert.assertEquals(
                "pop3s://pop.gmail.com:995?username=abc&password=xxx",
                checker.hidePwd("pop3s://pop.gmail.com:995?username=abc&password=12345678"));
        Assert.assertEquals(
                "pop3s://pop.gmail.com:995?password=xxx&username=abc",
                checker.hidePwd("pop3s://pop.gmail.com:995?password=1234567&username=abc"));
        Assert.assertEquals(
                "pop3s://pop.gmail.com:995?username=abc",
                checker.hidePwd("pop3s://pop.gmail.com:995?username=abc"));
    }

    @Test
    public void testRouteId() {
        final MailImporterEnabledChecker c = new MailImporterEnabledChecker();
        Assert.assertEquals("mailImporterRoute00", c.createRouteId(0));
        Assert.assertEquals("mailImporterRoute01", c.createRouteId(1));
        Assert.assertEquals("mailImporterRoute90", c.createRouteId(90));
        Assert.assertEquals("mailImporterRoute100", c.createRouteId(100));
    }

}
