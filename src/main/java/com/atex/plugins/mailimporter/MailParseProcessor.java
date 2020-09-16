package com.atex.plugins.mailimporter;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.atex.plugins.mailimporter.util.MailImporterServiceLoaderUtil;

/**
 * <p>
 *   Simple Camel {@link Processor} used to process and import
 *   articles sent to a Polopoly installation through e-mail.
 * </p>
 *
 * <p>
 *   Please note that this Polopoly Mail Publishing integration
 *   can only be used in combination with the Greenfield Online
 *   project.
 * </p>
 */
@Component
public class MailParseProcessor
    implements Processor
{
    private MailParser parser = null;

    public MailParseProcessor() {
    }

    public MailParseProcessor(final MailParser parser) {
        this.parser = parser;
    }

    @PostConstruct
    public void init() {
        if (parser == null) {
            parser = MailImporterServiceLoaderUtil.loadService(MailParser.class, MailParserImpl.class);
        }
    }

    public void process(final Exchange exchange) throws Exception {
        final Message inMsg = exchange.getIn();
        final MailRouteConfig routeConfig = inMsg.getHeader("X-ROUTE-CONFIG", MailRouteConfig.class);

        final MailBean mail = parser.parse(exchange);

        exchange.getOut().setBody(mail);
        if (routeConfig != null) {
            exchange.getOut().setHeader("X-ROUTE-CONFIG", routeConfig);
        }
    }

}
