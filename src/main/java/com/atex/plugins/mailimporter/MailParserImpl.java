package com.atex.plugins.mailimporter;

import static com.atex.plugins.mailimporter.StringUtils.EMAIL_HTML_PATTERN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.apache.camel.Exchange;
import org.apache.camel.component.mail.MailMessage;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.atex.plugins.mailimporter.MailImporterConfig.Signature;
import com.polopoly.util.StringUtil;

/**
 * <p>
 * Simple e-mail parser capable of parsing the example e-mail
 * format used by this Polopoly Mail Publishing integration.
 * </p>
 *
 * <p>
 * Since the Camel Mail component does not seem to provide
 * much help in providing the plain content mail body for
 * multipart e-mail, we use {@link MimeMessageParser}
 * from Apache Email for that.
 * </p>
 */
public class MailParserImpl implements MailParser {

    private static final Logger LOG = LoggerFactory.getLogger(MailParserImpl.class);

    private static final String PARAGRAPH_DELIMITER = "\n\n";

    public MailParserImpl() {
    }

    @Override
    public MailBean parse(final Exchange exchange) throws Exception {
        MailMessage mailMessage = exchange.getIn(MailMessage.class);
        Message originalMessage = mailMessage.getOriginalMessage();

        if (!(originalMessage instanceof MimeMessage)) {
            throw new RuntimeException("Unknown e-mail message format received!");
        }

        MimeMessage realMessage = (MimeMessage) originalMessage;
        MimeMessageParser messageParser = new MimeMessageParser(realMessage).parse();

        MailBean mailBean = createMailBean();

        String subject = messageParser.getSubject();
        List<Address> to = messageParser.getTo();
        String toAddress = to.stream()
                             .map(Address::toString)
                             .collect(Collectors.joining(","));
        mailBean.setTo(toAddress);
        mailBean.setSubject(subject);

        String from = messageParser.getFrom();
        mailBean.setFrom(from);

        Map<String, DataHandler> attachments = mailMessage.getAttachments();

        Map<String, MailBeanAttachment> attachmentFiles = new HashMap<>();

        final MailRouteConfig routeConfig = mailMessage.getHeader("X-ROUTE-CONFIG", MailRouteConfig.class);
        if (attachments.size() > 0) {
            final long minImageSize = Optional.ofNullable(routeConfig)
                                              .map(MailRouteConfig::getImageMinSize)
                                              .orElse(-1L);
            for (String attachmentKey : attachments.keySet()) {
                final DataHandler dataHandler = attachments.get(attachmentKey);
                final String contentType = Optional.ofNullable(dataHandler.getContentType())
                                                   .orElse("")
                                                   .toLowerCase();

                final String filename = dataHandler.getName();
                final byte[] data = exchange.getContext()
                                            .getTypeConverter()
                                            .convertTo(byte[].class, dataHandler.getInputStream());
                if (!contentType.startsWith("image") || data.length > minImageSize) {
                    LOG.info(String.format("Found attachment %s (%s) of size %d",
                            filename,
                            contentType,
                            data.length));
                    final MailBeanAttachment attachment = new MailBeanAttachment();
                    attachment.setContentType(contentType);
                    attachment.setContent(data);
                    attachmentFiles.put(filename, attachment);
                } else {
                    LOG.warn(String.format("Skipping attachment %s (%s of size %d) minImageSize is %d",
                            filename,
                            contentType,
                            data.length,
                            minImageSize));
                }
            }
        }

        mailBean.setAttachments(attachmentFiles);

        String body = normalizeLineEndings(messageParser.getPlainContent());
        if (StringUtils.isHtmlBody(body)) {
            setHtmlContent(mailBean, body);
        } else {
            setPlainTextContent(mailBean, body);
        }

        if (routeConfig != null) {
            if (routeConfig.getSignatures() != null && routeConfig.getSignatures().size() > 0) {
                return removeSignatures(mailBean, routeConfig.getSignatures());
            }
        }

        return mailBean;
    }

