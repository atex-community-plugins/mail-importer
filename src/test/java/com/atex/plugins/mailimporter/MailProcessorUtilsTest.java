package com.atex.plugins.mailimporter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.atex.onecms.content.ContentManager;
import com.atex.onecms.image.exif.MetadataTagsAspectBean;
import com.atex.plugins.mailimporter.MailProcessorUtils.MetadataTagsHolder;
import com.atex.plugins.baseline.ws.JettyWrapper;
import com.atex.plugins.baseline.ws.JettyRule;
import com.atex.plugins.mailimporter.ws.MetadataServiceServlet;
import com.polopoly.common.lang.ClassUtil;
import com.polopoly.model.ModelDomain;

/**
 * MailProcessorUtilsTest
 *
 * @author mnova
 */
public class MailProcessorUtilsTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final JettyRule jettyWrapperRule = new JettyRule();

    @Mock
    ContentManager contentManager;

    @Mock
    ModelDomain modelDomain;

    private MailProcessorUtils mpu;

    @Before
    public void before() {
        mpu = new MailProcessorUtils(contentManager, modelDomain);
    }

    @Test
    public void test_get_metadata() throws Exception {
        final JettyWrapper jw = jettyWrapperRule.getJettyWrapper();
        final String servletPath = "/metadata/image/extract";
        final KeepingMetadataServiceServlet servlet = new KeepingMetadataServiceServlet();
        jw.addServlet(servlet, servletPath);
        System.setProperty("image.metadata.service.url", jw.getURL(servletPath));

        try (final InputStream is = ClassUtil.getResourceAsStream(this.getClass(), "/image.jpg")) {
            final byte[] imageData = IOUtils.toByteArray(is);
            try (final ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {

                final Optional<MetadataTagsAspectBean> metadataTags = new ImageMetadataExtraction().extract(bais);
                Assert.assertTrue(metadataTags.isPresent());

                Assert.assertEquals(600, metadataTags.get().getImageWidth().intValue());
                Assert.assertEquals(450, metadataTags.get().getImageHeight().intValue());

                Assert.assertArrayEquals(imageData, servlet.getPostedBytes());
            }
        }
    }

    @Test
    public void test_get_metadata_tags() throws Exception {
        final JettyWrapper jw = jettyWrapperRule.getJettyWrapper();
        final String servletPath = "/metadata/image/extract";
        final KeepingMetadataServiceServlet servlet = new KeepingMetadataServiceServlet();
        jw.addServlet(servlet, servletPath);
        System.setProperty("image.metadata.service.url", jw.getURL(servletPath));

        try (final InputStream is = ClassUtil.getResourceAsStream(this.getClass(), "/image.jpg")) {
            final byte[] imageData = IOUtils.toByteArray(is);
            try (final ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {

                final MetadataTagsHolder metadataTags = mpu.getMetadataTags(bais);
                Assert.assertNotNull(metadataTags);

                Assert.assertNotNull(metadataTags.customTags);
                Assert.assertNotNull(metadataTags.tags);

                Assert.assertEquals(600, metadataTags.tags.getImageWidth().intValue());
                Assert.assertEquals(450, metadataTags.tags.getImageHeight().intValue());

                Assert.assertArrayEquals(imageData, servlet.getPostedBytes());
            }
        }
    }

    public static class KeepingMetadataServiceServlet extends MetadataServiceServlet {

        private byte[] postedBytes;

        public KeepingMetadataServiceServlet() throws IOException {
            super(HttpServletResponse.SC_OK);
            contentType(MediaType.APPLICATION_JSON);
            body(MailProcessorUtilsTest.class, "/metadata.json");
        }

        public byte[] getPostedBytes() {
            return postedBytes;
        }

        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
                throws ServletException, IOException {

            postedBytes = IOUtils.toByteArray(req.getInputStream());

            super.doPost(req, resp);
        }
    }
}