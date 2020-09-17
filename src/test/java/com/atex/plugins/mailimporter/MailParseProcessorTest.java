package com.atex.plugins.mailimporter;

import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Assert;
import org.junit.Test;

/**
 * MailParseProcessorTest
 *
 * @author mnova
 */
public class MailParseProcessorTest extends AbstractProcessorTest {

    private final MailParseProcessor mailProcessor = new MailParseProcessor();

    @Test
    public void testTextMailParse() throws Exception {
        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setFrom(new InternetAddress("mnova@example.com", "Marco Nova <mnova@example.com>"));
        msg.setSubject("Test Article");
        msg.setText("This is the simple text");
        msg.setRecipient(RecipientType.TO, new InternetAddress("mock.endpoint@example.com", "Mock Endpoint <mock.endpoint@example.com>"));
        final MailMessage mail = new MailMessage(msg);

        final MockEndpoint mock = setupMockEndpoint(1, MailBean.class);

        final Exchange exchange = ExchangeBuilder.anExchange(template.getCamelContext())
                                                 .build();
        exchange.setIn(mail);
        template.send("direct:start", exchange);

        assertMockEndpointsSatisfied();

        final MailBean outMail = mock.getReceivedExchanges()
                                     .get(0)
                                     .getIn()
                                     .getBody(MailBean.class);
        Assert.assertNotNull(outMail);
        Assert.assertEquals("mnova@example.com", outMail.getFrom());
        Assert.assertEquals("Test Article", outMail.getSubject());
        Assert.assertEquals("<p>This is the simple text</p>", outMail.getBody());
        Assert.assertEquals("", outMail.getLead());
    }

    @Test
    public void test_mail_with_lead() throws Exception {
        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setFrom(new InternetAddress("mnova@example.com", "Marco Nova <mnova@example.com>"));
        msg.setSubject("Test Article");
        msg.setText("This is the lead\n\nThis is the body");
        msg.setRecipient(RecipientType.TO, new InternetAddress("mock.endpoint@example.com", "Mock Endpoint <mock.endpoint@example.com>"));
        final MailMessage mail = new MailMessage(msg);

        final MockEndpoint mock = setupMockEndpoint(1, MailBean.class);

        final Exchange exchange = ExchangeBuilder.anExchange(template.getCamelContext())
                                                 .build();
        exchange.setIn(mail);
        template.send("direct:start", exchange);

        assertMockEndpointsSatisfied();

        final MailBean outMail = mock.getReceivedExchanges()
                                     .get(0)
                                     .getIn()
                                     .getBody(MailBean.class);
        Assert.assertNotNull(outMail);
        Assert.assertEquals("mnova@example.com", outMail.getFrom());
        Assert.assertEquals("Test Article", outMail.getSubject());
        Assert.assertEquals("<p>This is the body</p>", outMail.getBody());
        Assert.assertEquals("This is the lead", outMail.getLead());
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        mailProcessor.init();
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .process(mailProcessor)
                        .to("mock:result");
            }
        };
    }
}