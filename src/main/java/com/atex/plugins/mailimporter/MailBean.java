package com.atex.plugins.mailimporter;

import java.util.HashMap;
import java.util.Map;

/**
 * Bean class representing the example e-mail format used
 * by this Polopoly Mail Publishing integration.
 */
public class MailBean
{
    private String subject = null;

    private String lead = null;
    private String body = null;

    private String from = null;

    private Map<String, byte[]> attachments = new HashMap<String, byte[]>();


    public MailBean(final String subject,
                    final String lead,
                    final String body,
                    final String from,
                    final Map<String, byte[]> attachments)
    {
        this.subject = subject;

        this.lead = lead;
        this.body = body;
        this.from = from;

        this.attachments = attachments;
    }

    public String getSubject()
    {
        return subject;
    }

    public String getLead()
    {
        return lead;
    }

    public String getBody()
    {
        return body;
    }

    public Map<String, byte[]> getAttachments()
    {
        return attachments;
    }

    public String getFrom() {
        return from;
    }
}
