package com.atex.plugins.mailimporter;

import java.util.Collections;
import java.util.List;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
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
public class MailPublishProcessorTest extends AbstractProcessorTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ContentPublisher contentPublisher;

    private MailPublishProcessor publishProcessor;

    @Test
    public void test_publish_with_route_config() throws Exception {
        final ContentId newId = IdUtil.fromString("oncms:1234");
        final MailRouteConfig routeConfig = new MailRouteConfig();
        Mockito.when(contentPublisher.publish(Mockito.any(), Mockito.eq(routeConfig)))
               .thenReturn(Collections.singletonList(newId));
        final MockEndpoint mock = setupMockEndpoint(1, List.class);
        final MailBean bean = new MailBean();
        template.sendBodyAndHeader("direct:start", bean, "X-ROUTE-CONFIG", routeConfig);

        assertMockEndpointsSatisfied();

        final List<ContentId> outIds = mock.getReceivedExchanges()
                                           .get(0)
                                           .getIn()
                                           .getBody(List.class);
        Assert.assertNotNull(outIds);
        Assert.assertEquals(1, outIds.size());
        final ContentId outContentId = outIds.get(0);
        Assert.assertNotNull(outContentId);
        Assert.assertEquals(newId, outContentId);

        final ArgumentCaptor<MailRouteConfig> configCaptor = ArgumentCaptor.forClass(MailRouteConfig.class);
        Mockito.verify(contentPublisher).publish(Mockito.any(), configCaptor.capture());

        final MailRouteConfig usedConfig = configCaptor.getValue();
        Assert.assertEquals(routeConfig, usedConfig);
    }

    @Test
    public void test_publish_without_route_config() throws Exception {
        final ContentId newId = IdUtil.fromString("oncms:1234");
        Mockito.when(contentPublisher.publish(Mockito.any(), Mockito.any()))
               .thenReturn(Collections.singletonList(newId));
        final MockEndpoint mock = setupMockEndpoint(1, List.class);
        final MailBean bean = new MailBean();
        template.sendBody("direct:start", bean);

        assertMockEndpointsSatisfied();

        final List<ContentId> outIds = mock.getReceivedExchanges()
                                           .get(0)
                                           .getIn()
                                           .getBody(List.class);
        Assert.assertNotNull(outIds);
        Assert.assertEquals(1, outIds.size());
        final ContentId outContentId = outIds.get(0);
        Assert.assertNotNull(outContentId);
        Assert.assertEquals(newId, outContentId);

        final ArgumentCaptor<MailRouteConfig> configCaptor = ArgumentCaptor.forClass(MailRouteConfig.class);
        Mockito.verify(contentPublisher).publish(Mockito.any(), configCaptor.capture());

        final MailRouteConfig usedConfig = configCaptor.getValue();
        Assert.assertNull(usedConfig);
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        publishProcessor = new MailPublishProcessor(contentPublisher);
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .process(publishProcessor)
                        .to("mock:result");
            }
        };
    }

}