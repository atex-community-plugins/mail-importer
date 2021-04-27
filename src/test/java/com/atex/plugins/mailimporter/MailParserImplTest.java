package com.atex.plugins.mailimporter;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.spi.HeadersMapFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.atex.plugins.mailimporter.MailImporterConfig.Signature;
import com.atex.plugins.mailimporter.util.MailImporterServiceLoaderUtil;

/**
 * MailParserImplTest
 *
 * @author mnova
 */
public class MailParserImplTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    Exchange exchange;

    @Mock
    CamelContext context;

    @Before
    public void before() {
       final HeadersMapFactory f = Mockito.mock(HeadersMapFactory.class);
       Mockito.when(context.getHeadersMapFactory())
              .thenReturn(f);
       Mockito.when(f.newMap())
              .thenReturn(new HashMap<>());
    }

    @Test
    public void test_service_loader() {
        final MailParser mailParser = createParser();
        Assert.assertNotNull(mailParser);
    }

   @Test
   public void testHtmlEmail() throws Exception {
       final MailBean bean = parse("/mails/TEST SISTEMI.eml");
       Assert.assertEquals("luca.pandolfini@lastampa.it", bean.getFrom());
       Assert.assertEquals("\"aosta.collab\" <aosta.collab@lastampa.it>", bean.getTo());
       Assert.assertEquals("TEST SISTEMI", bean.getSubject());
       Assert.assertEquals("<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
               "Nulla pulvinar est ullamcorper ipsum scelerisque mollis. Nunc sit amet enim ultricies, " +
               "pulvinar enim et, volutpat nibh. Fusce auctor turpis ut orci auctor facilisis. Phasellus " +
               "vel malesuada tortor. Pellentesque id purus ipsum. Donec tempor egestas commodo. " +
               "Ut nec euismod nisl. Praesent at ante tincidunt diam consectetur ornare. " +
               "Morbi posuere faucibus odio et pulvinar.</p>", bean.getBody());
       Assert.assertEquals("Lucpan", bean.getLead());
       Assert.assertEquals(0, bean.getAttachments().size());
   }

    @Test
    public void testPlainTextEmail() throws Exception {
        final MailBean bean = parse("/mails/TEST SISTEMI 2.eml");
        Assert.assertEquals("luca.pandolfini@lastampa.it", bean.getFrom());
        Assert.assertEquals("\"aosta.collab\" <aosta.collab@lastampa.it>", bean.getTo());
        Assert.assertEquals("TEST SISTEMI 2", bean.getSubject());
        Assert.assertEquals("<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Nulla pulvinar est ullamcorper ipsum scelerisque mollis. Nunc sit amet enim ultricies, " +
                "pulvinar enim et, volutpat nibh. Fusce auctor turpis ut orci auctor facilisis. Phasellus " +
                "vel malesuada tortor. Pellentesque id purus ipsum. Donec tempor egestas commodo. " +
                "Ut nec euismod nisl. Praesent at ante tincidunt diam consectetur ornare. " +
                "Morbi posuere faucibus odio et pulvinar.</p>", bean.getBody());
        Assert.assertEquals("Lucpan", bean.getLead());
        Assert.assertEquals(0, bean.getAttachments().size());
    }

    @Test
    public void testSignedEmail() throws Exception {
        final MailBean bean = parse(
                "/mails/signed_email.eml",
                Signature.of(
                        "This communication may contain confidential",
                        7
                )
        );
        Assert.assertEquals("This is the lead", bean.getLead());
        Assert.assertEquals("<p>This is the body line 1\n" +
                "\n" +
                "This is the body line 2\n" +
                "\n" +
                "This is the body line 3</p>", bean.getBody());
    }

    private MailBean parse(final String name,
                           final Signature...signatures) throws Exception {
       try (InputStream is = this.getClass().getResourceAsStream(name)) {
           Assert.assertNotNull(is);
           final Session session = Session.getDefaultInstance(new Properties());
           final MimeMessage msg = new MimeMessage(session, is);
           Assert.assertNotNull(msg);

           final MailMessage mm = new MailMessage(msg);
           mm.setCamelContext(context);
           Mockito.when(exchange.getIn(Mockito.eq(MailMessage.class)))
                  .thenReturn(mm);
           Mockito.when(exchange.getIn()).thenReturn(mm);

           final MailParser mailParser = createParser();
           final MailBean bean = mailParser.parse(exchange);
           Assert.assertNotNull(bean);
           if (signatures.length > 0) {
               return mailParser.removeSignatures(bean, Arrays.asList(signatures));
           }

           return bean;
       }
   }

   private MailParser createParser() {
       return MailImporterServiceLoaderUtil.loadService(
               MailParser.class,
               MailParserImpl.class);
   }

}