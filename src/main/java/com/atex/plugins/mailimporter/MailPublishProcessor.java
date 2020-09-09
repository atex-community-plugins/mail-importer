package com.atex.plugins.mailimporter;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.atex.onecms.content.ContentId;
import com.atex.onecms.content.IdUtil;
import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.polopoly.integration.IntegrationServerApplication;

/**
 * MailPublishProcessor
 *
 * @author mnova
 */
@Component
public class MailPublishProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailPublishProcessor.class.getName());

    private ContentPublisher publisher = null;

    public MailPublishProcessor() {
    }

    public MailPublishProcessor(final ContentPublisher publisher) {
        this.publisher = publisher;
    }

    @PostConstruct
    public void init() {
        if (publisher == null) {
            publisher = new ContentPublisher(IntegrationServerApplication.getPolopolyApplication());
            publisher.init();
        }
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message inMsg = exchange.getIn();
        final MailBean mail = inMsg.getBody(MailBean.class);
        final MailRouteConfig routeConfig = inMsg.getHeader("X-ROUTE-CONFIG", MailRouteConfig.class);
        final ContentId contentId = publisher.publish(mail, routeConfig);

        LOGGER.info(String.format("Article from mail published as '%s'!", IdUtil.toIdString(contentId)));

        exchange.getOut().setBody(contentId);
    }

}