    @Override
    public String removeSignatures(final String text,
                                   final List<Signature> signatureList) {
        if (StringUtils.notEmpty(text) && signatureList != null && signatureList.size() > 0) {
            final StringBuilder sb = new StringBuilder();
            String s = text;
            while (true) {
                int idx = s.toLowerCase().indexOf("<p>");
                if (idx >= 0) {
                    sb.append(s, 0, idx);
                    sb.append("<p>");
                    int end = s.toLowerCase().indexOf("</p>", idx);
                    if (end < 0) {
                        end = s.length();
                    }
                    sb.append(cleanupSignatures(s.substring(idx + 3, end), signatureList));
                    sb.append("</p>");
                    s = s.substring(end + 4);
                } else {
                    sb.append(cleanupSignatures(s, signatureList));
                    break;
                }
            }
            return sb.toString();
        }
        return text;
    }

    private String cleanupSignatures(final String text,
                                     final List<Signature> signatureList) {
        if (StringUtils.notEmpty(text)) {
            final List<Pattern> patterns = signatureList.stream()
                                                        .map(Signature::getRegex)
                                                        .map(Pattern::compile)
                                                        .collect(Collectors.toList());
            final String[] lines = text.split("\n");
            final List<String> ll = new ArrayList<>();
            for (String l : lines) {
                int before = 0;
                boolean match = false;
                for (int i = 0; i < patterns.size(); i++) {
                    final Pattern p = patterns.get(i);
                    if (p.matcher(l).find()) {
                        before = signatureList.get(i).getBefore();
                        match = true;
                        break;
                    }
                }
                if (match) {
                    final int e = ll.size();
                    final int top = Math.max(0, ll.size() - before);
                    if (e > top) {
                        ll.subList(top, e).clear();
                    }
                    break;
                } else {
                    ll.add(l);
                }
            }
            for (int i = ll.size() - 1; i >= 0; i--) {
                if (StringUtil.isEmpty(ll.get(i))) {
                    ll.remove(i);
                } else {
                    break;
                }
            }
            return String.join("\n", ll);
        }
        return text;
    }

    protected void setPlainTextContent(MailBean mailBean, String body) {
        String lead = "";

        if (body.contains(PARAGRAPH_DELIMITER)) {
            int paragraphIndex = body.indexOf(PARAGRAPH_DELIMITER);

            lead = body.substring(0, paragraphIndex).trim();
            body = body.substring(paragraphIndex + PARAGRAPH_DELIMITER.length()).trim();
        }

        // E-mail clients use in-line chunk data references to mark positions where images
        // are in-lined in the mail. The default Mac Mail client (might be others as well)
        // use a format like [cid:af1b90bbaa34355a] that neither GMail nor Apache Email
        // seem to understand.
        //
        // I order to avoid these ugly markers in the resulting article texts, we try to clear
        // them out, preferably without affecting anything else.

        lead = removeInlinedCIDReferences(lead);
        body = removeInlinedCIDReferences(body);

        body = "<p>" + StringEscapeUtils.escapeHtml(body) + "</p>";

        mailBean.setBody(body);
        mailBean.setLead(lead);
    }

    protected void setHtmlContent(MailBean mailBean, String fullText) {
        // HTML Layout is specified as:
        //          lead
        //          <p>{whitespace}</p>
        //          body
        //
        //          newlines are not required.
        String lead;
        String body;

        Matcher matcher = EMAIL_HTML_PATTERN.matcher(fullText);
        if (matcher.find() && matcher.groupCount() > 0) {
            int paragraphTagIndex = matcher.end(1);

            lead = fullText.substring(0, paragraphTagIndex).trim();

            if (matcher.groupCount() > 2) {
                int bodyStart = matcher.start(3);
                body = fullText.substring(bodyStart).trim();
            } else {
                body = "";
            }
        } else {
            lead = "";
            body = fullText;
        }

        // E-mail clients use in-line chunk data references to mark positions where images
        // are in-lined in the mail. The default Mac Mail client (might be others as well)
        // use a format like [cid:af1b90bbaa34355a] that neither GMail nor Apache Email
        // seem to understand.
        //
        // I order to avoid these ugly markers in the resulting article texts, we try to clear
        // them out, preferably without affecting anything else.

        lead = removeInlinedCIDReferences(lead);
        lead = StringEscapeUtils.unescapeXml(StringEscapeUtils.escapeHtml(lead));

        body = removeInlinedCIDReferences(body);
        body = StringEscapeUtils.unescapeXml(StringEscapeUtils.escapeHtml(body));

        mailBean.setBody(body);
        mailBean.setLead(lead);
    }

    private String normalizeLineEndings(final String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
    }

    private String removeInlinedCIDReferences(final String text) {
        return text.replaceAll("\\[cid:.*?\\]\n*", "");
    }
}
