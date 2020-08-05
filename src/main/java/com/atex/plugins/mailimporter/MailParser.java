package com.atex.plugins.mailimporter;

import org.apache.camel.Exchange;
import org.apache.camel.component.mail.MailMessage;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.mail.util.MimeMessageParser;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 *   Simple e-mail parser capable of parsing the example e-mail
 *   format used by this Polopoly Mail Publishing integration.
 * </p>
 *
 * <p>
 *   Since the Camel Mail component does not seem to provide
 *   much help in providing the plain content mail body for
 *   multipart e-mail, we use {@link MimeMessageParser}
 *   from Apache Email for that.
 * </p>
 */
public class MailParser
{
    private static final String PARAGRAPH_DELIMITER = "\n\n";

    public MailParser()
    {

    }

    public MailBean parse(final Exchange exchange)
        throws Exception
    {
        MailMessage mailMessage = exchange.getIn(MailMessage.class);
        Message originalMessage = mailMessage.getOriginalMessage();

        if (!(originalMessage instanceof MimeMessage)) {
            throw new RuntimeException("Unknown e-mail message format received!");
        }

        MimeMessage realMessage = (MimeMessage) originalMessage;
        MimeMessageParser messageParser = new MimeMessageParser(realMessage).parse();

        MailBean mailBean = new MailBean();

        String subject = messageParser.getSubject();
        mailBean.setSubject(subject);

        String from = messageParser.getFrom();
        mailBean.setFrom(from);

        Map<String, DataHandler> attachments = exchange.getIn().getAttachments();

        Map<String, byte[]> attachmentFiles = new HashMap<>();

        if (attachments.size() > 0) {
            for (String attachmentKey : attachments.keySet()) {
                DataHandler dataHandler = attachments.get(attachmentKey);

                String filename = dataHandler.getName();
                byte[] data = exchange.getContext().getTypeConverter().convertTo(byte[].class, dataHandler.getInputStream());

                attachmentFiles.put(filename, data);
            }
        }

        mailBean.setAttachments(attachmentFiles);

        String body = normalizeLineEndings(messageParser.getPlainContent());
        if (StringUtils.isHtmlBody(body)) {
            setHtmlContent(mailBean, body);
        } else {
            setPlainTextContent(mailBean, body);
        }

        return mailBean;
    }

    protected void setPlainTextContent(MailBean mailBean, String body) {
        String lead = "";

        if (body.contains(PARAGRAPH_DELIMITER)) {
            int paragraphIndex = body.indexOf(PARAGRAPH_DELIMITER);

            lead = body.substring(0, paragraphIndex).trim();
            body = body.substring(paragraphIndex + PARAGRAPH_DELIMITER.length()).trim();
        }

        /**
         * E-mail clients use in-line chunk data references to mark positions where images
         * are in-lined in the mail. The default Mac Mail client (might be others as well)
         * use a format like [cid:af1b90bbaa34355a] that neither GMail nor Apache Email
         * seem to understand.
         *
         * I order to avoid these ugly markers in the resulting article texts, we try to clear
         * them out, preferably without affecting anything else.
         */

        lead = removeInlinedCIDReferences(lead);
        body = removeInlinedCIDReferences(body);

        body = "<p>" + StringEscapeUtils.escapeHtml(body) + "</p>";

        mailBean.setBody(body);
        mailBean.setLead(lead);
    }

    protected void setHtmlContent(MailBean mailBean, String body) {
        String lead = "";

        Pattern pattern = Pattern.compile("(.*)<p>(\\s*)<\\/p>(.*)");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            int paragraphIndex = matcher.end(1);

            lead = body.substring(0, paragraphIndex).trim();

            int bodyStart = matcher.start(3);
            body = body.substring(bodyStart).trim();
        }

        /**
         * E-mail clients use in-line chunk data references to mark positions where images
         * are in-lined in the mail. The default Mac Mail client (might be others as well)
         * use a format like [cid:af1b90bbaa34355a] that neither GMail nor Apache Email
         * seem to understand.
         *
         * I order to avoid these ugly markers in the resulting article texts, we try to clear
         * them out, preferably without affecting anything else.
         */

        lead = removeInlinedCIDReferences(lead);
        lead = StringEscapeUtils.unescapeXml(StringEscapeUtils.escapeHtml(lead));

        body = removeInlinedCIDReferences(body);
        body = StringEscapeUtils.unescapeXml(StringEscapeUtils.escapeHtml(body));

        mailBean.setBody(body);
        mailBean.setLead(lead);
    }

    private String normalizeLineEndings(final String text)
    {
        if (text == null) return "";
        return text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
    }

    private String removeInlinedCIDReferences(final String text)
    {
        return text.replaceAll("\\[cid:.*?\\]\n*", "");
    }
}
