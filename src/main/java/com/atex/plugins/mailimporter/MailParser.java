package com.atex.plugins.mailimporter;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.commons.mail.util.MimeMessageParser;

import com.atex.plugins.mailimporter.MailImporterConfig.Signature;

/**
 * MailParser
 *
 * @author mnova
 */
public interface MailParser {

    default MailBean createMailBean() {
        return new MailBean();
    }

    MailBean parse(final Exchange exchange) throws Exception;

    String getMessageText(MimeMessageParser messageParser);

    default MailBean removeSignatures(final MailBean bean,
                                      final List<Signature> signatureList) {
        if (bean != null) {
            bean.setBody(removeSignatures(bean.getBody(), signatureList));
        }
        return bean;
    }

    default String removeSignatures(final String text,
                                    final List<Signature> signatureList) {
        throw new UnsupportedOperationException();
    }

}
