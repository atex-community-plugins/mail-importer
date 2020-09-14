package com.atex.plugins.mailimporter;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.atex.onecms.image.exif.MetadataTagsAspectBean;
import com.atex.plugins.baseline.ws.JettyRule;
import com.atex.plugins.baseline.ws.JettyWrapper;
import com.atex.plugins.mailimporter.ws.MetadataServiceServlet;
import com.polopoly.common.lang.ClassUtil;

/**
 * ImageMetadataExtractionTest
 *
 * @author mnova
 */
public class ImageMetadataExtractionTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final JettyRule jettyWrapperRule = new JettyRule();

    @Test
    public void test_image_metadata_extraction() throws Exception {
        final JettyWrapper jw = jettyWrapperRule.getJettyWrapper();
        final String servletPath = "/metadata/image/extract";
        jw.addServlet(
                new MetadataServiceServlet(HttpServletResponse.SC_OK)
                .body(this.getClass(), "/metadata.json")
                .contentType(MediaType.APPLICATION_JSON),
                servletPath
        );
        System.setProperty("image.metadata.service.url", jw.getURL(servletPath));
        try (final InputStream is = ClassUtil.getResourceAsStream(this.getClass(), "/image.jpg")) {
            final Optional<MetadataTagsAspectBean> metadataOpt = new ImageMetadataExtraction().extract(is);
            Assert.assertTrue(metadataOpt.isPresent());

            final MetadataTagsAspectBean meta = metadataOpt.get();
            Assert.assertNotNull(meta);

            Assert.assertEquals(600, meta.getImageWidth().intValue());
            Assert.assertEquals(450, meta.getImageHeight().intValue());
        }
    }

    @Test
    public void test_image_metadata_extraction_failed() throws Exception {
        final JettyWrapper jw = jettyWrapperRule.getJettyWrapper();
        final String servletPath = "/metadata/image/extract";
        jw.addServlet(
                new MetadataServiceServlet(HttpServletResponse.SC_OK)
                        .body(this.getClass(), "/metadataExifError.json")
                        .contentType(MediaType.APPLICATION_JSON),
                servletPath
        );
        System.setProperty("image.metadata.service.url", jw.getURL(servletPath));
        try (final InputStream is = ClassUtil.getResourceAsStream(this.getClass(), "/image.jpg")) {
            final Optional<MetadataTagsAspectBean> metadataOpt = new ImageMetadataExtraction().extract(is);
            Assert.assertTrue(metadataOpt.isPresent());

            final MetadataTagsAspectBean meta = metadataOpt.get();
            Assert.assertNotNull(meta);

            Assert.assertNull(meta.getImageWidth());
            Assert.assertNull(meta.getImageHeight());

            final Map<String, Map<String, ?>> tags = meta.getTags();
            Assert.assertNotNull(tags);
            final Map<String, ?> exifTool = tags.get("ExifTool");
            Assert.assertNotNull(exifTool);

            Assert.assertEquals("Unknown file type", exifTool.get("Error"));
        }
    }

}