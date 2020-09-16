package com.atex.plugins.mailimporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.atex.onecms.content.ContentFileInfo;
import com.atex.onecms.content.ContentId;
import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.ContentResultBuilder;
import com.atex.onecms.content.ContentVersionId;
import com.atex.onecms.content.ContentWrite;
import com.atex.onecms.content.FilesAspectBean;
import com.atex.onecms.content.IdUtil;
import com.atex.onecms.content.InsertionInfoAspectBean;
import com.atex.onecms.content.RepositoryClient;
import com.atex.onecms.content.Status;
import com.atex.onecms.content.Subject;
import com.atex.onecms.content.files.FileInfo;
import com.atex.onecms.content.files.FileService;
import com.atex.onecms.content.metadata.MetadataInfo;
import com.atex.onecms.image.ImageInfoAspectBean;
import com.atex.plugins.baseline.ws.JettyRule;
import com.atex.plugins.baseline.ws.JettyWrapper;
import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.atex.plugins.mailimporter.util.MailImporterServiceLoaderUtil;
import com.atex.plugins.mailimporter.ws.MetadataServiceServlet;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.HttpFileServiceClient;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.cm.policymvc.PolicyModelDomain;
import com.polopoly.common.lang.ClassUtil;
import com.polopoly.metadata.Dimension;
import com.polopoly.metadata.Entity;
import com.polopoly.metadata.Metadata;
import com.polopoly.model.ModelType;
import com.polopoly.model.ModelTypeBean;

/**
 * ContentPublisherTest
 *
 * @author mnova
 */
