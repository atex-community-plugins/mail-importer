package com.atex.plugins.mailimporter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atex.onecms.content.ContentId;
import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.ContentResult;
import com.atex.onecms.content.ContentWrite;
import com.atex.onecms.content.ContentWriteBuilder;
import com.atex.onecms.content.FilesAspectBean;
import com.atex.onecms.content.IdUtil;
import com.atex.onecms.content.InsertionInfoAspectBean;
import com.atex.onecms.content.RepositoryClient;
import com.atex.onecms.content.Subject;
import com.atex.onecms.content.files.FileInfo;
import com.atex.onecms.content.files.FileService;
import com.atex.onecms.content.metadata.MetadataInfo;
import com.atex.onecms.image.ImageInfoAspectBean;
import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.HttpFileServiceClient;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.common.lang.StringUtil;
import com.polopoly.metadata.Dimension;
import com.polopoly.metadata.Entity;
import com.polopoly.metadata.Metadata;
import com.polopoly.model.ModelDomain;

/**
 * Utility class used to publish a parsed {@link MailBean}
 * as content in Polopoly. The following applies to content
 * created by this Polopoly Mail Publishing integration:
 *
 * <ul>
 *   <li>All articles will be instances of <code>standard.Article</code>.</li>
 *   <li>All images will be instances of <code>standard.Image</code>.</li>
 *   <li>All content will have <code>PolopolyPost.d</code> as security parent.</li>
 * </ul>
 */
