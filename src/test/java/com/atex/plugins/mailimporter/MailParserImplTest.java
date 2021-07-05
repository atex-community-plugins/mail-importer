package com.atex.plugins.mailimporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.spi.HeadersMapFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.atex.plugins.mailimporter.MailImporterConfig.Signature;
import com.atex.plugins.mailimporter.util.MailImporterServiceLoaderUtil;
import com.polopoly.common.lang.ClassUtil;

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

    @Mock
    TypeConverter typeConverter;

    @Before
    public void before() {
        final HeadersMapFactory f = Mockito.mock(HeadersMapFactory.class);
        Mockito.when(context.getHeadersMapFactory())
               .thenReturn(f);
        Mockito.when(f.newMap())
               .thenReturn(new HashMap<>());
        Mockito.when(context.getTypeConverter())
               .thenReturn(typeConverter);
        Mockito.when(exchange.getContext())
               .thenReturn(context);
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
        Assert.assertEquals(0, bean.getAttachments().size());
    }

    @Test
    public void testSignedEmailWithImages() throws Exception {
        final MailRouteConfig config = of(Signature.of(
                "This communication may contain confidential",
                7
                )
        );
        final byte[] imageArray = getImageArray();
        Mockito.when(typeConverter.convertTo(Mockito.any(), Mockito.any()))
               .thenReturn(imageArray);
        final MailBeanAttachment attachment = new MailBeanAttachment();
        attachment.setContent(imageArray);
        attachment.setContentType("image/jpg");
        final MailBean bean = parse(
                "/mails/signed_email.eml",
                Collections.singletonList(attachment),
                config
        );
        Assert.assertEquals("This is the lead", bean.getLead());
        Assert.assertEquals("<p>This is the body line 1\n" +
                "\n" +
                "This is the body line 2\n" +
                "\n" +
                "This is the body line 3</p>", bean.getBody());
        final Map<String, MailBeanAttachment> am = bean.getAttachments();
        Assert.assertEquals(1, am.size());
        final MailBeanAttachment mba = am.get("a0.jpg");
        Assert.assertNotNull(mba);
        Assert.assertEquals("image/jpg", mba.getContentType());
        Assert.assertEquals(imageArray, mba.getContent());
    }

    @Test
    public void testSignedEmailWithImagesBlockedBySize() throws Exception {
        final MailRouteConfig config = of(Signature.of(
                "This communication may contain confidential",
                7
                )
        );
        assert config != null;
        config.setImageMinSize(20000L);
        final byte[] imageArray = getImageArray();
        Mockito.when(typeConverter.convertTo(Mockito.any(), Mockito.any()))
               .thenReturn(imageArray);
        final MailBeanAttachment attachment = new MailBeanAttachment();
        attachment.setContent(imageArray);
        attachment.setContentType("image/jpg");
        final MailBean bean = parse(
                "/mails/signed_email.eml",
                Collections.singletonList(attachment),
                config
        );
        Assert.assertEquals("This is the lead", bean.getLead());
        Assert.assertEquals("<p>This is the body line 1\n" +
                "\n" +
                "This is the body line 2\n" +
                "\n" +
                "This is the body line 3</p>", bean.getBody());
        Assert.assertEquals(0, bean.getAttachments().size());
    }

    @Test
    public void testEmailWithMultilineSignature() throws Exception {
        final MailBean bean = parse("/mails/mail-with-attachment.eml",
                Signature.of(
                        "__+\n*GRUPPO EDITORIALE",
                        0
                ));
        Assert.assertEquals("mnova@atex.com", bean.getFrom());
        Assert.assertEquals("\"test.collab\" <test.collab@gedivisual.it>", bean.getTo());
        Assert.assertEquals("PROVA SISTEMI A - SONIA4", bean.getSubject());
        Assert.assertEquals("<p></p>", bean.getBody());
        Assert.assertEquals("sonbul", bean.getLead());
        Assert.assertEquals(0, bean.getAttachments().size());
    }

    private MailBean parse(final String name,
                           final Signature... signatures) throws Exception {
        return parse(name, (Consumer<MailMessage>) null, of(signatures));
    }

    private MailBean parse(final String name,
                           final List<MailBeanAttachment> attachments,
                           final MailRouteConfig config) throws Exception {
        return parse(name, mm -> {
            int counter = 0;
            final Map<String, DataHandler> aMap = new LinkedHashMap<>();
            for (MailBeanAttachment a : attachments) {
                final String aName = "a" + (counter++) + ".jpg";
                final DataSource ds = new InputStreamDataSource(a.getContent(), aName, a.getContentType());
                aMap.put(aName, new DataHandler(ds));
            }
            mm.setAttachments(aMap);
        }, config);
    }

    private MailBean parse(final String name,
                           final Consumer<MailMessage> mmHandler,
                           final MailRouteConfig config) throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(name)) {
            Assert.assertNotNull(is);
            final Session session = Session.getDefaultInstance(new Properties());
            final MimeMessage msg = new MimeMessage(session, is);
            Assert.assertNotNull(msg);

            final MailMessage mm = new MailMessage(msg);
            mm.setCamelContext(context);
            if (mmHandler != null) {
                mmHandler.accept(mm);
            }
            if (config != null) {
                mm.setHeader("X-ROUTE-CONFIG", config);
            }
            Mockito.when(exchange.getIn(Mockito.eq(MailMessage.class)))
                   .thenReturn(mm);
            Mockito.when(exchange.getIn()).thenReturn(mm);

            final MailParser mailParser = createParser();
            final MailBean bean = mailParser.parse(exchange);
            Assert.assertNotNull(bean);
            return bean;
        }
    }

    private MailRouteConfig of(final Signature...signatures) {
        if (signatures.length > 0) {
            final MailRouteConfig c = new MailRouteConfig();
            Arrays.stream(signatures).forEach(c.getSignatures()::add);
            return c;
        }
        return null;
    }

    private MailParser createParser() {
        return MailImporterServiceLoaderUtil.loadService(
                MailParser.class,
                MailParserImpl.class);
    }

    private byte[] getImageArray() {
        try (final InputStream is = ClassUtil.getResourceAsStream(this.getClass(), "/image.jpg")) {
            return IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class InputStreamDataSource implements DataSource {

        private final byte[] array;
        private final String name;
        private final String contentType;

        public InputStreamDataSource(final byte[] array,
                                     final String name,
                                     final String contentType) {
            this.array = array;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(array);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Read-only data");
        }
    }

}