package com.atex.plugins.mailimporter;

import java.util.List;
import java.util.stream.Collectors;

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
import com.atex.plugins.mailimporter.util.MailImporterServiceLoaderUtil;
import com.polopoly.integration.IntegrationServerApplication;

/**
 * MailPublishProcessor
 *
 * @author mnova
 */
@Component
public class MailPublishProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailPublishProcessor.class.getName());

    private MailPublisher publisher = null;

    public MailPublishProcessor() {
    }

    public MailPublishProcessor(final ContentPublisher publisher) {
        this.publisher = publisher;
    }

    @PostConstruct
    public void init() {
        if (publisher == null) {
            publisher = MailImporterServiceLoaderUtil.loadService(MailPublisher.class, ContentPublisher.class);
            publisher.init(IntegrationServerApplication.getPolopolyApplication());
        }
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message inMsg = exchange.getIn();
        final MailBean mail = inMsg.getBody(MailBean.class);
        final MailRouteConfig routeConfig = inMsg.getHeader("X-ROUTE-CONFIG", MailRouteConfig.class);
        final List<ContentId> ids = publisher.publish(mail, routeConfig);

        LOGGER.info(String.format("Contents from mail published as '%s'!", ids.stream()
                                                                             .map(IdUtil::toIdString)
                                                                             .collect(Collectors.joining(","))));

        exchange.getOut().setBody(ids);
    }

}