public class ContentPublisherTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final JettyRule jettyWrapperRule = new JettyRule();

    @Mock
    Application application;

    @Mock
    ContentManager contentManager;

    @Mock
    PolicyCMServer cmServer;

    @Mock
    PolicyModelDomain modelDomain;

    @Mock
    FileService fileService;

    @Captor
    ArgumentCaptor<ContentWrite<?>> contentWriteCaptor;

    @Captor
    ArgumentCaptor<Subject> contentWriteSubjectCaptor;

    ContentPublisher publisher;

    @Before
    public void before() throws Exception {
        final CmClient mockCmClient = mockPreferredComponent(CmClient.class);
        Mockito.when(mockCmClient.getPolicyCMServer())
               .thenReturn(cmServer);
        Mockito.when(mockCmClient.getPolicyModelDomain())
               .thenReturn(modelDomain);

        final RepositoryClient mockRepoClient = mockPreferredComponent(RepositoryClient.class);
        Mockito.when(mockRepoClient.getContentManager())
               .thenReturn(contentManager);

        final HttpFileServiceClient mockHttpFileServiceClient = mockPreferredComponent(HttpFileServiceClient.class);
        Mockito.when(mockHttpFileServiceClient.getFileService())
               .thenReturn(fileService);

        final JettyWrapper jw = jettyWrapperRule.getJettyWrapper();
        final String servletPath = "/metadata/image/extract";
        jw.addServlet(
                new MetadataServiceServlet(HttpServletResponse.SC_OK)
                        .body(this.getClass(), "/metadata.json")
                        .contentType(MediaType.APPLICATION_JSON),
                servletPath
        );
        System.setProperty("image.metadata.service.url", jw.getURL(servletPath));

        publisher = new ContentPublisher();
        publisher.init(application);
    }

    @Test
    public void test_service_loader() {
        final MailPublisher mailPublisher = MailImporterServiceLoaderUtil.loadService(
                MailPublisher.class,
                ContentPublisher.class);
        Assert.assertNotNull(mailPublisher);
    }

    @Test
    public void test_publish_simple_article() throws Exception {
        MailImporterConfig config = new MailImporterConfig();
        config.setArticleNamePattern("Email_${from}_${subject}");
        config.setTaxonomyId("configTaxonomyId.d");
        config.setArticlePartition("production");

        publisher.setConfig(config);

        setupModelTypeName("com.my.articleBean", MyArticleBean.class);

        final ContentVersionId resultId = IdUtil.fromVersionedString("onecms:1234:5678");
        setupContentWrites(Collections.singletonMap("com.my.articleBean", resultId));

        final ContentVersionId deskLevelId = IdUtil.fromVersionedString("policy:2.101:1234");
        final ContentVersionId gongWebPageId = IdUtil.fromVersionedString("policy:2.201:5678");

        setupResolve("dam.desk.level", deskLevelId);
        setupResolve("gong.web.page", gongWebPageId);

        final MailRouteConfig routeConfig = new MailRouteConfig();
        routeConfig.setTaxonomyId("routeTaxonomy.d");
        routeConfig.setArticleAspect("com.my.articleBean");
        routeConfig.setArticlePartition("article-partition");
        routeConfig.setDeskLevel("dam.desk.level");
        routeConfig.setWebPage("gong.web.page");
        routeConfig.setSection("URGENT");
        routeConfig.setSource("MAIL");

        final MailBean mail = new MailBean();
        mail.setSubject("This is the subject");
        mail.setLead("This is the lead");
        mail.setBody("This is the body");
        mail.setFrom("mnova@atex.com");

        final ContentId contentId = publisher.publish(mail, routeConfig);
        Assert.assertNotNull(contentId);
        Assert.assertEquals(resultId.getContentId(), contentId);

        Mockito.verify(contentManager).create(contentWriteCaptor.capture(), contentWriteSubjectCaptor.capture());
        final ContentWrite<?> cw = contentWriteCaptor.getValue();
        Assert.assertEquals("com.my.articleBean", cw.getContentDataType());
        final MyArticleBean contentData = (MyArticleBean) cw.getContentData();
        Assert.assertNotNull(contentData);
        Assert.assertEquals("Email_mnova@atex.com_This is the subject", contentData.getName());
        Assert.assertEquals("This is the subject", contentData.getHeadline());
        Assert.assertEquals("This is the lead", contentData.getLead());
        Assert.assertEquals("This is the body", contentData.getBody());
        Assert.assertEquals("URGENT", contentData.getSection());
        Assert.assertEquals("MAIL", contentData.getSource());

        final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
        Assert.assertNotNull(metadataInfo);
        Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
        assertPartition(metadataInfo.getMetadata(), "article-partition");

        final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
        Assert.assertNotNull(insInfo);
        Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
        Assert.assertEquals(gongWebPageId.getContentId(), insInfo.getInsertParentId());

        final Subject subject = contentWriteSubjectCaptor.getValue();
        Assert.assertNotNull(subject);
        Assert.assertEquals("98", subject.getPrincipalId());
    }

    @Test
    public void test_publish_simple_article_without_webpage() throws Exception {
        MailImporterConfig config = new MailImporterConfig();
        config.setArticleNamePattern("Email_${from}_${subject}");
        config.setTaxonomyId("configTaxonomyId.d");
        config.setArticlePartition("production");

        publisher.setConfig(config);

        setupModelTypeName("com.my.articleBean", MyArticleBean.class);

        final ContentVersionId resultId = IdUtil.fromVersionedString("onecms:1234:5678");
        setupContentWrites(Collections.singletonMap("com.my.articleBean", resultId));

        final ContentVersionId deskLevelId = IdUtil.fromVersionedString("policy:2.101:1234");
        setupResolve("dam.desk.level", deskLevelId);

        final MailRouteConfig routeConfig = new MailRouteConfig();
        routeConfig.setTaxonomyId("routeTaxonomy.d");
        routeConfig.setArticleAspect("com.my.articleBean");
        routeConfig.setArticlePartition("article-partition");
        routeConfig.setDeskLevel("dam.desk.level");
        routeConfig.setPrincipalId("2020");

        final MailBean mail = new MailBean();
        mail.setSubject("This is the subject");
        mail.setLead("This is the lead");
        mail.setBody("This is the body");
        mail.setFrom("mnova@atex.com");

        final ContentId contentId = publisher.publish(mail, routeConfig);
        Assert.assertNotNull(contentId);
        Assert.assertEquals(resultId.getContentId(), contentId);

        Mockito.verify(contentManager).create(contentWriteCaptor.capture(), contentWriteSubjectCaptor.capture());
        final ContentWrite<?> cw = contentWriteCaptor.getValue();
        Assert.assertEquals("com.my.articleBean", cw.getContentDataType());
        final MyArticleBean contentData = (MyArticleBean) cw.getContentData();
        Assert.assertNotNull(contentData);
        Assert.assertEquals("Email_mnova@atex.com_This is the subject", contentData.getName());
        Assert.assertEquals("This is the subject", contentData.getHeadline());
        Assert.assertEquals("This is the lead", contentData.getLead());
        Assert.assertEquals("This is the body", contentData.getBody());

        final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
        Assert.assertNotNull(metadataInfo);
        Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
        assertPartition(metadataInfo.getMetadata(), "article-partition");

        final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
        Assert.assertNotNull(insInfo);
        Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
        Assert.assertNull(insInfo.getInsertParentId());

        final Subject subject = contentWriteSubjectCaptor.getValue();
        Assert.assertNotNull(subject);
        Assert.assertEquals("2020", subject.getPrincipalId());
    }

    @Test
    public void test_publish_with_fields_defaults() throws Exception {
        MailImporterConfig config = new MailImporterConfig();
        config.setArticleNamePattern("Email_${from}");
        config.setTaxonomyId("configTaxonomyId.d");
        config.setArticlePartition("production");

        publisher.setConfig(config);

        setupModelTypeName("com.my.articleBean.ext", ExtendedMyArticleBean.class);

        final ContentVersionId resultId = IdUtil.fromVersionedString("onecms:1234:5678");
        setupContentWrites(Collections.singletonMap("com.my.articleBean.ext", resultId));

        final ContentVersionId deskLevelId = IdUtil.fromVersionedString("policy:2.101:1234");
        final ContentVersionId gongWebPageId = IdUtil.fromVersionedString("policy:2.201:5678");

        setupResolve("dam.desk.level", deskLevelId);
        setupResolve("gong.web.page", gongWebPageId);

        final Map<String, String> fieldDefaults = new HashMap<>();
        fieldDefaults.put("section", "aSection");
        fieldDefaults.put("source", "aSource");
        fieldDefaults.put("inputTemplate", "p.DamWireArticle");
        fieldDefaults.put("objectType", "article");

        final MailRouteConfig routeConfig = new MailRouteConfig();
        routeConfig.setTaxonomyId("routeTaxonomy.d");
        routeConfig.setArticleAspect("com.my.articleBean.ext");
        routeConfig.setArticlePartition("my-article-partition");
        routeConfig.setDeskLevel("dam.desk.level");
        routeConfig.setWebPage("gong.web.page");
        routeConfig.setSection("URGENT");
        routeConfig.setSource("MAIL");
        routeConfig.setFieldsDefaults(Collections.singletonMap("com.my.articleBean.ext", fieldDefaults));

        final MailBean mail = new MailBean();
        mail.setSubject("This is the subject");
        mail.setLead("This is the lead");
        mail.setBody("This is the body");
        mail.setFrom("mnova@atex.com");

        final ContentId contentId = publisher.publish(mail, routeConfig);
        Assert.assertNotNull(contentId);
        Assert.assertEquals(resultId.getContentId(), contentId);

        Mockito.verify(contentManager).create(contentWriteCaptor.capture(), contentWriteSubjectCaptor.capture());
        final ContentWrite<?> cw = contentWriteCaptor.getValue();
        Assert.assertEquals("com.my.articleBean.ext", cw.getContentDataType());
        final ExtendedMyArticleBean contentData = (ExtendedMyArticleBean) cw.getContentData();
        Assert.assertNotNull(contentData);
        Assert.assertEquals("Email_mnova@atex.com", contentData.getName());
        Assert.assertEquals("This is the subject", contentData.getHeadline());
        Assert.assertEquals("This is the lead", contentData.getLead());
        Assert.assertEquals("This is the body", contentData.getBody());
        Assert.assertEquals("p.DamWireArticle", contentData.getInputTemplate());
        Assert.assertEquals("article", contentData.getObjectType());
        Assert.assertEquals("aSection", contentData.getSection());
        Assert.assertEquals("aSource", contentData.getSource());

        final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
        Assert.assertNotNull(metadataInfo);
        Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
        assertPartition(metadataInfo.getMetadata(), "my-article-partition");

        final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
        Assert.assertNotNull(insInfo);
        Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
        Assert.assertEquals(gongWebPageId.getContentId(), insInfo.getInsertParentId());

        final Subject subject = contentWriteSubjectCaptor.getValue();
        Assert.assertNotNull(subject);
        Assert.assertEquals("98", subject.getPrincipalId());
    }

    @Test
    public void test_publish_with_fields_mappings() throws Exception {
        MailImporterConfig config = new MailImporterConfig();
        config.setArticleNamePattern(null);
        config.setTaxonomyId("configTaxonomyId.d");
        config.setArticlePartition("production");

        publisher.setConfig(config);

        setupModelTypeName("com.my.articleBean.ext", ExtendedMyArticleBean.class);

        final ContentVersionId resultId = IdUtil.fromVersionedString("onecms:01234:56789");
        setupContentWrites(Collections.singletonMap("com.my.articleBean.ext", resultId));

        final ContentVersionId deskLevelId = IdUtil.fromVersionedString("policy:2.301:1234");
        final ContentVersionId gongWebPageId = IdUtil.fromVersionedString("policy:2.401:5678");

        setupResolve("dam.desk.level", deskLevelId);
        setupResolve("gong.web.page", gongWebPageId);

        final Map<String, String> fieldsDefaults = new HashMap<>();
        fieldsDefaults.put("srcInputTemplate", "p.DamWireArticle");
        fieldsDefaults.put("srcObjectType", "article");

        final Map<String, String> fieldsMappings = new HashMap<>();
        fieldsMappings.put("srcInputTemplate", "inputTemplate");
        fieldsMappings.put("srcObjectType", "objectType");
        fieldsMappings.put("headline", "name");

        final MailRouteConfig routeConfig = new MailRouteConfig();
        routeConfig.setTaxonomyId("routeTaxonomy.d");
        routeConfig.setArticleAspect("com.my.articleBean.ext");
        routeConfig.setArticlePartition("my-article-partition");
        routeConfig.setDeskLevel("dam.desk.level");
        routeConfig.setWebPage("gong.web.page");
        routeConfig.setSection("URGENT");
        routeConfig.setSource("MAIL");
        routeConfig.setFieldsDefaults(Collections.singletonMap("com.my.articleBean.ext", fieldsDefaults));
        routeConfig.setFieldsMappings(Collections.singletonMap("com.my.articleBean.ext", fieldsMappings));

        final MailBean mail = new MailBean();
        mail.setSubject("This is the subject");
        mail.setLead("This is the lead");
        mail.setBody("This is the body");
        mail.setFrom("mnova@atex.com");

        final ContentId contentId = publisher.publish(mail, routeConfig);
        Assert.assertNotNull(contentId);
        Assert.assertEquals(resultId.getContentId(), contentId);

        Mockito.verify(contentManager).create(contentWriteCaptor.capture(), contentWriteSubjectCaptor.capture());
        final ContentWrite<?> cw = contentWriteCaptor.getValue();
        Assert.assertEquals("com.my.articleBean.ext", cw.getContentDataType());
        final ExtendedMyArticleBean contentData = (ExtendedMyArticleBean) cw.getContentData();
        Assert.assertNotNull(contentData);
        Assert.assertEquals("This is the subject", contentData.getName());
        Assert.assertEquals(null, contentData.getHeadline());
        Assert.assertEquals("This is the lead", contentData.getLead());
        Assert.assertEquals("This is the body", contentData.getBody());
        Assert.assertEquals("p.DamWireArticle", contentData.getInputTemplate());
        Assert.assertEquals("article", contentData.getObjectType());
        Assert.assertEquals("URGENT", contentData.getSection());
        Assert.assertEquals("MAIL", contentData.getSource());

        final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
        Assert.assertNotNull(metadataInfo);
        Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
        assertPartition(metadataInfo.getMetadata(), "my-article-partition");

        final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
        Assert.assertNotNull(insInfo);
        Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
        Assert.assertEquals(gongWebPageId.getContentId(), insInfo.getInsertParentId());

        final Subject subject = contentWriteSubjectCaptor.getValue();
        Assert.assertNotNull(subject);
        Assert.assertEquals("98", subject.getPrincipalId());
    }

    @Test
    public void test_publish_simple_article_with_image() throws Exception {
        MailImporterConfig config = new MailImporterConfig();
        config.setTaxonomyId("configTaxonomyId.d");
        config.setArticleNamePattern("Email_${from}_${subject}");
        config.setArticlePartition("production");
        config.setAcceptedImageExtensions(Collections.singletonList("jpg"));
        config.setImagePartition("incoming");
        config.setAttachmentNamePattern("Attachment_${from}_${filename}");

        publisher.setConfig(config);

        setupModelTypeName("com.my.articleBean", MyArticleBean.class);
        setupModelTypeName("com.my.imageBean", MyImageBean.class);

        final ContentVersionId articleId = IdUtil.fromVersionedString("onecms:1234:5678");
        final ContentVersionId imageId = IdUtil.fromVersionedString("onecms:abcd:efgh");
        final Map<String, ContentVersionId> contentWrites = new HashMap<>();
        contentWrites.put("com.my.articleBean", articleId);
        contentWrites.put("com.my.imageBean", imageId);
        setupContentWrites(contentWrites);

        final long now = System.currentTimeMillis();
        final FileInfo fileInfo = new FileInfo(
                "content://myimage.jpg",
                "image/jpg",
                "myimage.jpg",
                0,
                null,
                now,
                now,
                now);
        final Supplier<byte[]> imgSupplier = setupFileService("myimage.jpg", fileInfo);

        final ContentVersionId deskLevelId = IdUtil.fromVersionedString("policy:2.101:1234");
        final ContentVersionId gongWebPageId = IdUtil.fromVersionedString("policy:2.201:5678");

        setupResolve("dam.desk.level", deskLevelId);
        setupResolve("gong.web.page", gongWebPageId);

        final MailRouteConfig routeConfig = new MailRouteConfig();
        routeConfig.setTaxonomyId("routeTaxonomy.d");
        routeConfig.setArticleAspect("com.my.articleBean");
        routeConfig.setArticlePartition("article-partition");
        routeConfig.setImageAspect("com.my.imageBean");
        routeConfig.setImagePartition("image-partition");
        routeConfig.setDeskLevel("dam.desk.level");
        routeConfig.setWebPage("gong.web.page");
        routeConfig.setSection("URGENT");
        routeConfig.setSource("MAIL");

        final MailBean mail = new MailBean();
        mail.setSubject("This is the subject");
        mail.setLead("This is the lead");
        mail.setBody("This is the body");
        mail.setFrom("mnova@atex.com");

        final byte[] imgData;
        try (final InputStream is = ClassUtil.getResourceAsStream(this.getClass(), "/image.jpg")) {
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                IOUtils.copy(is, baos);
                baos.close();
                imgData = baos.toByteArray();
                mail.setAttachments(Collections.singletonMap("myimage.jpg", imgData));
            }
        }

        final ContentId contentId = publisher.publish(mail, routeConfig);
        Assert.assertNotNull(contentId);
        Assert.assertEquals(articleId.getContentId(), contentId);

        Mockito.verify(contentManager, Mockito.times(2))
               .create(contentWriteCaptor.capture(), contentWriteSubjectCaptor.capture());
        final List<ContentWrite<?>> cwValues = contentWriteCaptor.getAllValues();
        Assert.assertNotNull(cwValues);
        Assert.assertEquals(2, cwValues.size());

        {
            final ContentWrite<?> cw = cwValues.get(0);
            Assert.assertEquals("com.my.imageBean", cw.getContentDataType());

            final MyImageBean contentData = (MyImageBean) cw.getContentData();
            Assert.assertNotNull(contentData);
            Assert.assertEquals("Attachment_mnova@atex.com_myimage.jpg", contentData.getName());
            Assert.assertEquals("Atex", contentData.getByline());
            Assert.assertEquals("OLYMPUS DIGITAL CAMERA", contentData.getDescription());
            Assert.assertEquals("MAIL", contentData.getSource());
            Assert.assertEquals("URGENT", contentData.getSection());
            // the image exif value contains some values which are incorrect since we resized the
            // image before importing in the project but all the code paths assumes the exif values
            // contains the correct values.
            Assert.assertEquals(600, contentData.getWidth());
            Assert.assertEquals(450, contentData.getHeight());

            final FilesAspectBean files = cw.getAspect(FilesAspectBean.ASPECT_NAME, FilesAspectBean.class);
            Assert.assertNotNull(files);
            Assert.assertNotNull(files.getFiles());
            final ContentFileInfo contentFileInfo = files.getFiles().get("myimage.jpg");
            Assert.assertNotNull(contentFileInfo);
            Assert.assertEquals("content://myimage.jpg", contentFileInfo.getFileUri());
            Assert.assertEquals("myimage.jpg", contentFileInfo.getFilePath());

            final ImageInfoAspectBean imageInfo = cw.getAspect(ImageInfoAspectBean.ASPECT_NAME, ImageInfoAspectBean.class);
            Assert.assertNotNull(imageInfo);
            Assert.assertEquals("myimage.jpg", imageInfo.getFilePath());
            Assert.assertEquals(600, imageInfo.getWidth());
            Assert.assertEquals(450, imageInfo.getHeight());

            final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
            Assert.assertNotNull(metadataInfo);
            Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
            assertPartition(metadataInfo.getMetadata(), "image-partition");

            final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
            Assert.assertNotNull(insInfo);
            Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
            Assert.assertEquals(gongWebPageId.getContentId(), insInfo.getInsertParentId());
        }

        {
            final ContentWrite<?> cw = cwValues.get(1);
            Assert.assertEquals("com.my.articleBean", cw.getContentDataType());

            final MyArticleBean contentData = (MyArticleBean) cw.getContentData();
            Assert.assertNotNull(contentData);
            Assert.assertEquals("Email_mnova@atex.com_This is the subject", contentData.getName());
            Assert.assertEquals("This is the subject", contentData.getHeadline());
            Assert.assertEquals("This is the lead", contentData.getLead());
            Assert.assertEquals("This is the body", contentData.getBody());
            Assert.assertEquals("URGENT", contentData.getSection());
            Assert.assertEquals("MAIL", contentData.getSource());

            final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
            Assert.assertNotNull(metadataInfo);
            Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
            assertPartition(metadataInfo.getMetadata(), "article-partition");

            final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
            Assert.assertNotNull(insInfo);
            Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
            Assert.assertEquals(gongWebPageId.getContentId(), insInfo.getInsertParentId());
        }

        contentWriteSubjectCaptor.getAllValues()
                                 .forEach(subject -> {
                                     Assert.assertNotNull(subject);
                                     Assert.assertEquals("98", subject.getPrincipalId());
                                 });

        Assert.assertArrayEquals(imgData, imgSupplier.get());
    }

    @Test
    public void test_publish_image_with_fields_defaults() throws Exception {
        MailImporterConfig config = new MailImporterConfig();
        config.setTaxonomyId("configTaxonomyId.d");
        config.setArticleNamePattern("Email_${from}_${subject}");
        config.setArticlePartition("production");
        config.setAcceptedImageExtensions(Collections.singletonList("jpg"));
        config.setImagePartition("incoming");
        config.setAttachmentNamePattern("Attachment_${from}_${filename}");

        publisher.setConfig(config);

        setupModelTypeName("com.my.articleBean.ext", ExtendedMyArticleBean.class);
        setupModelTypeName("com.my.imageBean.ext", ExtendedMyImageBean.class);

        final ContentVersionId articleId = IdUtil.fromVersionedString("onecms:1234:5678");
        final ContentVersionId imageId = IdUtil.fromVersionedString("onecms:abcd:efgh");
        final Map<String, ContentVersionId> contentWrites = new HashMap<>();
        contentWrites.put("com.my.articleBean.ext", articleId);
        contentWrites.put("com.my.imageBean.ext", imageId);
        setupContentWrites(contentWrites);

        final long now = System.currentTimeMillis();
        final FileInfo fileInfo = new FileInfo(
                "content://myimage.jpg",
                "image/jpg",
                "myimage.jpg",
                0,
                null,
                now,
                now,
                now);
        final Supplier<byte[]> imgSupplier = setupFileService("myimage.jpg", fileInfo);

        final ContentVersionId deskLevelId = IdUtil.fromVersionedString("policy:2.101:1234");
        final ContentVersionId gongWebPageId = IdUtil.fromVersionedString("policy:2.201:5678");

        setupResolve("dam.desk.level", deskLevelId);
        setupResolve("gong.web.page", gongWebPageId);

        final Map<String, Map<String, String>> fieldsDefaults = new HashMap<>();
        {
            final Map<String, String> imageDefaults = new HashMap<>();
            imageDefaults.put("section", "aSection");
            imageDefaults.put("source", "anImgSource");
            imageDefaults.put("inputTemplate", "p.DamWireImage");
            imageDefaults.put("objectType", "image");
            fieldsDefaults.put("com.my.imageBean.ext", imageDefaults);
        }
        {
            final Map<String, String> articleDefaults = new HashMap<>();
            articleDefaults.put("section", "anotherSection");
            articleDefaults.put("source", "aSource");
            articleDefaults.put("inputTemplate", "p.DamWireArticle");
            articleDefaults.put("objectType", "article");
            fieldsDefaults.put("com.my.articleBean.ext", articleDefaults);
        }

        final MailRouteConfig routeConfig = new MailRouteConfig();
        routeConfig.setTaxonomyId("routeTaxonomy.d");
        routeConfig.setArticleAspect("com.my.articleBean.ext");
        routeConfig.setImageAspect("com.my.imageBean.ext");
        routeConfig.setArticlePartition("article-partition");
        routeConfig.setImagePartition("image-partition");
        routeConfig.setDeskLevel("dam.desk.level");
        routeConfig.setWebPage("gong.web.page");
        routeConfig.setSection("URGENT");
        routeConfig.setSource("MAIL");
        routeConfig.setFieldsDefaults(fieldsDefaults);
        routeConfig.setPrincipalId("1024");

        final MailBean mail = new MailBean();
        mail.setSubject("This is the subject");
        mail.setLead("This is the lead");
        mail.setBody("This is the body");
        mail.setFrom("mnova@atex.com");

        final byte[] imgData;

        try (final InputStream is = ClassUtil.getResourceAsStream(this.getClass(), "/image.jpg")) {
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                IOUtils.copy(is, baos);
                baos.close();
                imgData = baos.toByteArray();
                mail.setAttachments(Collections.singletonMap("myimage.jpg", imgData));
            }
        }

        final ContentId contentId = publisher.publish(mail, routeConfig);
        Assert.assertNotNull(contentId);
        Assert.assertEquals(articleId.getContentId(), contentId);

        Mockito.verify(contentManager, Mockito.times(2))
               .create(contentWriteCaptor.capture(), contentWriteSubjectCaptor.capture());
        final List<ContentWrite<?>> cwValues = contentWriteCaptor.getAllValues();
        Assert.assertNotNull(cwValues);
        Assert.assertEquals(2, cwValues.size());

        {
            final ContentWrite<?> cw = cwValues.get(0);
            Assert.assertEquals("com.my.imageBean.ext", cw.getContentDataType());

            final ExtendedMyImageBean contentData = (ExtendedMyImageBean) cw.getContentData();
            Assert.assertNotNull(contentData);
            Assert.assertEquals("Attachment_mnova@atex.com_myimage.jpg", contentData.getName());
            Assert.assertEquals("Atex", contentData.getByline());
            Assert.assertEquals("OLYMPUS DIGITAL CAMERA", contentData.getDescription());
            Assert.assertEquals("anImgSource", contentData.getSource());
            Assert.assertEquals("aSection", contentData.getSection());
            Assert.assertEquals("p.DamWireImage", contentData.getInputTemplate());
            Assert.assertEquals("image", contentData.getObjectType());
            // the image exif value contains some values which are incorrect since we resized the
            // image before importing in the project but all the code paths assumes the exif values
            // contains the correct values.
            Assert.assertEquals(600, contentData.getWidth());
            Assert.assertEquals(450, contentData.getHeight());

            final FilesAspectBean files = cw.getAspect(FilesAspectBean.ASPECT_NAME, FilesAspectBean.class);
            Assert.assertNotNull(files);
            Assert.assertNotNull(files.getFiles());
            final ContentFileInfo contentFileInfo = files.getFiles().get("myimage.jpg");
            Assert.assertNotNull(contentFileInfo);
            Assert.assertEquals("content://myimage.jpg", contentFileInfo.getFileUri());
            Assert.assertEquals("myimage.jpg", contentFileInfo.getFilePath());

            final ImageInfoAspectBean imageInfo = cw.getAspect(ImageInfoAspectBean.ASPECT_NAME, ImageInfoAspectBean.class);
            Assert.assertNotNull(imageInfo);
            Assert.assertEquals("myimage.jpg", imageInfo.getFilePath());
            Assert.assertEquals(600, imageInfo.getWidth());
            Assert.assertEquals(450, imageInfo.getHeight());

            final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
            Assert.assertNotNull(metadataInfo);
            Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
            assertPartition(metadataInfo.getMetadata(), "image-partition");

            final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
            Assert.assertNotNull(insInfo);
            Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
            Assert.assertEquals(gongWebPageId.getContentId(), insInfo.getInsertParentId());
        }

        {
            final ContentWrite<?> cw = cwValues.get(1);
            Assert.assertEquals("com.my.articleBean.ext", cw.getContentDataType());

            final ExtendedMyArticleBean contentData = (ExtendedMyArticleBean) cw.getContentData();
            Assert.assertNotNull(contentData);
            Assert.assertEquals("Email_mnova@atex.com_This is the subject", contentData.getName());
            Assert.assertEquals("This is the subject", contentData.getHeadline());
            Assert.assertEquals("This is the lead", contentData.getLead());
            Assert.assertEquals("This is the body", contentData.getBody());
            Assert.assertEquals("anotherSection", contentData.getSection());
            Assert.assertEquals("aSource", contentData.getSource());
            Assert.assertEquals("article", contentData.getObjectType());
            Assert.assertEquals("p.DamWireArticle", contentData.getInputTemplate());

            final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
            Assert.assertNotNull(metadataInfo);
            Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
            assertPartition(metadataInfo.getMetadata(), "article-partition");

            final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
            Assert.assertNotNull(insInfo);
            Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
            Assert.assertEquals(gongWebPageId.getContentId(), insInfo.getInsertParentId());
        }

        contentWriteSubjectCaptor.getAllValues()
                                 .forEach(subject -> {
                                     Assert.assertNotNull(subject);
                                     Assert.assertEquals("1024", subject.getPrincipalId());
                                 });

        Assert.assertArrayEquals(imgData, imgSupplier.get());
    }

    @Test
    public void test_publish_simple_article_with_unrecognized_image() throws Exception {
        final JettyWrapper jw = jettyWrapperRule.getJettyWrapper();
        final String servletPath = "/metadata/image/extract";
        jw.addServlet(
                new MetadataServiceServlet(HttpServletResponse.SC_OK)
                        .body(this.getClass(), "/metadataExifError.json")
                        .contentType(MediaType.APPLICATION_JSON),
                servletPath
        );
        MailImporterConfig config = new MailImporterConfig();
        config.setTaxonomyId("configTaxonomyId.d");
        config.setArticleNamePattern("Email_${from}_${subject}");
        config.setArticlePartition("production");
        config.setAcceptedImageExtensions(Collections.singletonList("jpg"));
        config.setImagePartition("incoming");
        config.setAttachmentNamePattern("Attachment_${from}_${filename}");

        publisher.setConfig(config);

        setupModelTypeName("com.my.articleBean", MyArticleBean.class);
        setupModelTypeName("com.my.imageBean", MyImageBean.class);

        final ContentVersionId articleId = IdUtil.fromVersionedString("onecms:1234:5678");
        final ContentVersionId imageId = IdUtil.fromVersionedString("onecms:abcd:efgh");
        final Map<String, ContentVersionId> contentWrites = new HashMap<>();
        contentWrites.put("com.my.articleBean", articleId);
        contentWrites.put("com.my.imageBean", imageId);
        setupContentWrites(contentWrites);

        final long now = System.currentTimeMillis();
        final FileInfo fileInfo = new FileInfo(
                "content://myimage.jpg",
                "image/jpg",
                "myimage.jpg",
                0,
                null,
                now,
                now,
                now);
        final Supplier<byte[]> imgSupplier = setupFileService("myimage.jpg", fileInfo);

        final ContentVersionId deskLevelId = IdUtil.fromVersionedString("policy:2.101:1234");
        final ContentVersionId gongWebPageId = IdUtil.fromVersionedString("policy:2.201:5678");

        setupResolve("dam.desk.level", deskLevelId);
        setupResolve("gong.web.page", gongWebPageId);

        final MailRouteConfig routeConfig = new MailRouteConfig();
        routeConfig.setTaxonomyId("routeTaxonomy.d");
        routeConfig.setArticleAspect("com.my.articleBean");
        routeConfig.setArticlePartition("article-partition");
        routeConfig.setImageAspect("com.my.imageBean");
        routeConfig.setImagePartition("image-partition");
        routeConfig.setDeskLevel("dam.desk.level");
        routeConfig.setWebPage("gong.web.page");
        routeConfig.setSection("URGENT");
        routeConfig.setSource("MAIL");

        final MailBean mail = new MailBean();
        mail.setSubject("This is the subject");
        mail.setLead("This is the lead");
        mail.setBody("This is the body");
        mail.setFrom("mnova@atex.com");

        final byte[] imgData;

        try (final InputStream is = ClassUtil.getResourceAsStream(this.getClass(), "/image.jpg")) {
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                IOUtils.copy(is, baos);
                baos.close();
                imgData = baos.toByteArray();
                mail.setAttachments(Collections.singletonMap("myimage.jpg", imgData));
            }
        }

        final ContentId contentId = publisher.publish(mail, routeConfig);
        Assert.assertNotNull(contentId);
        Assert.assertEquals(articleId.getContentId(), contentId);

        Mockito.verify(contentManager, Mockito.times(2))
               .create(contentWriteCaptor.capture(), contentWriteSubjectCaptor.capture());
        final List<ContentWrite<?>> cwValues = contentWriteCaptor.getAllValues();
        Assert.assertNotNull(cwValues);
        Assert.assertEquals(2, cwValues.size());

        {
            final ContentWrite<?> cw = cwValues.get(0);
            Assert.assertEquals("com.my.imageBean", cw.getContentDataType());

            final MyImageBean contentData = (MyImageBean) cw.getContentData();
            Assert.assertNotNull(contentData);
            Assert.assertEquals("Attachment_mnova@atex.com_myimage.jpg", contentData.getName());
            Assert.assertEquals("Atex", contentData.getByline());
            Assert.assertEquals("OLYMPUS DIGITAL CAMERA", contentData.getDescription());
            Assert.assertEquals("MAIL", contentData.getSource());
            Assert.assertEquals("URGENT", contentData.getSection());
            // the image exif value contains some values which are incorrect since we resized the
            // image before importing in the project but all the code paths assumes the exif values
            // contains the correct values.
            Assert.assertEquals(0, contentData.getWidth());
            Assert.assertEquals(0, contentData.getHeight());

            final FilesAspectBean files = cw.getAspect(FilesAspectBean.ASPECT_NAME, FilesAspectBean.class);
            Assert.assertNotNull(files);
            Assert.assertNotNull(files.getFiles());
            final ContentFileInfo contentFileInfo = files.getFiles().get("myimage.jpg");
            Assert.assertNotNull(contentFileInfo);
            Assert.assertEquals("content://myimage.jpg", contentFileInfo.getFileUri());
            Assert.assertEquals("myimage.jpg", contentFileInfo.getFilePath());

            final ImageInfoAspectBean imageInfo = cw.getAspect(ImageInfoAspectBean.ASPECT_NAME, ImageInfoAspectBean.class);
            Assert.assertNotNull(imageInfo);
            Assert.assertEquals("myimage.jpg", imageInfo.getFilePath());
            Assert.assertEquals(0, imageInfo.getWidth());
            Assert.assertEquals(0, imageInfo.getHeight());

            final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
            Assert.assertNotNull(metadataInfo);
            Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
            assertPartition(metadataInfo.getMetadata(), "image-partition");

            final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
            Assert.assertNotNull(insInfo);
            Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
            Assert.assertEquals(gongWebPageId.getContentId(), insInfo.getInsertParentId());
        }

        {
            final ContentWrite<?> cw = cwValues.get(1);
            Assert.assertEquals("com.my.articleBean", cw.getContentDataType());

            final MyArticleBean contentData = (MyArticleBean) cw.getContentData();
            Assert.assertNotNull(contentData);
            Assert.assertEquals("Email_mnova@atex.com_This is the subject", contentData.getName());
            Assert.assertEquals("This is the subject", contentData.getHeadline());
            Assert.assertEquals("This is the lead", contentData.getLead());
            Assert.assertEquals("This is the body", contentData.getBody());
            Assert.assertEquals("URGENT", contentData.getSection());
            Assert.assertEquals("MAIL", contentData.getSource());

            final MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
            Assert.assertNotNull(metadataInfo);
            Assert.assertEquals(asSet("routeTaxonomy.d"), metadataInfo.getTaxonomyIds());
            assertPartition(metadataInfo.getMetadata(), "article-partition");

            final InsertionInfoAspectBean insInfo = cw.getAspect(InsertionInfoAspectBean.ASPECT_NAME, InsertionInfoAspectBean.class);
            Assert.assertNotNull(insInfo);
            Assert.assertEquals(deskLevelId.getContentId(), insInfo.getSecurityParentId());
            Assert.assertEquals(gongWebPageId.getContentId(), insInfo.getInsertParentId());
        }

        contentWriteSubjectCaptor.getAllValues()
                                 .forEach(subject -> {
                                     Assert.assertNotNull(subject);
                                     Assert.assertEquals("98", subject.getPrincipalId());
                                 });

        Assert.assertArrayEquals(imgData, imgSupplier.get());
    }

    private void assertPartition(final Metadata metadata, final String expectedPartition) {
        if (expectedPartition == null) {
            Assert.assertNull(metadata);
        } else {
            Assert.assertNotNull(metadata);
            final Dimension dim = metadata.getDimensionById("dimension.partition");
            Assert.assertNotNull(dim);
            Assert.assertNotNull(dim.getEntities());
            Assert.assertEquals(1, dim.getEntities().size());
            final Entity entity = dim.getEntities().get(0);
            Assert.assertEquals(expectedPartition, entity.getId());
            Assert.assertEquals(expectedPartition, entity.getName());
        }
    }

    private Set<String> asSet(final String...values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private Supplier<byte[]> setupFileService(final String imageName,
                                              final FileInfo fileInfo) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Mockito.when(fileService.uploadFile(
                Mockito.eq("tmp"),
                Mockito.any(),
                Mockito.eq(imageName),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()))
               .thenAnswer(invocation -> {
                   final InputStream is = invocation.getArgumentAt(3, InputStream.class);
                   if (is == null) {
                       throw new AssertionError("missing input stream");
                   }
                   IOUtils.copy(is, out);
                   return fileInfo;
               });
        return out::toByteArray;
    }

    private void setupResolve(final String externalId,
                              final ContentVersionId id) {
        Mockito.when(contentManager.resolve(Mockito.eq(externalId), Mockito.any()))
               .thenReturn(id);
    }

    private void setupContentWrites(final Map<String, ContentVersionId> aspectWrites) {
        Mockito.when(contentManager.create(Mockito.any(), Mockito.any()))
               .thenAnswer(invocation -> {
                   final ContentWrite<?> cw = invocation.getArgumentAt(0, ContentWrite.class);
                   final String contentType = cw.getContentDataType();
                   final ContentVersionId id = aspectWrites.get(contentType);
                   if (id != null) {
                       return new ContentResultBuilder<>()
                               .id(id)
                               .type(cw.getContentDataType())
                               .mainAspectData(cw.getContentData())
                               .aspects(cw.getAspects())
                               .status(Status.OK)
                               .build();
                   }
                   throw new AssertionError("Unexpected content type " + contentType);
               });
    }

    private void setupModelTypeName(final String name,
                                    final Class<?> klass) {
        ModelType modelType = new ModelTypeBean(modelDomain, klass);
        Mockito.when(modelDomain.getModelType(Mockito.eq(name)))
               .thenReturn(modelType);
    }

    private <T> T mockPreferredComponent(Class<T> classToMock) throws IllegalApplicationStateException {
        final T mockComponent = Mockito.mock(classToMock);
        Mockito.when(application.getPreferredApplicationComponent(Mockito.eq(classToMock)))
               .thenReturn(mockComponent);
        return mockComponent;
    }

    public static class MyArticleBean {
        private String name;
        private String headline;
        private String lead;
        private String body;
        private String source;
        private String section;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getLead() {
            return lead;
        }

        public void setLead(final String lead) {
            this.lead = lead;
        }

        public String getHeadline() {
            return headline;
        }

        public void setHeadline(final String headline) {
            this.headline = headline;
        }

        public String getBody() {
            return body;
        }

        public void setBody(final String body) {
            this.body = body;
        }

        public String getSource() {
            return source;
        }

        public void setSource(final String source) {
            this.source = source;
        }

        public String getSection() {
            return section;
        }

        public void setSection(final String section) {
            this.section = section;
        }
    }

    public static class ExtendedMyArticleBean extends MyArticleBean {
        private String inputTemplate;
        private String objectType;

        public String getInputTemplate() {
            return inputTemplate;
        }

        public void setInputTemplate(final String inputTemplate) {
            this.inputTemplate = inputTemplate;
        }

        public String getObjectType() {
            return objectType;
        }

        public void setObjectType(final String objectType) {
            this.objectType = objectType;
        }
    }

    public static class MyImageBean {
        private String name;
        private String byline;
        private String source;
        private String section;
        private String description;
        private int width;
        private int height;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getByline() {
            return byline;
        }

        public void setByline(final String byline) {
            this.byline = byline;
        }

        public String getSource() {
            return source;
        }

        public void setSource(final String source) {
            this.source = source;
        }

        public String getSection() {
            return section;
        }

        public void setSection(final String section) {
            this.section = section;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(final int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(final int height) {
            this.height = height;
        }
    }

    public static class ExtendedMyImageBean extends MyImageBean {
        private String inputTemplate;
        private String objectType;

        public String getInputTemplate() {
            return inputTemplate;
        }

        public void setInputTemplate(final String inputTemplate) {
            this.inputTemplate = inputTemplate;
        }

        public String getObjectType() {
            return objectType;
        }

        public void setObjectType(final String objectType) {
            this.objectType = objectType;
        }
    }

}