package com.atex.plugins.mailimporter;

import org.junit.Assert;
import org.junit.Test;

import com.atex.plugins.mailimporter.util.MailImporterServiceLoaderUtil;

/**
 * MailParserImplTest
 *
 * @author mnova
 */
public class MailParserImplTest {

    @Test
    public void test_service_loader() {
        final MailParser mailParser = MailImporterServiceLoaderUtil.loadService(
                MailParser.class,
                MailParserImpl.class);
        Assert.assertNotNull(mailParser);
    }

}