public class ContentPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPublisher.class);

    private static final String SCHEME_TMP = "tmp";
    private static final String DIMENSION_PARTITION = "dimension.partition";

    static final ThreadLocal<MailImporterConfig> IMPORTER_CONFIG = new ThreadLocal<>();

    private FileService fileService = null;
    private ContentManager contentManager = null;
    private MailProcessorUtils mailProcessorUtils = null;
    private PolicyCMServer cmServer = null;
    private ModelDomain modelDomain;

    private final Application application;

    public ContentPublisher(final Application application) {
        this.application = application;
        if (application == null) {
            LOG.error("Failed to get Application");
        }
    }

    private Application getApplication() {
        return application;
    }

    public MailImporterConfig getConfig() {
        final MailImporterConfig config = IMPORTER_CONFIG.get();
        if (config != null) {
            return config;
        }
        try {
            return MailImporterConfigLoader.createConfig(cmServer);
        } catch (CMException e) {
            LOG.error("unable to load mail importer config", e);
            return null;
        }
    }

    public void setConfig(final MailImporterConfig config) {
        IMPORTER_CONFIG.set(config);
    }

    public ContentId publish(final MailBean mail,
                             final MailRouteConfig routeConfig) throws Exception {

        // Please note that the mapping between data and Polopoly types we have to perform here will be
        // very much simplified in future versions of Polopoly, where the Data-API will have support for
        // write operations. It will then be possible to manage data mapping and conversions centralized
        // for the entire project.
        //
        // http://support.polopoly.com/confluence/display/Polopoly > Data-API.

        if (routeConfig == null) {
            throw new Exception("Missing routeConfig, (did you forget to add header X-ROUTE-CONFIG?)");
        }

        try {
            final MailImporterConfig config = getConfig();
            IMPORTER_CONFIG.set(config);

            final Object articleBean = createArticle(config, routeConfig, mail);
            final ContentResult<Object> cr = writeArticleBean(mailProcessorUtils, routeConfig, articleBean);
            return cr.getContentId().getContentId();
        } catch (CMException e) {
            throw new RuntimeException("Failed to publish contents!", e);
        } finally {
            IMPORTER_CONFIG.set(null);
        }
    }

    public void init() {
        final Application application = getApplication();
        final CmClient cmclient = getCmClient(application);

        if (contentManager == null) {
            contentManager = getContentManager(application);
        }

        if (modelDomain == null) {
            modelDomain = cmclient.getPolicyModelDomain();
        }

        if (cmServer == null) {
            cmServer = cmclient.getPolicyCMServer();
        }

        if (fileService == null) {
            fileService = getFileService(application);
        }

        if (mailProcessorUtils == null) {
            mailProcessorUtils = new MailProcessorUtils(contentManager, modelDomain);
        }
    }

    private Object createArticle(final MailImporterConfig config,
                                 final MailRouteConfig routeConfig,
                                 final MailBean mail) throws Exception {
        Object articleBean = mailProcessorUtils.getPopulatedArticleBean(config, routeConfig, mail);
        List<ContentId> images = new ArrayList<>();
        for (String filename : mail.getAttachments().keySet()) {
            if (isAcceptedImageExtension(config.getAcceptedImageExtensions(), filename)) {
                ContentId contentId = createImage(config, routeConfig, mail, filename, mail.getAttachments().get(filename));
                images.add(0, contentId);
            }
        }

        BeanUtils.setProperty(articleBean, "images", images);
        return articleBean;
    }

    private ContentResult<Object> writeArticleBean(final MailProcessorUtils mailProcessorUtils,
                                                   final MailRouteConfig routeConfig,
                                                   final Object articleBean) {
        ContentWriteBuilder<Object> cwb = new ContentWriteBuilder<>();
        cwb.mainAspectData(articleBean);
        cwb.type(routeConfig.getArticleAspect());

        final MetadataInfo metadataInfo = mailProcessorUtils.getMetadataInfo(routeConfig.getTaxonomyId());

        if (StringUtil.notEmpty(routeConfig.getArticlePartition())) {
            final Metadata metadata = new Metadata();
            metadata.addDimension(createDimensionWithEntity(DIMENSION_PARTITION, routeConfig.getArticlePartition()));
            metadataInfo.setMetadata(metadata);
        }

        cwb.aspect(MetadataInfo.ASPECT_NAME, metadataInfo);

        final InsertionInfoAspectBean insertionInfoAspectBean = mailProcessorUtils.getInsertionInfoAspectBean(routeConfig);
        cwb.aspect(InsertionInfoAspectBean.ASPECT_NAME, insertionInfoAspectBean);

        ContentWrite<Object> content = cwb.buildCreate();
        return contentManager.create(content, createSubject(routeConfig));
    }

    private Subject createSubject(final MailRouteConfig config) {
        final String principalId = Optional.ofNullable(config)
                                           .map(MailRouteConfig::getPrincipalId)
                                           .filter(StringUtil::notEmpty)
                                           .orElse("98");
        return new Subject(principalId, null);
    }

    private FileService getFileService(Application application) {
        try {
            HttpFileServiceClient httpFileServiceClient = application.getPreferredApplicationComponent(HttpFileServiceClient.class);
            return httpFileServiceClient.getFileService();
        } catch (IllegalApplicationStateException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isAcceptedImageExtension(final List<String> acceptedImageExtensions, final String filename) {
        for (String suffix : acceptedImageExtensions) {
            if (filename.toLowerCase().endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private Dimension createDimensionWithEntity(String dimension, String entity) {
        return new Dimension(dimension, dimension, false, new Entity(entity, entity));
    }

    private ContentId createImage(final MailImporterConfig config,
                                  final MailRouteConfig routeConfig,
                                  final MailBean mailBean,
                                  final String name,
                                  final byte[] imageData) throws Exception {
        final FileInfo fInfo;
        final MailProcessorUtils.MetadataTagsHolder metadataTags;

        try (final ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {

            final String mimeType = mailProcessorUtils.getFormatName(bis);
            bis.reset();

            metadataTags = mailProcessorUtils.getMetadataTags(bis);
            bis.reset();

            fInfo = fileService.uploadFile(SCHEME_TMP, null, name, bis, mimeType, createSubject(routeConfig));
            assert fInfo != null;
        }

        FilesAspectBean filesAspectBean = mailProcessorUtils.getFilesAspectBean(fInfo);
        ImageInfoAspectBean imageInfoAspectBean = mailProcessorUtils.getImageInfoAspectBean(metadataTags.tags, fInfo);
        final InsertionInfoAspectBean insertionInfoAspectBean = mailProcessorUtils.getInsertionInfoAspectBean(routeConfig);

        Object bean = mailProcessorUtils.getPopulatedImageBean(config, routeConfig, mailBean, metadataTags, name);

        // leave creation date to prestore hook
        final MetadataInfo metadataInfo = mailProcessorUtils.getMetadataInfo(routeConfig.getTaxonomyId());

        if (!StringUtil.isEmpty(routeConfig.getImagePartition())) {
            final Metadata metadata = new Metadata();
            final Dimension partition = createDimensionWithEntity(DIMENSION_PARTITION, routeConfig.getImagePartition());
            metadata.addDimension(partition);
            metadataInfo.setMetadata(metadata);
        }

        ContentWriteBuilder<Object> cwb = new ContentWriteBuilder<>();
        cwb.type(routeConfig.getImageAspect());
        cwb.mainAspectData(bean);

        cwb.aspect(FilesAspectBean.ASPECT_NAME, filesAspectBean);
        cwb.aspect(ImageInfoAspectBean.ASPECT_NAME, imageInfoAspectBean);
        cwb.aspect(InsertionInfoAspectBean.ASPECT_NAME, insertionInfoAspectBean);
        cwb.aspect(MetadataInfo.ASPECT_NAME, metadataInfo);

        ContentWrite<Object> content = cwb.buildCreate();
        ContentResult<Object> cr = contentManager.create(content, createSubject(routeConfig));
        if (!cr.getStatus().isSuccess()) {
            LOG.error("Error importing image: " + name + "." + cr.getStatus().toString());
        }
        LOG.info("Inserted image " + name + " with contentid: " + IdUtil.toIdString(cr.getContentId().getContentId()));

        return cr.getContentId().getContentId();
    }

    private CmClient getCmClient(Application application) {
        try {
            return application.getPreferredApplicationComponent(CmClient.class);
        } catch (IllegalApplicationStateException e) {
            LOG.error("Failed to get CmClient", e);
            throw new RuntimeException(e);
        }
    }

    private ContentManager getContentManager(final Application application) {
        try {
            final RepositoryClient repositoryClient = application.getPreferredApplicationComponent(RepositoryClient.class);
            if (repositoryClient == null) {
                throw new RuntimeException("missing repository client");
            }
            return repositoryClient.getContentManager();
        } catch (IllegalApplicationStateException e) {
            throw new RuntimeException(e);
        }
    }
}
