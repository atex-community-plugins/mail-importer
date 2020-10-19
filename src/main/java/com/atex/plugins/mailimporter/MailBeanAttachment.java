package com.atex.plugins.mailimporter;

/**
 * MailBeanAttachment
 *
 * @author mnova
 */
public class MailBeanAttachment {

    private String contentType;
    private byte[] content;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(final byte[] content) {
        this.content = content;
    }
}
