package com.atex.plugins.mailimporter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.atex.plugins.baseline.policy.BaselinePolicy;
import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.atex.plugins.mailimporter.MailImporterConfig.Signature;
import com.polopoly.common.lang.ClassUtil;

/**
 * MailImporterConfigLoaderTest
 *
 * @author mnova
 */
public class MailImporterConfigLoaderTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    BaselinePolicy mock;

    @Test
    public void testLoaderValues() {
        setupValue("mailimporter_enabled", "true");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "incoming2");
        setupValue("article_aspect", "my.article");
        setupValue("image_aspect", "my.image");
        setupValue("accepted_image_extensions", "a,b,c");
        setupValue("attachment_name_pattern", "myext");
        setupValue("article_name_pattern", "Subject");
        setupValue("taxonomyId", "Taxo");
        setupValue("mail_uri", "imaps://mailserver?username=pippo");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("incoming2", config.getArticlePartition());
        Assert.assertEquals("my.article", config.getArticleAspect());
        Assert.assertEquals("my.image", config.getImageAspect());
        Assert.assertEquals(Arrays .asList("a", "b", "c"), config.getAcceptedImageExtensions());
        Assert.assertEquals("myext", config.getAttachmentNamePattern());
        Assert.assertEquals("Subject", config.getArticleNamePattern());
        Assert.assertEquals("Taxo", config.getTaxonomyId());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(1, mailUris.size());
        {
            final MailRouteConfig route = mailUris.get(0);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://mailserver?username=pippo", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("my.article", route.getArticleAspect());
            Assert.assertEquals("my.image", route.getImageAspect());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("incoming2", route.getArticlePartition());
            Assert.assertEquals("Taxo", route.getTaxonomyId());
            Assert.assertEquals("98", route.getPrincipalId());
            Assert.assertEquals(-1, route.getMinWords());
            Assert.assertEquals(-1L, route.getImageMinSize());
            Assert.assertEquals(0, route.getSignatures().size());
        }
    }

    @Test
    public void test_general_disabled() {
        setupValue("mailimporter_enabled", "false");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "incoming2");
        setupValue("mail_uri", "imaps://mailserver?username=pippo");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertFalse(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("incoming2", config.getArticlePartition());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(1, mailUris.size());
        {
            final MailRouteConfig route = mailUris.get(0);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://mailserver?username=pippo", route.getUri());
            Assert.assertFalse(route.isEnabled());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("incoming2", route.getArticlePartition());
            Assert.assertEquals("98", route.getPrincipalId());
            Assert.assertEquals(-1, route.getMinWords());
            Assert.assertEquals(-1L, route.getImageMinSize());
            Assert.assertEquals(0, route.getSignatures().size());
        }
    }

    @Test
    public void test_with_mail_uri_and_json_uri() {
        setupValue("mailimporter_enabled", "true");
        setupValue("article_aspect", "atex.onecms.article");
        setupValue("image_aspect", "atex.onecms.image");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "incoming2");
        setupValue("mail_uri", "imaps://mailserver?username=pippo");
        setupValue("taxonomyId", "Taxo");
        setupJson("/conf1.json");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("incoming2", config.getArticlePartition());
        Assert.assertEquals("atex.onecms.article", config.getArticleAspect());
        Assert.assertEquals("atex.onecms.image", config.getImageAspect());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(2, mailUris.size());
        {
            final MailRouteConfig route = mailUris.get(0);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://mailserver?username=pippo", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("incoming2", route.getArticlePartition());
            Assert.assertEquals("Taxo", route.getTaxonomyId());
            Assert.assertEquals("98", route.getPrincipalId());
            Assert.assertEquals("atex.onecms.article", route.getArticleAspect());
            Assert.assertEquals("atex.onecms.image", route.getImageAspect());
            Assert.assertEquals(-1, route.getMinWords());
            Assert.assertEquals(-1L, route.getImageMinSize());
            Assert.assertEquals(0, route.getSignatures().size());
        }
        {
            final MailRouteConfig route = mailUris.get(1);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://imap.ethereal?username=rickey.berger", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("production", route.getImagePartition());
            Assert.assertEquals("archive", route.getArticlePartition());
            Assert.assertEquals("Taxo", route.getTaxonomyId());
            Assert.assertEquals("2020", route.getPrincipalId());
            Assert.assertEquals("com.my.standard.article", route.getArticleAspect());
            Assert.assertEquals("com.my.standard.image", route.getImageAspect());
            Assert.assertEquals(20, route.getMinWords());
            Assert.assertEquals(60000L, route.getImageMinSize());
            Assert.assertEquals(1, route.getSignatures().size());
            {
                final Signature s = route.getSignatures().get(0);
                Assert.assertEquals(5, s.getBefore());
                Assert.assertEquals("This communication may contain confidential", s.getRegex());
            }
        }
    }

    @Test
    public void test_without_mail_uri_and_with_json_uri() {
        setupValue("mailimporter_enabled", "true");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "incoming2");
        setupValue("mail_uri", "");
        setupJson("/conf1.json");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("incoming2", config.getArticlePartition());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(1, mailUris.size());
        {
            final MailRouteConfig route = mailUris.get(0);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://imap.ethereal?username=rickey.berger", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("production", route.getImagePartition());
            Assert.assertEquals("archive", route.getArticlePartition());
            Assert.assertEquals("la-stampa.2014v1.sport.page", route.getWebPage());
            Assert.assertEquals("dam.assets.incoming.sport.d", route.getDeskLevel());
            Assert.assertEquals("SPORT", route.getSection());
            Assert.assertEquals("MAILBOX", route.getSource());
            Assert.assertEquals("2020", route.getPrincipalId());
            Assert.assertEquals("com.my.standard.article", route.getArticleAspect());
            Assert.assertEquals("com.my.standard.image", route.getImageAspect());
            Assert.assertEquals(20, route.getMinWords());
            Assert.assertEquals(60000L, route.getImageMinSize());
            Assert.assertEquals(1, route.getSignatures().size());
            {
                final Signature s = route.getSignatures().get(0);
                Assert.assertEquals(5, s.getBefore());
                Assert.assertEquals("This communication may contain confidential", s.getRegex());
            }
        }
    }

    @Test
    public void test_with_json_uri_disabled() {
        setupValue("mailimporter_enabled", "true");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "incoming2");
        setupValue("mail_uri", "");
        setupJson("/conf2.json");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("incoming2", config.getArticlePartition());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(0, mailUris.size());
    }

    @Test
    public void test_without_mail_uri_and_with_multiple_json_uri() {
        setupValue("mailimporter_enabled", "true");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "production");
        setupValue("mail_uri", "");
        setupValue("taxonomyId", "Taxo");
        setupJson("/conf3.json");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("production", config.getArticlePartition());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(3, mailUris.size());
        {
            final MailRouteConfig route = mailUris.get(0);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://imap.ethereal?username=rickey.berger", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("production", route.getArticlePartition());
            Assert.assertEquals("t1", route.getTaxonomyId());
            Assert.assertEquals("2021", route.getPrincipalId());
            Assert.assertEquals(15, route.getMinWords());
            Assert.assertEquals(70000L, route.getImageMinSize());
            Assert.assertEquals(0, route.getSignatures().size());
        }
        {
            final MailRouteConfig route = mailUris.get(1);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://imap.ethereal?username=clay.regazzoni", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("production", route.getArticlePartition());
            Assert.assertEquals("t2", route.getTaxonomyId());
            Assert.assertEquals("2022", route.getPrincipalId());
            Assert.assertEquals(15, route.getMinWords());
            Assert.assertEquals(70000L, route.getImageMinSize());
            Assert.assertEquals(0, route.getSignatures().size());
        }
        {
            final MailRouteConfig route = mailUris.get(2);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://imap.ethereal?username=ayrton.senna", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("production", route.getArticlePartition());
            Assert.assertEquals("Taxo", route.getTaxonomyId());
            Assert.assertEquals("98", route.getPrincipalId());
            Assert.assertEquals(15, route.getMinWords());
            Assert.assertEquals(70000L, route.getImageMinSize());
            Assert.assertEquals(0, route.getSignatures().size());
        }
    }

    @Test
    public void test_with_json_defaults() {
        setupValue("mailimporter_enabled", "true");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "incoming2");
        setupValue("mail_uri", "");
        setupValue("taxonomyId", "Taxo");
        setupJson("/conf4.json");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("incoming2", config.getArticlePartition());
        Assert.assertEquals("myTaxonomy", config.getTaxonomyId());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(1, mailUris.size());
        {
            final MailRouteConfig route = mailUris.get(0);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://imap.ethereal?username=rickey.berger", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("incoming2", route.getArticlePartition());
            Assert.assertEquals("la-stampa.2014v1.site", route.getWebPage());
            Assert.assertEquals("dam.assets.incoming.d", route.getDeskLevel());
            Assert.assertEquals("CRONACA", route.getSection());
            Assert.assertEquals("MAIL", route.getSource());
            Assert.assertEquals("myTaxonomy", route.getTaxonomyId());
            Assert.assertEquals("98", route.getPrincipalId());
            Assert.assertEquals(-1, route.getMinWords());
            Assert.assertEquals(-1L, route.getImageMinSize());
            Assert.assertEquals(0, route.getSignatures().size());
        }
    }

    @Test
    public void test_mail_uri_with_json_defaults() {
        setupValue("mailimporter_enabled", "true");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "production");
        setupValue("mail_uri", "imaps://mailserver?username=pippo");
        setupJson("/conf5.json");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("production", config.getArticlePartition());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(1, mailUris.size());
        {
            final MailRouteConfig route = mailUris.get(0);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://mailserver?username=pippo", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("production", route.getArticlePartition());
            Assert.assertEquals("la-stampa.2014v1.site", route.getWebPage());
            Assert.assertEquals("dam.assets.incoming.d", route.getDeskLevel());
            Assert.assertEquals("CRONACA", route.getSection());
            Assert.assertEquals("MAIL", route.getSource());
            Assert.assertEquals("98", route.getPrincipalId());
            Assert.assertEquals(-1, route.getMinWords());
            Assert.assertEquals(-1L, route.getImageMinSize());
            Assert.assertEquals(0, route.getSignatures().size());
        }
    }

    @Test
    public void test_with_contentTypes_default_and_field_mappings() {
        setupValue("mailimporter_enabled", "true");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "production");
        setupValue("article_aspect", "com.my.standard.article.6");
        setupValue("image_aspect", "com.my.standard.image.6");
        setupValue("mail_uri", "imaps://mailserver?username=pippo");
        setupJson("/conf6.json");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("production", config.getArticlePartition());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(1, mailUris.size());
        {
            final MailRouteConfig route = mailUris.get(0);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://mailserver?username=pippo", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("production", route.getArticlePartition());
            Assert.assertEquals("la-stampa.2014v1.site", route.getWebPage());
            Assert.assertEquals("dam.assets.incoming.d", route.getDeskLevel());
            Assert.assertEquals("CRONACA", route.getSection());
            Assert.assertEquals("MAIL", route.getSource());
            Assert.assertEquals("2020", route.getPrincipalId());
            Assert.assertEquals("com.my.standard.article.6", route.getArticleAspect());
            Assert.assertEquals("com.my.standard.image.6", route.getImageAspect());
            Assert.assertEquals(-1, route.getMinWords());
            Assert.assertEquals(-1L, route.getImageMinSize());

            final Map<String, Map<String, String>> defaults = route.getFieldsDefaults();
            Assert.assertNotNull(defaults);

            {
                final Map<String, String> map = defaults.get("atex.onecms.article");
                Assert.assertNotNull(map);
                Assert.assertEquals("parentDocument", map.get("contentType"));
                Assert.assertEquals("article", map.get("objectType"));
                Assert.assertEquals("p.DamWireArticle", map.get("inputTemplate"));
                Assert.assertEquals(3, map.size());
            }
            {
                final Map<String, String> map = defaults.get("atex.onecms.image");
                Assert.assertNotNull(map);
                Assert.assertEquals("parentDocument", map.get("contentType"));
                Assert.assertEquals("image", map.get("objectType"));
                Assert.assertEquals("p.DamWireImage", map.get("inputTemplate"));
                Assert.assertEquals(3, map.size());
            }
            {
                final Map<String, String> map = defaults.get("atex.dam.standard.Graphic");
                Assert.assertNotNull(map);
                Assert.assertEquals("parentDocument", map.get("contentType"));
                Assert.assertEquals("graphic", map.get("objectType"));
                Assert.assertEquals("p.DamWireGraphic", map.get("inputTemplate"));
                Assert.assertEquals(3, map.size());
            }

            final Map<String, Map<String, String>> mappings = route.getFieldsMappings();
            Assert.assertNotNull(mappings);

            {
                final Map<String, String> map = mappings.get("atex.onecms.article");
                Assert.assertNotNull(map);
                Assert.assertEquals("myname", map.get("name"));
                Assert.assertEquals(1, map.size());
            }
            {
                final Map<String, String> map = mappings.get("atex.onecms.image");
                Assert.assertNotNull(map);
                Assert.assertEquals("imageContentType", map.get("contentType"));
                Assert.assertEquals(1, map.size());
            }
            {
                final Map<String, String> map = mappings.get("atex.dam.standard.Graphic");
                Assert.assertNotNull(map);
                Assert.assertEquals(0, map.size());
            }
            {
                Assert.assertNull(mappings.get("atex.dam.standard.Page"));
            }

            Assert.assertEquals(2, route.getSignatures().size());
            {
                final Signature s = route.getSignatures().get(0);
                Assert.assertEquals(1, s.getBefore());
                Assert.assertEquals("Sign1", s.getRegex());
            }
            {
                final Signature s = route.getSignatures().get(1);
                Assert.assertEquals(2, s.getBefore());
                Assert.assertEquals("Sign2", s.getRegex());
            }

        }
    }

    @Test
    public void test_default_configuration() {
        setupValue("mailimporter_enabled", "true");
        setupValue("image_partition", "incoming");
        setupValue("article_partition", "production");
        setupValue("mail_uri", "imaps://mailserver?username=pippo");
        setupJson("/confDefault.json");

        final MailImporterConfig config = MailImporterConfigLoader.createConfig(mock);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals("incoming", config.getImagePartition());
        Assert.assertEquals("production", config.getArticlePartition());

        final List<MailRouteConfig> mailUris = config.getMailUris();
        Assert.assertEquals(1, mailUris.size());
        {
            final MailRouteConfig route = mailUris.get(0);
            Assert.assertNotNull(route);
            Assert.assertEquals("imaps://mailserver?username=pippo", route.getUri());
            Assert.assertTrue(route.isEnabled());
            Assert.assertEquals("incoming", route.getImagePartition());
            Assert.assertEquals("production", route.getArticlePartition());
            Assert.assertEquals(null, route.getWebPage());
            Assert.assertEquals(null, route.getDeskLevel());
            Assert.assertEquals(null, route.getSection());
            Assert.assertEquals(null, route.getSource());
            Assert.assertEquals("98", route.getPrincipalId());

            final Map<String, Map<String, String>> defaults = route.getFieldsDefaults();
            Assert.assertNotNull(defaults);
            Assert.assertEquals(0, defaults.size());

            final Map<String, Map<String, String>> mappings = route.getFieldsMappings();
            Assert.assertNotNull(mappings);
            Assert.assertEquals(0, mappings.size());
        }
    }

    private void setupJson(final String filename) {
        try {
            setupValue("json", ClassUtil.getResourceAsString(this.getClass(), filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupValue(final String name,
                            final String value) {
        Mockito.when(mock.getChildValue(Mockito.eq(name), Mockito.anyString()))
               .thenReturn(value);

    }

}