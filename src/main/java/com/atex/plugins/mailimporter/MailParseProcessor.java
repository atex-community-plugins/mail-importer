package com.atex.plugins.mailimporter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.mail.internet.MimeMessage;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.mail.MailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOG = LoggerFactory.getLogger(MailParseProcessor.class);

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
        final MailBean mail = parser.parse(exchange);
        exchange.getOut().setBody(mail);
        final MailRouteConfig config = inMsg.getHeader("X-ROUTE-CONFIG", MailRouteConfig.class);
        if (config != null && mail != null) {
            exchange.getOut().setHeader("X-ROUTE-CONFIG", config);
            if (StringUtils.notEmpty(config.getDumpFolder())) {
                dumpEmail(exchange, mail, config.getDumpFolder());
            }
        }
    }

    private void dumpEmail(final Exchange exchange,
                           final MailBean mail,
                           final String dumpFolder) {
        final MailMessage mailMessage = exchange.getIn(MailMessage.class);
        final javax.mail.Message originalMessage = mailMessage.getOriginalMessage();

        if (!(originalMessage instanceof MimeMessage)) {
            throw new RuntimeException("Unknown e-mail message format received!");
        }

        final MimeMessage realMessage = (MimeMessage) originalMessage;
        final File path = mkdirs(new File(dumpFolder));
        final String mailId = System.currentTimeMillis() + ".eml";
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        final File baseFolder = mkdirs(new File(path, sdf.format(new Date())));
        final File f = new File(baseFolder, mailId);
        LOG.info(String.format(
                "Write '%s' with id %s to %s",
                mail.getSubject(),
                mailId,
                baseFolder.getAbsolutePath()
        ));
        try (FileOutputStream fos = new FileOutputStream(f, false)) {
            realMessage.writeTo(fos);
        } catch (Exception e) {
            LOG.error("cannot write " + mailId + ": " + e.getMessage(), e);
        }
        new GZipper().zip(f);
        f.delete();
    }

    private File mkdirs(final File f) {
        if (!f.exists()) {
            if (!f.mkdirs()) {
                LOG.warn("Cannot create " + f.getAbsolutePath());
            }
        }
        return f;
    }

}
