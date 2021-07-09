package com.atex.plugins.mailimporter;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
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
    public static final Pattern EMPTY_PARA_PATTERN = Pattern.compile("^((\\n*<p>(&nbsp;)?</p>\\n*)|(\n+))");

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
        mailBean.setSubject(StringUtil.trim(subject));

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

        final String body = normalizeLineEndings(getMessageText(messageParser));
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
    public String getMessageText(final MimeMessageParser messageParser) {
        final String content = messageParser.getPlainContent();
        if (!StringUtil.isEmpty(content)) {
            return content;
        }
        final String htmlContent = messageParser.getHtmlContent();
        if (htmlContent != null) {
            return Jsoup.clean(htmlContent, simpleTextWithParagraphs());
        }
        return "";
    }

    private String cleanUpHtml(final String text) {
        return text
                   // NBSP will cause lot of issues, convert them
                   .replace("&nbsp;", " ")
                   .replace('\u00A0', ' ')

                   // ignore text lines
                   .replace("\n", "")
                   .replace("\r", "")

                   // convert paragraphs into new lines.
                   .replace("<p>", "")
                   .replace("</p>", "\n")

                   // convert line breaks into new lines
                   .replace("<br>", "\n")
                   .replace("<br/>", "\n")
                   .replace("<br />", "\n")

                   // convert divs into new lines
                   .replace("<div>", "")
                   .replace("</div>", "\n")

                   // normalize line endings
                   .replace("\n ", "\n")
                   .replace(" \n", "\n")
                   .replaceAll("\r\n", "\n")
                   .replaceAll("\r", "\n");
    }

    private Whitelist simpleTextWithParagraphs() {
        return new Whitelist().addTags(StringUtils.EMAIL_HTML_TAGS.toArray(new String[] {}));
    }

    @Override
    public String removeSignatures(final String text,
                                   final List<Signature> signatureList) {
        if (StringUtils.notEmpty(text) && signatureList != null && signatureList.size() > 0) {
            if (text.toLowerCase().contains("<p>")) {
                final String lines = StringUtils.paragraphsToLines(text);
                final String newText = cleanupSignatures(lines, signatureList);
                return StringUtils.linesToParagraphs(newText);
            } else {
                return cleanupSignatures(text, signatureList);
            }
        }
        return text;
    }

    private String cleanupSignatures(final String text,
                                     final List<Signature> signatureList) {
        if (StringUtils.notEmpty(text)) {
            final List<Pattern> patterns = signatureList.stream()
                                                        .map(Signature::getRegex)
                                                        .map(re -> Pattern.compile(re, Pattern.MULTILINE))
                                                        .collect(Collectors.toList());
            String newText = text;
            for (int pIdx = 0; pIdx < patterns.size(); pIdx++) {
                final Pattern p = patterns.get(pIdx);
                final Matcher matcher = p.matcher(newText);
                if (matcher.find()) {
                    newText = newText.substring(0, matcher.start());
                    final String[] lines = newText.split("\n");
                    final List<String> ll = new ArrayList<>(Arrays.asList(lines));
                    final int e = ll.size();
                    final int before = signatureList.get(pIdx).getBefore();
                    final int top = Math.max(0, ll.size() - before);
                    if (e > top) {
                        ll.subList(top, e).clear();
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
            }
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

        body = StringUtils.linesToParagraphs(
                StringEscapeUtils.escapeHtml(body.replace('\u00A0', ' '))
        );

        mailBean.setBody(body);
        mailBean.setLead(lead);
    }

    protected void setHtmlContent(final MailBean mailBean,
                                  final String html) {
        // HTML Layout is specified as:
        //          lead
        //          <p>{whitespace}</p>
        //          body
        //
        //          newlines are not required.

        final String fullText = Parser.unescapeEntities(html, true);

        String lead = "";
        // trim lines to make sure we don't have unwanted spaces.
        String body = StringUtils.trimLines(cleanUpHtml(fullText));
        if (body.contains("\n\n")) {
            int paragraphIndex = body.indexOf("\n\n");
            lead = body.substring(0, paragraphIndex).trim();
            body = body.substring(paragraphIndex + "\n\n".length()).trim();
        } else if (body.contains("\n")) {
            int paragraphIndex = body.indexOf("\n");
            lead = body.substring(0, paragraphIndex).trim();
            body = body.substring(paragraphIndex + "\n".length()).trim();
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
        body = StringUtils.linesToParagraphs(StringEscapeUtils.unescapeXml(StringEscapeUtils.escapeHtml(body)));

        mailBean.setBody(body);
        mailBean.setLead(lead);
    }

    private String normalizeLineEndings(final String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\r\n", "\n")
                   .replaceAll("\n ", "\n")
                   .replaceAll("\r", "\n");
    }

    String removeStartingEmptyLines(String body) {
        while (true) {
            final Matcher matcher = EMPTY_PARA_PATTERN.matcher(body);
            if (matcher.find()) {
                body = body.substring(matcher.end());
            } else {
                break;
            }
        }
        return body;
    }

    private String removeInlinedCIDReferences(final String text) {
        return text.replaceAll("\\[cid:.*?]\n*", "");
    }
}
