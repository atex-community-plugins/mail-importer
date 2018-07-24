package com.atex.plugins.mailimporter;

import com.atex.onecms.content.*;
import com.atex.onecms.content.files.FileInfo;
import com.atex.onecms.content.files.FileService;
import com.atex.onecms.content.metadata.MetadataInfo;
import com.atex.onecms.image.ImageInfoAspectBean;
import com.polopoly.application.Application;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.*;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.integration.IntegrationServerApplication;
import com.polopoly.metadata.Metadata;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

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
public class ContentPublisher
{
    public static final Subject SYSTEM_SUBJECT 	  = new Subject("98", null);
    public static final String SCHEME_TMP = "tmp";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private FileService fileService = null;
    private ContentManager contentManager = null;
    private MailProcessorUtils mailProcessorUtils = null;
    private PolicyCMServer cmServer = null;
    private MailImporterConfigPolicy config = null;

    public ContentPublisher()
    {
    }

    private Application getApplication() {
        return IntegrationServerApplication.getPolopolyApplication();
    }

    private MailImporterConfigPolicy getConfig(PolicyCMServer cmServer) throws CMException {
        return (MailImporterConfigPolicy) cmServer.getPolicy(new ExternalContentId(MailImporterConfigPolicy.CONFIG_EXT_ID));
    }

    public ContentId publish(final MailBean mail)
        throws Exception
    {

        /**
         * Please note that the mapping between data and Polopoly types we have to perform here will be
         * very much simplified in future versions of Polopoly, where the Data-API will have support for
         * write operations. It will then be possible to manage data mapping and conversions centralized
         * for the entire project.
         *
         * http://support.polopoly.com/confluence/display/Polopoly > Data-API.
         */

        Application application = getApplication();
        CmClient cmclient = getCmClient(application);
        if (contentManager == null) {
            contentManager = cmclient.getContentManager();
        }

        if (cmServer == null) {
            cmServer = cmclient.getPolicyCMServer();
        }

        if (config == null) {
            config = getConfig(cmServer);
        }

        if (fileService == null) {
            fileService = getFileService(application);
        }

        if (mailProcessorUtils == null) {
            mailProcessorUtils = new MailProcessorUtils(contentManager, config);
        }

        try {
            Object articleBean = createArticle(mail);
            ContentResult<Object> cr = writeArticleBean(mailProcessorUtils, articleBean);
            return cr.getContentId().getContentId();
        } catch (CMException e) {
            throw new RuntimeException("Failed to publish contents!", e);
        }
    }

    public Object createArticle(MailBean mail) throws Exception {
        String articleBeanName = config.getArticleBean();
        Object articleBean = mailProcessorUtils.getPopulatedArticleBean(articleBeanName, mail);
        List<ContentId> images = new ArrayList<>();
        for (String filename : mail.getAttachments().keySet()) {
            if (isAcceptedImageExtension(filename)) {
                ContentId contentId = createImage(mail, filename, mail.getAttachments().get(filename));
                images.add(0, contentId);
            }
        }

        BeanUtils.setProperty(articleBean,"images", images);
        return articleBean;
    }

    private ContentResult<Object> writeArticleBean(MailProcessorUtils mailProcessorUtils, Object articleBean) {
        ContentWriteBuilder<Object> cwb = new ContentWriteBuilder<>();
        cwb.mainAspectData(articleBean);
        cwb.type(articleBean.getClass().getName());

        InsertionInfoAspectBean insertionInfoAspectBean = mailProcessorUtils.getInsertionInfoAspectBean();
        cwb.aspect("p.InsertionInfo", insertionInfoAspectBean);

        ContentWrite<Object> content = cwb.buildCreate();
        return contentManager.create(content, SYSTEM_SUBJECT);
    }

    private FileService getFileService(Application application) throws com.polopoly.application.IllegalApplicationStateException {
        HttpFileServiceClient httpFileServiceClient = application.getPreferredApplicationComponent(HttpFileServiceClient.class);
        return httpFileServiceClient.getFileService();
    }

    private boolean isAcceptedImageExtension(final String filename)
    {
        List<String> acceptedImageExtensions = config.getAcceptedImageExtensions();
        for (String suffix : acceptedImageExtensions) {
            if (filename.toLowerCase().endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private ContentId createImage(MailBean mailBean, final String name, final byte[] imageData)  throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(imageData);

        String mimeType = mailProcessorUtils.getFormatName(bis);
        bis.reset();

        MailProcessorUtils.MetadataTagsHolder metadataTags = mailProcessorUtils.getMetadataTags(bis);
        bis.reset();

        FileInfo fInfo = fileService.uploadFile(SCHEME_TMP, null, name, bis, mimeType, SYSTEM_SUBJECT);

        FilesAspectBean filesAspectBean = mailProcessorUtils.getFilesAspectBean(fInfo);
        ImageInfoAspectBean imageInfoAspectBean = mailProcessorUtils.getImageInfoAspectBean(metadataTags.tags, fInfo);
        InsertionInfoAspectBean insertionInfoAspectBean = mailProcessorUtils.getInsertionInfoAspectBean();

        Object bean = mailProcessorUtils.getPopulatedImageBean(config.getImageBean(), mailBean, metadataTags, name);

        // leave creation date to prestore hook
        MetadataInfo metadataInfo = mailProcessorUtils.getMetadataInfo();
        Metadata metadata = new Metadata();

        metadataInfo.setMetadata(metadata);

        ContentWriteBuilder cwb = new ContentWriteBuilder();
        cwb.type(bean.getClass().getName());
        cwb.mainAspectData(bean);

        cwb.aspect(FilesAspectBean.ASPECT_NAME, filesAspectBean);
        cwb.aspect(ImageInfoAspectBean.ASPECT_NAME, imageInfoAspectBean);
        cwb.aspect(InsertionInfoAspectBean.ASPECT_NAME, insertionInfoAspectBean);
        cwb.aspect(MetadataInfo.ASPECT_NAME, metadataInfo);

        ContentWrite content = cwb.buildCreate();
        ContentResult cr = contentManager.create(content, SYSTEM_SUBJECT);
        if (!cr.getStatus().isOk()) {
            log.error("Error importing image: " + name + "." + cr.getStatus().toString());
        }
        log.info("Inserted image " + name + " with contentid: " + cr.getContentId().getContentId());

        return cr.getContentId().getContentId();
    }

    private CmClient getCmClient(Application application) {
        return (CmClient)application.getApplicationComponent(CmClientBase.DEFAULT_COMPOUND_NAME);
    }

}
