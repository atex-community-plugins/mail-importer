package com.atex.plugins.mailimporter;

import org.apache.camel.Exchange;

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

}
