package com.atex.plugins.mailimporter;

import com.atex.onecms.content.ContentId;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

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
public class MailProcessor
    implements Processor
{
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final MailParser parser = new MailParser();
    private final ContentPublisher publisher = new ContentPublisher();

    public void process(final Exchange exchange)
        throws Exception
    {
        MailBean mail = parser.parse(exchange);
        ContentId contentId = publisher.publish(mail);

        LOG.info(String.format("Article from mail published as '%s'!", contentId.toString()));
    }


}
