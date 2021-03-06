package com.atex.plugins.mailimporter;

import java.util.HashMap;
import java.util.Map;

/**
 * Bean class representing the example e-mail format used
 * by this Polopoly Mail Publishing integration.
 */
public class MailBean {
    private String subject = null;

    private String lead = null;
    private String body = null;

    private String from = null;

    private Map<String, MailBeanAttachment> attachments = new HashMap<>();
    private String to;

    public MailBean() {
    }

    public MailBean(final String subject,
                    final String lead,
                    final String body,
                    final String from,
                    final Map<String, MailBeanAttachment> attachments) {
        this.subject = subject;

        this.lead = lead;
        this.body = body;
        this.from = from;

        this.attachments = attachments;
    }

    public String getSubject() {
        return subject;
    }

    public String getLead() {
        return lead;
    }

    public String getBody() {
        return body;
    }

    public Map<String, MailBeanAttachment> getAttachments() {
        return attachments;
    }

    public String getFrom() {
        return from;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setLead(String lead) {
        this.lead = lead;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setAttachments(Map<String, MailBeanAttachment> attachments) {
        this.attachments = attachments;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTo() {
        return to;
    }
}
