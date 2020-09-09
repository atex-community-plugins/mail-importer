package com.atex.plugins.mailimporter;

import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.atex.onecms.content.ContentId;
import com.atex.onecms.content.IdUtil;
import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;

/**
 * MailPublishProcessorTest
 *
 * @author mnova
 */
public class MailImporterProcessorTest extends AbstractProcessorTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ContentPublisher contentPublisher;

    @Mock
    private MailParser parser;

    private MailParseProcessor parseProcessor;
    private MailPublishProcessor publishProcessor;

    @Test
    public void test_verify_route_config_is_passed_on() throws Exception {

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setFrom("Marco Nova <mnova@atex.com>");
        msg.setSubject("Test Article");
        msg.setText("This is the simple text");
        msg.setRecipient(RecipientType.TO, new InternetAddress("mock.endpoint@example.com", "Mock Endpoint <mock.endpoint@example.com>"));

        final MailBean expectedMail = new MailBean();

        Mockito.when(parser.parse(Mockito.any()))
               .then(invocation -> {
                   final Exchange exchange = invocation.getArgumentAt(0, Exchange.class);
                   final MailMessage body = exchange.getIn().getBody(MailMessage.class);
                   if (body != null &&
                           body.getMessage() != null &&
                           body.getMessage().getSubject().equals("Test Article")) {
                       return expectedMail;
                   }
                   throw new AssertionError("No mail message has been received");
               });

        final ContentId newId = IdUtil.fromString("oncms:1234");
        Mockito.when(contentPublisher.publish(Mockito.any(), Mockito.any()))
               .thenReturn(newId);
        final MockEndpoint mock = setupMockEndpoint(1, ContentId.class);

        final MailRouteConfig expectedRouteConfig = new MailRouteConfig();
        //template.sendBody("direct:start", new MailMessage(msg));
        template.sendBodyAndHeader("direct:start", new MailMessage(msg), "X-ROUTE-CONFIG", expectedRouteConfig);

        assertMockEndpointsSatisfied();

        final ContentId outContentId = mock.getReceivedExchanges()
                                           .get(0)
                                           .getIn()
                                           .getBody(ContentId.class);
        Assert.assertNotNull(outContentId);
        Assert.assertEquals(newId, outContentId);

        final ArgumentCaptor<MailBean> mailCaptor = ArgumentCaptor.forClass(MailBean.class);
        final ArgumentCaptor<MailRouteConfig> configCaptor = ArgumentCaptor.forClass(MailRouteConfig.class);
        Mockito.verify(contentPublisher)
               .publish(mailCaptor.capture(), configCaptor.capture());

        final MailBean usedMail = mailCaptor.getValue();
        Assert.assertEquals(expectedMail, usedMail);

        final MailRouteConfig usedConfig = configCaptor.getValue();
        Assert.assertEquals(expectedRouteConfig, usedConfig);
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        parseProcessor = new MailParseProcessor(parser);
        publishProcessor = new MailPublishProcessor(contentPublisher);
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .process(parseProcessor)
                        .process(publishProcessor)
                        .to("mock:result");
            }
        };
    }

}